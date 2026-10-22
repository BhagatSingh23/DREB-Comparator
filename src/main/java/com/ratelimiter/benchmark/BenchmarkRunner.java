package com.ratelimiter.benchmark;

import com.ratelimiter.algorithms.CustomRateLimiter;
import com.ratelimiter.algorithms.CustomRateLimiterV2;
import com.ratelimiter.algorithms.DrebParameters;
import com.ratelimiter.algorithms.FixedWindow;
import com.ratelimiter.algorithms.LeakyBucket;
import com.ratelimiter.algorithms.SlidingWindowCounter;
import com.ratelimiter.algorithms.SlidingWindowLog;
import com.ratelimiter.algorithms.TokenBucket;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import com.ratelimiter.reporting.ChartGenerator;
import com.ratelimiter.reporting.CsvReporter;
import com.ratelimiter.reporting.JsonReporter;
import com.ratelimiter.reporting.ReportGenerator;
import com.ratelimiter.reporting.ResultAggregator;
import com.ratelimiter.util.HardwareInfo;
import com.ratelimiter.workload.Workload;
import com.ratelimiter.workload.WorkloadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

/**
 * Main entry point for the benchmark framework.
 *
 * <p>Orchestrates: config loading → factorial sweeps (families, clients, capacities, seeds) →
 * algorithm registration → workload execution → metric collection →
 * result storage → statistical seed aggregation → chart generation → report generation.
 */
public class BenchmarkRunner {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkRunner.class);

    public static void main(String[] args) {
        String profileName = "standard";
        String explicitConfigPath = null;
        boolean dryRun = false;
        boolean smokeMode = false;
        List<String> overrideAlgorithms = null;
        String outputDirStr = null;

        // Parse CLI arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--config" -> {
                    if (i + 1 < args.length) explicitConfigPath = args[++i];
                }
                case "--profile" -> {
                    if (i + 1 < args.length) profileName = args[++i];
                }
                case "--dry-run" -> dryRun = true;
                case "--smoke" -> smokeMode = true;
                case "--algorithms" -> {
                    if (i + 1 < args.length) {
                        overrideAlgorithms = Arrays.asList(args[++i].split(","));
                    }
                }
                case "--output-dir" -> {
                    if (i + 1 < args.length) outputDirStr = args[++i];
                }
                default -> log.warn("Unknown argument: {}", args[i]);
            }
        }

        // Load configuration
        BenchmarkConfig config;
        try {
            if (explicitConfigPath != null) {
                log.info("Loading explicit configuration file: {}", explicitConfigPath);
                config = BenchmarkConfig.loadFromYaml(explicitConfigPath);
            } else {
                log.info("Resolving benchmark profile: {}", profileName);
                String yamlFileName = profileName.endsWith(".yaml") ? profileName : profileName + ".yaml";
                File directFile = new File(profileName);
                File configFile = new File("config/" + yamlFileName);
                File benchmarksFile = new File("benchmarks/configurations/" + yamlFileName);

                if (directFile.exists()) {
                    config = BenchmarkConfig.loadFromYaml(directFile.getAbsolutePath());
                } else if (configFile.exists()) {
                    config = BenchmarkConfig.loadFromYaml(configFile.getAbsolutePath());
                } else if (benchmarksFile.exists()) {
                    config = BenchmarkConfig.loadFromYaml(benchmarksFile.getAbsolutePath());
                } else {
                    config = BenchmarkConfig.loadFromResource(yamlFileName);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load configuration", e);
            return;
        }

        if (smokeMode) {
            config.setSmoke(true);
        }

        // Refuse to produce official results (non-smoke) from a dirty tree
        if (!config.isSmoke() && HardwareInfo.isGitDirty()) {
            throw new IllegalStateException("Cannot run official benchmark with a dirty working tree! Commit or stash all changes, or pass --smoke.");
        }

        if (overrideAlgorithms != null) {
            config.setAlgorithms(overrideAlgorithms);
        }

        // Create timestamped output directory
        if (outputDirStr == null) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            outputDirStr = "results/" + timestamp;
        }
        Path outputDir = Paths.get(outputDirStr);

        try {
            Files.createDirectories(outputDir.resolve("raw"));
            Files.createDirectories(outputDir.resolve("processed"));
            Files.createDirectories(outputDir.resolve("graphs"));
            Files.createDirectories(outputDir.resolve("report"));
        } catch (IOException e) {
            log.error("Failed to create output directories under {}", outputDir, e);
            return;
        }

        // Collect and write metadata
        Map<String, Object> metadata = HardwareInfo.collect();
        metadata.put("configHash", config.getConfigHash());
        metadata.put("libraryTag", "v1.0.2-phase1");
        metadata.put("libraryCommitHash", HardwareInfo.getLibraryCommitHash());
        metadata.put("isDirty", HardwareInfo.isGitDirty());
        metadata.put("rateLimit", config.getRateLimit());
        metadata.put("windowSize", config.getWindowSizeMs());
        metadata.put("workloads", config.getWorkloadTypes());

        try {
            JsonReporter.writeMetadata(outputDir, metadata);
            JsonReporter.writeConfiguration(outputDir, config);
        } catch (IOException e) {
            log.error("Failed to write metadata/configuration", e);
        }

        // Extract Factorial Grids
        List<String> families = (config.getMatchingFamilies() != null && !config.getMatchingFamilies().isEmpty())
                ? config.getMatchingFamilies()
                : List.of(config.getFamily() != null ? config.getFamily() : "equal_capacity_and_rate");

        List<Integer> clientsList = (config.getClientsGrid() != null && !config.getClientsGrid().isEmpty())
                ? config.getClientsGrid()
                : List.of(config.getClients());

        List<Long> capacityList = (config.getCapacityGrid() != null && !config.getCapacityGrid().isEmpty())
                ? config.getCapacityGrid()
                : List.of(config.getBucketCapacity());

        List<Long> seedsList = (config.getSeeds() != null && !config.getSeeds().isEmpty())
                ? config.getSeeds()
                : List.of(config.getSeed());

        // Dry run estimation
        if (dryRun) {
            long totalRuns = (long) families.size() * clientsList.size() * capacityList.size() * seedsList.size()
                    * config.getWorkloadTypes().size() * config.getMeasurementRuns();
            log.info("DRY RUN — Estimated configurations: {} families x {} client levels x {} capacity levels x {} seeds = {} sweeps. Total measurement runs: {}",
                    families.size(), clientsList.size(), capacityList.size(), seedsList.size(),
                    families.size() * clientsList.size() * capacityList.size() * seedsList.size(), totalRuns);
            return;
        }

        List<BenchmarkResult> allResults = new ArrayList<>();

        for (String family : families) {
            log.info("========== Baseline Matching Family: {} ==========", family);

            for (int clientCount : clientsList) {
                for (long capacity : capacityList) {
                    double refillRate = config.getRefillRate() > 0 ? config.getRefillRate() : capacity;

                    for (long seed : seedsList) {
                        BenchmarkConfig runConfig = copyConfig(config);
                        runConfig.setFamily(family);
                        runConfig.setClients(clientCount);
                        runConfig.setBucketCapacity(capacity);
                        runConfig.setRateLimit(capacity);
                        runConfig.setRefillRate(refillRate);
                        runConfig.setSeed(seed);

                        Map<String, Supplier<RateLimiter>> registry =
                                buildAlgorithmRegistry(runConfig, family, capacity, refillRate);

                        for (String workloadType : runConfig.getWorkloadTypes()) {
                            Workload workload = WorkloadFactory.create(workloadType, runConfig);

                            for (Map.Entry<String, Supplier<RateLimiter>> entry : registry.entrySet()) {
                                String algoName = entry.getKey();
                                Supplier<RateLimiter> supplier = entry.getValue();

                                log.info("Running [{} | C={} | Clients={} | Seed={}] {} x {}",
                                        family, capacity, clientCount, seed, algoName, workloadType);

                                BenchmarkExecutor executor = new BenchmarkExecutor(
                                        runConfig, supplier, algoName, workload, workloadType);
                                BenchmarkResult result = executor.execute();
                                allResults.add(result);
                            }
                        }
                    }
                }
            }
        }

        // Generate outputs
        try {
            log.info("Writing raw CSV results...");
            CsvReporter.writeResults(outputDir.resolve("raw"), allResults);

            log.info("Computing seed aggregation, 95% bootstrap CIs, and paired differences vs Token Bucket...");
            ResultAggregator.aggregateSeedResults(allResults, outputDir.resolve("processed"), config.isSmoke());

            log.info("Generating charts...");
            ChartGenerator.generateAllCharts(outputDir.resolve("graphs"), allResults);

            log.info("Generating report...");
            ReportGenerator.generateReport(outputDir.resolve("report"), allResults, metadata, config);

            log.info("Writing JSON results...");
            JsonReporter.writeResults(outputDir, allResults);
        } catch (Exception e) {
            log.error("Error generating reports", e);
        }

        // Console summary
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println("  Benchmark Complete");
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════════");
        System.out.printf("  Results directory: %s%n", outputDir);
        System.out.printf("  Total executions: %d%n", allResults.size());
        System.out.println("──────────────────────────────────────────────────────────────────────────────────────────");
        System.out.printf("  %-25s | %-16s | %-6s | %-6s | %-5s | %-12s | %-10s | %s%n",
                "Algorithm", "Workload", "Family", "Cap", "Seed", "Throughput", "P99 (ns)", "Violations");
        System.out.println("──────────────────────────────────────────────────────────────────────────────────────────");
        for (BenchmarkResult r : allResults) {
            System.out.printf("  %-25s | %-16s | %-6s | %-6.0f | %-5d | %10.1f op/s | %10.0f | %d%n",
                    r.getAlgorithmName(), r.getWorkloadName(),
                    r.getFamily().substring(0, Math.min(6, r.getFamily().length())),
                    r.getCapacity(), r.getSeed(),
                    r.getThroughputOpsPerSec(), r.getP99LatencyNs(), r.getViolationCount());
        }
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════════");
    }

    private static BenchmarkConfig copyConfig(BenchmarkConfig src) {
        BenchmarkConfig c = new BenchmarkConfig();
        c.setProfileName(src.getProfileName());
        c.setRateLimit(src.getRateLimit());
        c.setWindowSizeMs(src.getWindowSizeMs());
        c.setBucketCapacity(src.getBucketCapacity());
        c.setRefillRate(src.getRefillRate());
        c.setClients(src.getClients());
        c.setDurationSeconds(src.getDurationSeconds());
        c.setWarmupSeconds(src.getWarmupSeconds());
        c.setWarmupRuns(src.getWarmupRuns());
        c.setMeasurementRuns(src.getMeasurementRuns());
        c.setSeed(src.getSeed());
        c.setWorkloadTypes(src.getWorkloadTypes());
        c.setAlgorithms(src.getAlgorithms());
        c.setDrebMappingStrategy(src.getDrebMappingStrategy());
        c.setFamily(src.getFamily());
        c.setConfigHash(src.getConfigHash());
        c.setGitCommitHash(src.getGitCommitHash());
        c.setSmoke(src.isSmoke());
        c.setInitialState(src.getInitialState());
        c.setLibraryTag(src.getLibraryTag());
        c.setLibraryCommitHash(src.getLibraryCommitHash());
        return c;
    }

    /**
     * Builds algorithm registry adhering strictly to the baseline-matching family rules:
     * - Equal Capacity and Rate: all algorithms configured with targetC and targetR.
     * - Equal Instantaneous Burst: DREB Proportional capacity scaled by 9/7 so burst ceiling equals targetC;
     *   DREB v2 base 1.0 scaled by 7/9 so burst ceiling equals targetC.
     * - Equal Cumulative Envelope: DREB Burst-Matched & v2 capacity scaled by 7/9 so S_max + R*T equals targetC + R*T.
     */
    private static Map<String, Supplier<RateLimiter>> buildAlgorithmRegistry(
            BenchmarkConfig benchConfig, String family, double targetC, double targetR) {
        Map<String, Supplier<RateLimiter>> registry = new LinkedHashMap<>();

        List<String> algos = benchConfig.getAlgorithms();
        boolean all = algos.contains("all");

        RateLimiterConfig standardConfig = RateLimiterConfig.builder()
                .maxRequests((long) targetC)
                .windowSizeMs(benchConfig.getWindowSizeMs())
                .bucketCapacity((long) targetC)
                .refillRate(targetR)
                .build();

        RateLimiterConfig tbSmallConfig = RateLimiterConfig.builder()
                .maxRequests((long) (targetC * 3.0 / 7.0))
                .windowSizeMs(benchConfig.getWindowSizeMs())
                .bucketCapacity((long) (targetC * 3.0 / 7.0))
                .refillRate(targetR)
                .build();

        if (all || algos.contains("token_bucket") || algos.contains("tb_large")) {
            registry.put("Token Bucket (TB-large)", () -> new TokenBucket(standardConfig));
        }
        if (all || algos.contains("tb_small") || algos.contains("token_bucket_small")) {
            registry.put("Token Bucket (TB-small)", () -> new TokenBucket(tbSmallConfig));
        }
        if (all || algos.contains("leaky_bucket")) {
            registry.put("Leaky Bucket", () -> new LeakyBucket(standardConfig));
        }
        if (all || algos.contains("fixed_window")) {
            registry.put("Fixed Window", () -> new FixedWindow(standardConfig));
        }
        if (all || algos.contains("sliding_window_log")) {
            registry.put("Sliding Window Log", () -> new SlidingWindowLog(standardConfig));
        }
        if (all || algos.contains("sliding_window_counter")) {
            registry.put("Sliding Window Counter", () -> new SlidingWindowCounter(standardConfig));
        }

        // DREB Proportional
        if (all || algos.contains("dreb_proportional") || (algos.contains("custom") &&
                ("both".equalsIgnoreCase(benchConfig.getDrebMappingStrategy()) || "proportional".equalsIgnoreCase(benchConfig.getDrebMappingStrategy())))) {
            double propC = "equal_instantaneous_burst".equals(family) ? (targetC * 9.0 / 7.0) : targetC;
            RateLimiterConfig propConfig = RateLimiterConfig.builder()
                    .maxRequests((long) propC)
                    .windowSizeMs(benchConfig.getWindowSizeMs())
                    .bucketCapacity((long) propC)
                    .refillRate(targetR)
                    .build();
            registry.put("DREB (proportional)", () -> new CustomRateLimiter(propConfig, CustomRateLimiter.PROPORTIONAL));
        }

        // DREB Burst-Matched
        if (all || algos.contains("dreb_burst_matched") || (algos.contains("custom") &&
                ("both".equalsIgnoreCase(benchConfig.getDrebMappingStrategy()) || "burst_ceiling_matched".equalsIgnoreCase(benchConfig.getDrebMappingStrategy())))) {
            double bmC = "equal_cumulative_envelope".equals(family) ? (targetC * 7.0 / 9.0) : targetC;
            RateLimiterConfig bmConfig = RateLimiterConfig.builder()
                    .maxRequests((long) bmC)
                    .windowSizeMs(benchConfig.getWindowSizeMs())
                    .bucketCapacity((long) bmC)
                    .refillRate(targetR)
                    .build();
            registry.put("DREB (burst-matched)", () -> new CustomRateLimiter(bmConfig, CustomRateLimiter.BURST_CEILING_MATCHED));
        }

        // DREB v2 Adaptive Base 1.5 (headline comparison policy, max burst = C)
        if (all || algos.contains("dreb_v2_base15") || (algos.contains("custom") && "v2_adaptive_base_1_5".equalsIgnoreCase(benchConfig.getDrebMappingStrategy()))) {
            double v2C = "equal_cumulative_envelope".equals(family) ? (targetC * 7.0 / 9.0) : targetC;
            RateLimiterConfig v2Config = RateLimiterConfig.builder()
                    .maxRequests((long) v2C)
                    .windowSizeMs(benchConfig.getWindowSizeMs())
                    .bucketCapacity((long) v2C)
                    .refillRate(targetR)
                    .build();
            registry.put("DREB (v2-adaptive-base1.5)", () -> new CustomRateLimiterV2(v2Config, DrebParameters.Strategy.V2_ADAPTIVE_BASE_1_5));
        }

        // DREB v2 Adaptive Base 1.0 (named variant; admits up to 9C/7)
        if (all || algos.contains("dreb_v2_base10") || (algos.contains("custom") &&
                ("v2_adaptive_base_1_0".equalsIgnoreCase(benchConfig.getDrebMappingStrategy()) || "v2-adaptive".equalsIgnoreCase(benchConfig.getDrebMappingStrategy())))) {
            double v2C = ("equal_cumulative_envelope".equals(family) || "equal_instantaneous_burst".equals(family))
                    ? (targetC * 7.0 / 9.0) : targetC;
            RateLimiterConfig v2Config = RateLimiterConfig.builder()
                    .maxRequests((long) v2C)
                    .windowSizeMs(benchConfig.getWindowSizeMs())
                    .bucketCapacity((long) v2C)
                    .refillRate(targetR)
                    .build();
            registry.put("DREB (v2-adaptive-base1.0)", () -> new CustomRateLimiterV2(v2Config, DrebParameters.Strategy.V2_ADAPTIVE_BASE_1_0));
        }

        return registry;
    }
}

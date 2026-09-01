package com.ratelimiter.benchmark;

import com.ratelimiter.algorithms.CustomRateLimiter;
import com.ratelimiter.algorithms.CustomRateLimiterV2;
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
import com.ratelimiter.util.HardwareInfo;
import com.ratelimiter.workload.Workload;
import com.ratelimiter.workload.WorkloadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <p>Orchestrates: config loading → algorithm registration → workload execution →
 * metric collection → result storage → chart generation → report generation.
 *
 * <p>Usage:
 * <pre>
 *   java -cp ... com.ratelimiter.benchmark.BenchmarkRunner \
 *     --profile smoke [--dry-run] [--algorithms token_bucket,fixed_window] [--output-dir results/custom]
 * </pre>
 */
public class BenchmarkRunner {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkRunner.class);

    public static void main(String[] args) {
        String profileName = "standard";
        boolean dryRun = false;
        List<String> overrideAlgorithms = null;
        String outputDirStr = null;

        // Parse CLI arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--profile" -> {
                    if (i + 1 < args.length) profileName = args[++i];
                }
                case "--dry-run" -> dryRun = true;
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
        log.info("Loading benchmark profile: {}", profileName);
        BenchmarkConfig config;
        try {
            String yamlFileName = profileName.endsWith(".yaml") ? profileName : profileName + ".yaml";
            java.io.File profileFile = new java.io.File("benchmarks/configurations/" + yamlFileName);
            if (profileFile.exists()) {
                config = BenchmarkConfig.loadFromYaml(profileFile.getAbsolutePath());
            } else {
                config = BenchmarkConfig.loadFromResource(yamlFileName);
            }
        } catch (Exception e) {
            log.error("Failed to load configuration profile '{}'", profileName, e);
            return;
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
        metadata.put("rateLimit", config.getRateLimit());
        metadata.put("windowSize", config.getWindowSizeMs());
        metadata.put("clientCount", config.getClients());
        metadata.put("workloads", config.getWorkloadTypes());
        metadata.put("randomSeed", config.getSeed());
        metadata.put("warmupRuns", config.getWarmupRuns());
        metadata.put("measurementRuns", config.getMeasurementRuns());

        try {
            JsonReporter.writeMetadata(outputDir, metadata);
            JsonReporter.writeConfiguration(outputDir, config);
        } catch (IOException e) {
            log.error("Failed to write metadata/configuration", e);
        }

        // Build algorithm registry
        Map<String, Supplier<RateLimiter>> registry = buildAlgorithmRegistry(config);
        log.info("Registered {} algorithms: {}", registry.size(), registry.keySet());

        // Dry run: estimate and exit
        if (dryRun) {
            int workloads = config.getWorkloadTypes().size();
            int algos = registry.size();
            long runsPerCombo = config.getWarmupRuns() + config.getMeasurementRuns();
            long estimatedSec = algos * workloads * runsPerCombo * config.getDurationSeconds();
            log.info("DRY RUN — Estimated runtime: {} seconds ({} algorithms × {} workloads × {} runs × {}s/run)",
                    estimatedSec, algos, workloads, runsPerCombo, config.getDurationSeconds());
            return;
        }

        // Execute benchmarks
        List<BenchmarkResult> allResults = new ArrayList<>();

        for (String workloadType : config.getWorkloadTypes()) {
            Workload workload = WorkloadFactory.create(workloadType, config);

            for (Map.Entry<String, Supplier<RateLimiter>> entry : registry.entrySet()) {
                String algoName = entry.getKey();
                Supplier<RateLimiter> supplier = entry.getValue();

                log.info("Benchmarking {} with workload '{}'...", algoName, workloadType);

                BenchmarkExecutor executor = new BenchmarkExecutor(
                        config, supplier, algoName, workload, workloadType);
                BenchmarkResult result = executor.execute();
                allResults.add(result);

                log.info("  → Throughput: {:.2f} ops/sec, P99: {:.0f} ns, Violations: {}",
                        result.getThroughputOpsPerSec(), result.getP99LatencyNs(), result.getViolationCount());
            }
        }

        // Generate outputs
        try {
            log.info("Writing CSV results...");
            CsvReporter.writeResults(outputDir.resolve("raw"), allResults);

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
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  Benchmark Complete");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.printf("  Results directory: %s%n", outputDir);
        System.out.printf("  Algorithms tested: %d%n", registry.size());
        System.out.printf("  Workloads: %d%n", config.getWorkloadTypes().size());
        System.out.println("───────────────────────────────────────────────────────");
        for (BenchmarkResult r : allResults) {
            System.out.printf("  [%-25s | %-15s] %,.0f ops/sec  p99=%,.0f ns%n",
                    r.getAlgorithmName(), r.getWorkloadName(),
                    r.getThroughputOpsPerSec(), r.getP99LatencyNs());
        }
        System.out.println("═══════════════════════════════════════════════════════");
    }

    /**
     * Builds the algorithm registry, mapping algorithm names to factory suppliers.
     *
     * <p>CustomRateLimiter is tested with a probe call — if it throws
     * {@link UnsupportedOperationException}, it is skipped gracefully.
     */
    private static Map<String, Supplier<RateLimiter>> buildAlgorithmRegistry(BenchmarkConfig benchConfig) {
        Map<String, Supplier<RateLimiter>> registry = new LinkedHashMap<>();

        RateLimiterConfig rlConfig = RateLimiterConfig.builder()
                .maxRequests(benchConfig.getRateLimit())
                .windowSizeMs(benchConfig.getWindowSizeMs())
                .bucketCapacity(benchConfig.getBucketCapacity())
                .refillRate(benchConfig.getRefillRate())
                .build();

        List<String> algos = benchConfig.getAlgorithms();
        boolean all = algos.contains("all");

        if (all || algos.contains("token_bucket")) {
            registry.put("Token Bucket", () -> new TokenBucket(rlConfig));
        }
        if (all || algos.contains("leaky_bucket")) {
            registry.put("Leaky Bucket", () -> new LeakyBucket(rlConfig));
        }
        if (all || algos.contains("fixed_window")) {
            registry.put("Fixed Window", () -> new FixedWindow(rlConfig));
        }
        if (all || algos.contains("sliding_window_log")) {
            registry.put("Sliding Window Log", () -> new SlidingWindowLog(rlConfig));
        }
        if (all || algos.contains("sliding_window_counter")) {
            registry.put("Sliding Window Counter", () -> new SlidingWindowCounter(rlConfig));
        }
        if (all || algos.contains("custom")) {
            String strategy = benchConfig.getDrebMappingStrategy();
            boolean bothMappings = "both".equalsIgnoreCase(strategy);

            if (bothMappings || CustomRateLimiter.PROPORTIONAL.equals(strategy)) {
                try {
                    RateLimiter probe = new CustomRateLimiter(rlConfig, CustomRateLimiter.PROPORTIONAL);
                    probe.allow(new com.ratelimiter.core.RateLimitRequest("probe", 0));
                    String label = bothMappings ? "Custom (proportional)" : "Custom";
                    registry.put(label, () -> new CustomRateLimiter(rlConfig, CustomRateLimiter.PROPORTIONAL));
                    log.info("CustomRateLimiter ({}) — including in benchmarks", CustomRateLimiter.PROPORTIONAL);
                } catch (UnsupportedOperationException e) {
                    log.info("Skipping CustomRateLimiter (proportional) (not yet implemented)");
                }
            }

            if (bothMappings || CustomRateLimiter.BURST_CEILING_MATCHED.equals(strategy)) {
                try {
                    RateLimiter probe = new CustomRateLimiter(rlConfig, CustomRateLimiter.BURST_CEILING_MATCHED);
                    probe.allow(new com.ratelimiter.core.RateLimitRequest("probe", 0));
                    String label = bothMappings ? "Custom (burst-matched)" : "Custom";
                    registry.put(label, () -> new CustomRateLimiter(rlConfig, CustomRateLimiter.BURST_CEILING_MATCHED));
                    log.info("CustomRateLimiter ({}) — including in benchmarks", CustomRateLimiter.BURST_CEILING_MATCHED);
                } catch (UnsupportedOperationException e) {
                    log.info("Skipping CustomRateLimiter (burst_ceiling_matched) (not yet implemented)");
                }
            }

            // Always add V2 Adaptive if Custom is enabled
            try {
                RateLimiter probeV2 = new CustomRateLimiterV2(rlConfig);
                probeV2.allow(new com.ratelimiter.core.RateLimitRequest("probe", 0));
                registry.put("Custom (v2-adaptive)", () -> new CustomRateLimiterV2(rlConfig));
                log.info("CustomRateLimiterV2 (v2-adaptive) — including in benchmarks");
            } catch (UnsupportedOperationException e) {
                log.info("Skipping CustomRateLimiterV2 (not yet implemented)");
            }
        }

        return registry;
    }
}

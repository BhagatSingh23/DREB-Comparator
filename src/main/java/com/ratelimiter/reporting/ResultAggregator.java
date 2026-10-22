package com.ratelimiter.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Utility to aggregate JMH JSON output and workload CSVs into a unified summary.
 */
public class ResultAggregator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Reads JMH JSON and workload CSV results, aggregating them into a single CSV.
     *
     * @param jmhJsonPath     Path to the JMH results in JSON format.
     * @param workloadCsvDir  Directory containing workload simulation CSVs (e.g. all_results.csv).
     * @param outputPath      Path to output the unified summary CSV.
     */
    public void aggregateResults(Path jmhJsonPath, Path workloadCsvDir, Path outputPath) {
        List<SummaryRecord> records = new ArrayList<>();

        if (jmhJsonPath != null && Files.exists(jmhJsonPath)) {
            records.addAll(parseJmhResults(jmhJsonPath));
        } else {
            System.err.println("JMH JSON file not found, skipping JMH aggregation: " + jmhJsonPath);
        }

        if (workloadCsvDir != null) {
            Path workloadCsv = workloadCsvDir.resolve("all_results.csv");
            if (Files.exists(workloadCsv)) {
                records.addAll(parseWorkloadResults(workloadCsv));
            } else {
                System.err.println("Workload CSV not found, skipping workload aggregation: " + workloadCsv);
            }
        }

        writeSummaryCsv(outputPath, records);
    }

    private List<SummaryRecord> parseJmhResults(Path jmhJsonPath) {
        List<SummaryRecord> records = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(jmhJsonPath.toFile());
            if (root.isArray()) {
                for (JsonNode node : root) {
                    String benchmark = node.path("benchmark").asText("");
                    String mode = node.path("mode").asText("");
                    
                    JsonNode primaryMetric = node.path("primaryMetric");
                    double score = primaryMetric.path("score").asDouble();
                    String scoreUnit = primaryMetric.path("scoreUnit").asText("");
                    
                    String algorithm = "unknown";
                    JsonNode params = node.path("params");
                    if (params != null && params.has("algorithm")) {
                        algorithm = params.get("algorithm").asText();
                    }
                    
                    String metricName = getMetricFromJmhMode(mode);
                    records.add(new SummaryRecord(algorithm, "jmh", metricName, String.valueOf(score), scoreUnit));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read JMH results: " + e.getMessage());
        }
        return records;
    }

    private List<SummaryRecord> parseWorkloadResults(Path workloadCsvPath) {
        List<SummaryRecord> records = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(workloadCsvPath);
            if (lines.isEmpty()) return records;

            String[] headers = lines.get(0).split(",");
            for (int i = 1; i < lines.size(); i++) {
                String[] values = lines.get(i).split(",");
                if (values.length >= headers.length) {
                    String algorithm = values[0].trim();
                    for (int j = 1; j < headers.length; j++) {
                        String metric = headers[j].trim();
                        String val = values[j].trim();
                        
                        // Infer unit based on metric name for workload
                        String unit = "ops/s";
                        if (metric.toLowerCase().contains("memory")) unit = "MB";
                        else if (metric.toLowerCase().contains("p50") || metric.toLowerCase().contains("p95") || metric.toLowerCase().contains("p99")) unit = "ms";
                        else if (metric.toLowerCase().contains("fairness")) unit = "index";

                        records.add(new SummaryRecord(algorithm, "workload", metric, val, unit));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read workload results: " + e.getMessage());
        }
        return records;
    }

    private void writeSummaryCsv(Path outputPath, List<SummaryRecord> records) {
        try {
            Files.createDirectories(outputPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                writer.write("algorithm,source,metric,value,unit\n");
                for (SummaryRecord record : records) {
                    writer.write(String.format("%s,%s,%s,%s,%s\n",
                            record.algorithm(), record.source(), record.metric(), record.value(), record.unit()));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to write summary CSV: " + e.getMessage());
        }
    }

    private String getMetricFromJmhMode(String mode) {
        if (mode.equalsIgnoreCase("thrpt")) return "throughput";
        if (mode.equalsIgnoreCase("avgt")) return "average_time";
        if (mode.equalsIgnoreCase("sample")) return "sample_time";
        return mode;
    }

    private record SummaryRecord(String algorithm, String source, String metric, String value, String unit) {}

    public record BootstrapCi(double mean, double sd, double ciLower, double ciUpper) {}

    public static BootstrapCi computeBootstrap(List<Double> values, int numResamples, long seed) {
        return computeBootstrap(values, numResamples, seed, false);
    }

    public static BootstrapCi computeBootstrap(List<Double> values, int numResamples, long seed, boolean isSmoke) {
        if (values.isEmpty()) {
            return new BootstrapCi(0, 0, 0, 0);
        }
        int n = values.size();
        if (n < 30 && !isSmoke) {
            throw new IllegalStateException("Statistical inference requires N >= 30 seeds, but got N = " + n + ". Re-run with at least 30 seeds, or set --smoke.");
        }
        double sum = 0.0;
        for (double v : values) sum += v;
        double mean = sum / n;

        double var = 0.0;
        for (double v : values) var += Math.pow(v - mean, 2);
        double sd = (n > 1) ? Math.sqrt(var / (n - 1)) : 0.0;

        if (n < 2) {
            return new BootstrapCi(mean, sd, mean, mean);
        }

        java.util.Random rng = new java.util.Random(seed);
        double[] bootMeans = new double[numResamples];
        for (int b = 0; b < numResamples; b++) {
            double bSum = 0.0;
            for (int i = 0; i < n; i++) {
                bSum += values.get(rng.nextInt(n));
            }
            bootMeans[b] = bSum / n;
        }
        java.util.Arrays.sort(bootMeans);
        int lowIdx = (int) Math.floor(0.025 * numResamples);
        int highIdx = (int) Math.ceil(0.975 * numResamples) - 1;
        lowIdx = Math.max(0, Math.min(numResamples - 1, lowIdx));
        highIdx = Math.max(0, Math.min(numResamples - 1, highIdx));

        return new BootstrapCi(mean, sd, bootMeans[lowIdx], bootMeans[highIdx]);
    }

    public static void aggregateSeedResults(List<com.ratelimiter.benchmark.BenchmarkResult> results, Path processedDir) {
        aggregateSeedResults(results, processedDir, false);
    }

    /**
     * Aggregates benchmark results across multiple random seeds, computing mean, SD,
     * 95% bootstrap confidence intervals, and paired differences vs Token Bucket.
     *
     * @param results      List of all executed BenchmarkResults
     * @param processedDir Target directory (e.g. results/.../processed)
     * @param isSmoke      Whether smoke testing mode is active
     */
    public static void aggregateSeedResults(List<com.ratelimiter.benchmark.BenchmarkResult> results, Path processedDir, boolean isSmoke) {
        try {
            Files.createDirectories(processedDir);

            // 1. Group by (family, clients, capacity, workload, algorithm)
            record GroupKey(String family, int clients, double capacity, String workload, String algorithm) {}
            Map<GroupKey, List<com.ratelimiter.benchmark.BenchmarkResult>> groups = new java.util.LinkedHashMap<>();

            for (com.ratelimiter.benchmark.BenchmarkResult r : results) {
                GroupKey k = new GroupKey(r.getFamily(), r.getClients(), r.getCapacity(), r.getWorkloadName(), r.getAlgorithmName());
                groups.computeIfAbsent(k, x -> new ArrayList<>()).add(r);
            }

            // Write processed/summary_table.csv
            Path summaryPath = processedDir.resolve("summary_table.csv");
            try (BufferedWriter w = Files.newBufferedWriter(summaryPath)) {
                w.write("family,clients,capacity,workload,algorithm,N,inference_status," +
                        "acceptance_mean,acceptance_sd,acceptance_ci95_low,acceptance_ci95_high," +
                        "fairness_mean,fairness_sd,fairness_ci95_low,fairness_ci95_high,violations_total," +
                        "throughput_indicative_not_for_paper,p99_ns_indicative_not_for_paper\n");

                for (Map.Entry<GroupKey, List<com.ratelimiter.benchmark.BenchmarkResult>> entry : groups.entrySet()) {
                    GroupKey k = entry.getKey();
                    List<com.ratelimiter.benchmark.BenchmarkResult> list = entry.getValue();
                    int N = list.size();
                    String status = (N >= 30) ? "OFFICIAL" : "SMOKE, N too small for inference";

                    List<Double> throughputs = list.stream().map(com.ratelimiter.benchmark.BenchmarkResult::getThroughputOpsPerSec).toList();
                    List<Double> acceptances = list.stream().map(com.ratelimiter.benchmark.BenchmarkResult::getAcceptanceRate).toList();
                    List<Double> p99s = list.stream().map(com.ratelimiter.benchmark.BenchmarkResult::getP99LatencyNs).toList();
                    List<Double> fairness = list.stream().map(com.ratelimiter.benchmark.BenchmarkResult::getFairnessIndex).toList();
                    int totalViolations = list.stream().mapToInt(com.ratelimiter.benchmark.BenchmarkResult::getViolationCount).sum();

                    BootstrapCi accCi = computeBootstrap(acceptances, 1000, 43L, isSmoke);
                    BootstrapCi fnCi = computeBootstrap(fairness, 1000, 45L, isSmoke);

                    double tpMean = throughputs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double p99Mean = p99s.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                    w.write(String.format("%s,%d,%.1f,%s,%s,%d,%s," +
                            "%.4f,%.4f,%.4f,%.4f," +
                            "%.4f,%.4f,%.4f,%.4f,%d," +
                            "%.1f,%.1f\n",
                            k.family(), k.clients(), k.capacity(), k.workload(), k.algorithm(), N, status,
                            accCi.mean(), accCi.sd(), accCi.ciLower(), accCi.ciUpper(),
                            fnCi.mean(), fnCi.sd(), fnCi.ciLower(), fnCi.ciUpper(), totalViolations,
                            tpMean, p99Mean));
                }
            }

            // 2. Compute Paired Differences vs Token Bucket (per identical seed)
            record ConfigKey(String family, int clients, double capacity, String workload) {}
            Map<ConfigKey, Map<String, Map<Long, com.ratelimiter.benchmark.BenchmarkResult>>> byConfig = new java.util.LinkedHashMap<>();

            for (com.ratelimiter.benchmark.BenchmarkResult r : results) {
                ConfigKey ck = new ConfigKey(r.getFamily(), r.getClients(), r.getCapacity(), r.getWorkloadName());
                byConfig.computeIfAbsent(ck, x -> new java.util.LinkedHashMap<>())
                        .computeIfAbsent(r.getAlgorithmName(), x -> new java.util.LinkedHashMap<>())
                        .put(r.getSeed(), r);
            }

            Path pairedPath = processedDir.resolve("paired_differences.csv");
            try (BufferedWriter pw = Files.newBufferedWriter(pairedPath)) {
                pw.write("family,clients,capacity,workload,algorithm,baseline,N,inference_status," +
                        "diff_acceptance_mean,diff_acceptance_sd,diff_acceptance_ci95_low,diff_acceptance_ci95_high\n");

                for (Map.Entry<ConfigKey, Map<String, Map<Long, com.ratelimiter.benchmark.BenchmarkResult>>> ce : byConfig.entrySet()) {
                    ConfigKey ck = ce.getKey();
                    Map<String, Map<Long, com.ratelimiter.benchmark.BenchmarkResult>> algos = ce.getValue();

                    // Find Token Bucket baseline
                    Map<Long, com.ratelimiter.benchmark.BenchmarkResult> tbResults = algos.get("Token Bucket");
                    if (tbResults == null) {
                        tbResults = algos.get("Token Bucket (TB-large)");
                    }
                    if (tbResults == null || tbResults.isEmpty()) {
                        continue;
                    }

                    for (Map.Entry<String, Map<Long, com.ratelimiter.benchmark.BenchmarkResult>> ae : algos.entrySet()) {
                        String algo = ae.getKey();
                        if (algo.startsWith("Token Bucket")) continue;

                        Map<Long, com.ratelimiter.benchmark.BenchmarkResult> aResults = ae.getValue();
                        List<Double> diffAcc = new ArrayList<>();

                        for (Map.Entry<Long, com.ratelimiter.benchmark.BenchmarkResult> se : aResults.entrySet()) {
                            long seed = se.getKey();
                            com.ratelimiter.benchmark.BenchmarkResult aRes = se.getValue();
                            com.ratelimiter.benchmark.BenchmarkResult tbRes = tbResults.get(seed);
                            if (tbRes != null) {
                                diffAcc.add(aRes.getAcceptanceRate() - tbRes.getAcceptanceRate());
                            }
                        }

                        if (!diffAcc.isEmpty()) {
                            int N = diffAcc.size();
                            String status = (N >= 30) ? "OFFICIAL" : "SMOKE, N too small for inference";
                            BootstrapCi daccCi = computeBootstrap(diffAcc, 1000, 102L, isSmoke);

                            pw.write(String.format("%s,%d,%.1f,%s,%s,%s,%d,%s," +
                                    "%.4f,%.4f,%.4f,%.4f\n",
                                    ck.family(), ck.clients(), ck.capacity(), ck.workload(), algo, "Token Bucket", N, status,
                                    daccCi.mean(), daccCi.sd(), daccCi.ciLower(), daccCi.ciUpper()));
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Failed to write aggregate seed results: " + e.getMessage());
        }
    }
}

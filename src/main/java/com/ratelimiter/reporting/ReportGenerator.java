package com.ratelimiter.reporting;

import com.ratelimiter.benchmark.BenchmarkConfig;
import com.ratelimiter.benchmark.BenchmarkResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates a Markdown benchmark report with tables, metadata,
 * and embedded graph references.
 */
public final class ReportGenerator {

    private ReportGenerator() {
    }

    /**
     * Generates {@code benchmark-report.md} in the given directory.
     *
     * @param reportDir the directory to write the report to
     * @param results   all benchmark results
     * @param metadata  machine/environment metadata
     * @param config    benchmark configuration used
     * @throws IOException if file I/O fails
     */
    public static void generateReport(Path reportDir, List<BenchmarkResult> results,
                                      Map<String, Object> metadata, BenchmarkConfig config) throws IOException {
        Files.createDirectories(reportDir);
        Path reportPath = reportDir.resolve("benchmark-report.md");

        try (BufferedWriter w = Files.newBufferedWriter(reportPath)) {
            w.write("# Rate Limiter Benchmark Report\n\n");

            // Metadata
            w.write("## Environment\n\n");
            w.write("| Property | Value |\n");
            w.write("|----------|-------|\n");
            w.write("| Timestamp | " + metadata.getOrDefault("timestamp", "N/A") + " |\n");
            w.write("| OS | " + metadata.getOrDefault("os", "N/A") + " |\n");
            w.write("| CPU | " + metadata.getOrDefault("cpuModel", "N/A") + " |\n");
            w.write("| Cores | " + metadata.getOrDefault("cpuCores", "N/A") + " |\n");
            w.write("| JDK | " + metadata.getOrDefault("javaVersion", "N/A") + " (" + metadata.getOrDefault("jvmVendor", "N/A") + ") |\n");
            w.write("| Total RAM | " + metadata.getOrDefault("totalMemoryMb", "N/A") + " MB |\n\n");

            // Configuration
            w.write("## Configuration\n\n");
            w.write("| Parameter | Value |\n");
            w.write("|-----------|-------|\n");
            w.write("| Profile | " + config.getProfileName() + " |\n");
            w.write("| Rate Limit | " + config.getRateLimit() + " |\n");
            w.write("| Window Size | " + config.getWindowSizeMs() + " ms |\n");
            w.write("| Clients | " + config.getClients() + " |\n");
            w.write("| Duration | " + config.getDurationSeconds() + " s |\n");
            w.write("| Warmup Runs | " + config.getWarmupRuns() + " |\n");
            w.write("| Measurement Runs | " + config.getMeasurementRuns() + " |\n");
            w.write("| Seed | " + config.getSeed() + " |\n\n");

            // Algorithms tested
            w.write("## Algorithms Tested\n\n");
            for (String algo : results.stream().map(BenchmarkResult::getAlgorithmName).distinct().toList()) {
                w.write("- " + algo + "\n");
            }
            w.write("\n");

            // Workloads
            w.write("## Workloads\n\n");
            for (String wl : results.stream().map(BenchmarkResult::getWorkloadName).distinct().toList()) {
                w.write("- " + wl + "\n");
            }
            w.write("\n");

            // Correctness
            w.write("## Correctness Results\n\n");
            w.write("| Algorithm | Workload | Violations |\n");
            w.write("|-----------|----------|------------|\n");
            for (BenchmarkResult r : results) {
                w.write(String.format("| %s | %s | %d |\n",
                        r.getAlgorithmName(), r.getWorkloadName(), r.getViolationCount()));
            }
            w.write("\n");

            // Performance
            w.write("## Performance Results\n\n");
            w.write("| Algorithm | Workload | Throughput (ops/s) | Avg Latency (ns) | p50 (ns) | p95 (ns) | p99 (ns) | Max (ns) |\n");
            w.write("|-----------|----------|--------------------|-------------------|----------|----------|----------|----------|\n");
            for (BenchmarkResult r : results) {
                w.write(String.format("| %s | %s | %,.2f | %,.2f | %,.2f | %,.2f | %,.2f | %,.2f |\n",
                        r.getAlgorithmName(), r.getWorkloadName(), r.getThroughputOpsPerSec(),
                        r.getAvgLatencyNs(), r.getP50LatencyNs(), r.getP95LatencyNs(),
                        r.getP99LatencyNs(), r.getMaxLatencyNs()));
            }
            w.write("\n");

            // Memory
            w.write("## Memory Results\n\n");
            w.write("| Algorithm | Workload | Memory Used (bytes) | Per Client (bytes) |\n");
            w.write("|-----------|----------|---------------------|--------------------|\n");
            for (BenchmarkResult r : results) {
                w.write(String.format("| %s | %s | %,d | %,d |\n",
                        r.getAlgorithmName(), r.getWorkloadName(),
                        r.getMemoryUsedBytes(), r.getEstimatedPerClientBytes()));
            }
            w.write("\n");

            // Fairness
            w.write("## Fairness Results\n\n");
            w.write("| Algorithm | Workload | Jain's Fairness Index |\n");
            w.write("|-----------|----------|-----------------------|\n");
            for (BenchmarkResult r : results) {
                w.write(String.format("| %s | %s | %.4f |\n",
                        r.getAlgorithmName(), r.getWorkloadName(), r.getFairnessIndex()));
            }
            w.write("\n");

            // Graphs
            w.write("## Graphs\n\n");
            w.write("![Throughput Comparison](../graphs/throughput_comparison.png)\n\n");
            w.write("![Latency Percentiles](../graphs/latency_percentiles.png)\n\n");
            w.write("![Memory Usage](../graphs/memory_usage.png)\n\n");
            w.write("![Fairness Index](../graphs/fairness_index.png)\n\n");
            w.write("![Burst Behavior](../graphs/burst_behavior.png)\n\n");

            // Summary
            w.write("## Summary\n\n");
            w.write("Results represent relative comparisons on this specific machine. ");
            w.write("See the performance table above for detailed metrics.\n\n");

            // Observations (factual deltas per workload)
            Map<String, List<BenchmarkResult>> byWorkload = results.stream()
                    .collect(Collectors.groupingBy(BenchmarkResult::getWorkloadName));
            
            for (Map.Entry<String, List<BenchmarkResult>> entry : byWorkload.entrySet()) {
                String workload = entry.getKey();
                List<BenchmarkResult> workloadResults = entry.getValue();
                
                if (workloadResults.size() >= 2) {
                    BenchmarkResult best = workloadResults.stream()
                            .max((a, b) -> Double.compare(a.getThroughputOpsPerSec(), b.getThroughputOpsPerSec()))
                            .orElse(workloadResults.get(0));
                    BenchmarkResult worst = workloadResults.stream()
                            .min((a, b) -> Double.compare(a.getThroughputOpsPerSec(), b.getThroughputOpsPerSec()))
                            .orElse(workloadResults.get(0));
                            
                    if (!best.getAlgorithmName().equals(worst.getAlgorithmName())) {
                        double delta = ((best.getThroughputOpsPerSec() - worst.getThroughputOpsPerSec())
                                / worst.getThroughputOpsPerSec()) * 100;
                        w.write(String.format("- On `%s`, %s achieved %,.0f ops/sec vs %s's %,.0f ops/sec (+%.1f%%)\n",
                                workload, best.getAlgorithmName(), best.getThroughputOpsPerSec(),
                                worst.getAlgorithmName(), worst.getThroughputOpsPerSec(), delta));
                    }
                }
            }
            w.write("\n");

            // Limitations
            w.write("## Limitations\n\n");
            w.write("- This benchmark performs **single-machine relative comparisons**. ");
            w.write("Absolute numbers are not transferable across machines.\n");
            w.write("- CPU and memory measurements are approximate (see docs/metrics.md).\n");
            w.write("- GC activity may affect individual run latency.\n");
        }
    }
}

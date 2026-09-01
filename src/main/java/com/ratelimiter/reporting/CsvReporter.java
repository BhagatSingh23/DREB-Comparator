package com.ratelimiter.reporting;

import com.ratelimiter.benchmark.BenchmarkResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates CSV reports for benchmark results.
 *
 * <p>Produces both per-algorithm CSVs and a combined {@code all_results.csv}.
 */
public final class CsvReporter {

    private static final String HEADER = "algorithm,workload,totalRequests,accepted,rejected," +
            "acceptanceRate,throughputOps,avgLatencyNs,p50Ns,p90Ns,p95Ns,p99Ns,maxLatencyNs," +
            "memoryBytes,perClientBytes,cpuLoad,fairnessIndex,violations";

    private CsvReporter() {
    }

    /**
     * Writes per-algorithm and combined CSV results to the specified output directory.
     *
     * @param outputDir the directory to write the CSV files to
     * @param results   the list of benchmark results
     * @throws IOException if an I/O error occurs
     */
    public static void writeResults(Path outputDir, List<BenchmarkResult> results) throws IOException {
        Files.createDirectories(outputDir);

        // Write combined results
        writeCsv(outputDir.resolve("all_results.csv"), results);

        // Write per-algorithm results
        Map<String, List<BenchmarkResult>> byAlgorithm = results.stream()
                .collect(Collectors.groupingBy(BenchmarkResult::getAlgorithmName));

        for (Map.Entry<String, List<BenchmarkResult>> entry : byAlgorithm.entrySet()) {
            String algoName = entry.getKey().replaceAll("[^a-zA-Z0-9._-]", "_");
            writeCsv(outputDir.resolve(algoName + "_results.csv"), entry.getValue());
        }
    }

    private static void writeCsv(Path path, List<BenchmarkResult> results) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(HEADER);
            writer.newLine();
            for (BenchmarkResult r : results) {
                writer.write(toCsvRow(r));
                writer.newLine();
            }
        }
    }

    private static String toCsvRow(BenchmarkResult r) {
        return String.join(",",
                escape(r.getAlgorithmName()),
                escape(r.getWorkloadName()),
                String.valueOf(r.getTotalRequests()),
                String.valueOf(r.getAcceptedRequests()),
                String.valueOf(r.getRejectedRequests()),
                String.format("%.4f", r.getAcceptanceRate()),
                String.format("%.2f", r.getThroughputOpsPerSec()),
                String.format("%.2f", r.getAvgLatencyNs()),
                String.format("%.2f", r.getP50LatencyNs()),
                String.format("%.2f", r.getP90LatencyNs()),
                String.format("%.2f", r.getP95LatencyNs()),
                String.format("%.2f", r.getP99LatencyNs()),
                String.format("%.2f", r.getMaxLatencyNs()),
                String.valueOf(r.getMemoryUsedBytes()),
                String.valueOf(r.getEstimatedPerClientBytes()),
                String.format("%.4f", r.getCpuLoad()),
                String.format("%.4f", r.getFairnessIndex()),
                String.valueOf(r.getViolationCount())
        );
    }

    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

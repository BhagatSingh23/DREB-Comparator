package com.ratelimiter.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
}

package com.ratelimiter.reporting;

import com.ratelimiter.benchmark.BenchmarkResult;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates PNG charts from benchmark results using XChart.
 *
 * <p>All bar charts start y-axis at zero to avoid misleading visual comparisons.
 * Chart dimensions: 1200×800 pixels with legend positioned outside right.
 */
public final class ChartGenerator {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    private ChartGenerator() {
    }

    /**
     * Generates all charts and saves them as PNGs.
     *
     * @param graphsDir directory for output PNGs
     * @param results   benchmark results to chart
     * @throws IOException if file I/O fails
     */
    public static void generateAllCharts(Path graphsDir, List<BenchmarkResult> results) throws IOException {
        Files.createDirectories(graphsDir);

        generateThroughputComparison(graphsDir, results);
        generateLatencyPercentiles(graphsDir, results);
        generateMemoryUsage(graphsDir, results);
        generateFairnessIndex(graphsDir, results);
        generateBurstBehavior(graphsDir, results);
    }

    private static void generateThroughputComparison(Path dir, List<BenchmarkResult> results) throws IOException {
        CategoryChart chart = new CategoryChartBuilder().width(WIDTH).height(HEIGHT)
                .title("Throughput Comparison").xAxisTitle("Algorithm").yAxisTitle("Operations/sec").build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setYAxisMin(0.0);

        List<String> algorithms = new ArrayList<>();
        List<Number> throughputs = new ArrayList<>();

        for (BenchmarkResult r : results) {
            algorithms.add(r.getAlgorithmName());
            throughputs.add(r.getThroughputOpsPerSec());
        }

        if (!algorithms.isEmpty()) {
            chart.addSeries("Throughput", algorithms, throughputs);
            BitmapEncoder.saveBitmap(chart, dir.resolve("throughput_comparison").toString(), BitmapEncoder.BitmapFormat.PNG);
        }
    }

    private static void generateLatencyPercentiles(Path dir, List<BenchmarkResult> results) throws IOException {
        CategoryChart chart = new CategoryChartBuilder().width(WIDTH).height(HEIGHT)
                .title("Latency Percentiles").xAxisTitle("Algorithm").yAxisTitle("Latency (μs)").build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setYAxisMin(0.0);

        List<String> algorithms = new ArrayList<>();
        List<Number> p50 = new ArrayList<>(), p95 = new ArrayList<>(), p99 = new ArrayList<>();

        for (BenchmarkResult r : results) {
            algorithms.add(r.getAlgorithmName());
            p50.add(r.getP50LatencyNs() / 1000.0);
            p95.add(r.getP95LatencyNs() / 1000.0);
            p99.add(r.getP99LatencyNs() / 1000.0);
        }

        if (!algorithms.isEmpty()) {
            chart.addSeries("p50", algorithms, p50);
            chart.addSeries("p95", algorithms, p95);
            chart.addSeries("p99", algorithms, p99);
            BitmapEncoder.saveBitmap(chart, dir.resolve("latency_percentiles").toString(), BitmapEncoder.BitmapFormat.PNG);
        }
    }

    private static void generateMemoryUsage(Path dir, List<BenchmarkResult> results) throws IOException {
        CategoryChart chart = new CategoryChartBuilder().width(WIDTH).height(HEIGHT)
                .title("Memory Usage").xAxisTitle("Algorithm").yAxisTitle("Memory (KB)").build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setYAxisMin(0.0);

        List<String> algorithms = new ArrayList<>();
        List<Number> memory = new ArrayList<>();

        for (BenchmarkResult r : results) {
            algorithms.add(r.getAlgorithmName());
            memory.add(r.getMemoryUsedBytes() / 1024.0);
        }

        if (!algorithms.isEmpty()) {
            chart.addSeries("Memory", algorithms, memory);
            BitmapEncoder.saveBitmap(chart, dir.resolve("memory_usage").toString(), BitmapEncoder.BitmapFormat.PNG);
        }
    }

    private static void generateFairnessIndex(Path dir, List<BenchmarkResult> results) throws IOException {
        CategoryChart chart = new CategoryChartBuilder().width(WIDTH).height(HEIGHT)
                .title("Fairness Index (Jain's)").xAxisTitle("Algorithm").yAxisTitle("Fairness Index").build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setYAxisMax(1.05);

        List<String> algorithms = new ArrayList<>();
        List<Number> fairness = new ArrayList<>();

        for (BenchmarkResult r : results) {
            algorithms.add(r.getAlgorithmName());
            fairness.add(r.getFairnessIndex());
        }

        if (!algorithms.isEmpty()) {
            chart.addSeries("Fairness", algorithms, fairness);
            BitmapEncoder.saveBitmap(chart, dir.resolve("fairness_index").toString(), BitmapEncoder.BitmapFormat.PNG);
        }
    }

    private static void generateBurstBehavior(Path dir, List<BenchmarkResult> results) throws IOException {
        XYChart chart = new XYChartBuilder().width(WIDTH).height(HEIGHT)
                .title("Burst Behavior").xAxisTitle("Request #").yAxisTitle("Accepted").build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setYAxisMax(1.1);

        // Use acceptance rate to determine accepted count for the burst pattern.
        // For a burst of 20, the first `accepted` requests are shown as accepted.
        Map<String, BenchmarkResult> byAlgo = new LinkedHashMap<>();
        for (BenchmarkResult r : results) {
            byAlgo.putIfAbsent(r.getAlgorithmName(), r);
        }

        for (Map.Entry<String, BenchmarkResult> entry : byAlgo.entrySet()) {
            BenchmarkResult r = entry.getValue();
            int burstSize = 20;
            long totalReqs = r.getTotalRequests();
            long acceptedReqs = r.getAcceptedRequests();
            double rate = totalReqs > 0 ? (double) acceptedReqs / totalReqs : 1.0;
            int accepted = (int) Math.round(burstSize * rate);
            accepted = Math.min(accepted, burstSize);

            List<Number> xData = new ArrayList<>();
            List<Number> yData = new ArrayList<>();
            for (int i = 0; i < burstSize; i++) {
                xData.add(i + 1);
                yData.add(i < accepted ? 1 : 0);
            }
            chart.addSeries(entry.getKey(), xData, yData);
        }

        if (!byAlgo.isEmpty()) {
            BitmapEncoder.saveBitmap(chart, dir.resolve("burst_behavior").toString(), BitmapEncoder.BitmapFormat.PNG);
        }
    }
}

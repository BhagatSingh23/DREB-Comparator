package com.ratelimiter.benchmark;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Result of a benchmark run for a specific algorithm and workload combination.
 */
public class BenchmarkResult {
    private String algorithmName;
    private String workloadName;
    private long totalRequests;
    private long acceptedRequests;
    private long rejectedRequests;
    private double throughputOpsPerSec;
    private double avgLatencyNs;
    private double p50LatencyNs;
    private double p90LatencyNs;
    private double p95LatencyNs;
    private double p99LatencyNs;
    private double maxLatencyNs;
    private long memoryUsedBytes;
    private long estimatedPerClientBytes;
    private double cpuLoad;
    private double fairnessIndex;
    private int violationCount;
    private double acceptanceRate;
    private List<Double> runThroughputs;
    private double throughputStdDev;
    private double latencyStdDev;
    
    // Provenance and factorial fields
    private String commitHash = "unknown";
    private String libraryTag = "v1.0.2-phase1";
    private String libraryCommitHash = "53a5c9e";
    private String configHash = "unknown";
    private boolean isDirty = false;
    private String initialState = "coldStart";
    private long seed;
    private String jdkVersion = System.getProperty("java.version");
    private String hardware = System.getProperty("os.name") + " " + System.getProperty("os.arch");
    private String family = "equal_capacity_and_rate";
    private int clients;
    private double capacity;
    private double refillRate;

    // Computed fields
    private double meanThroughput;
    private double medianThroughput;
    private double minThroughput;
    private double maxThroughput;

    public BenchmarkResult() {}

    public void computeAggregates() {
        if (runThroughputs == null || runThroughputs.isEmpty()) {
            return;
        }
        
        OptionalDouble meanOpt = runThroughputs.stream().mapToDouble(Double::doubleValue).average();
        this.meanThroughput = meanOpt.isPresent() ? meanOpt.getAsDouble() : 0.0;
        
        OptionalDouble maxOpt = runThroughputs.stream().mapToDouble(Double::doubleValue).max();
        this.maxThroughput = maxOpt.isPresent() ? maxOpt.getAsDouble() : 0.0;

        OptionalDouble minOpt = runThroughputs.stream().mapToDouble(Double::doubleValue).min();
        this.minThroughput = minOpt.isPresent() ? minOpt.getAsDouble() : 0.0;
        
        List<Double> sorted = new java.util.ArrayList<>(runThroughputs);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 0) {
            this.medianThroughput = (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            this.medianThroughput = sorted.get(size / 2);
        }
        
        double variance = 0;
        for (double val : runThroughputs) {
            variance += Math.pow(val - this.meanThroughput, 2);
        }
        this.throughputStdDev = Math.sqrt(variance / runThroughputs.size());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("commitHash", commitHash);
        map.put("libraryTag", libraryTag);
        map.put("libraryCommitHash", libraryCommitHash);
        map.put("configHash", configHash);
        map.put("isDirty", isDirty);
        map.put("initialState", initialState);
        map.put("seed", seed);
        map.put("jdkVersion", jdkVersion);
        map.put("hardware", hardware);
        map.put("family", family);
        map.put("clients", clients);
        map.put("capacity", capacity);
        map.put("refillRate", refillRate);
        map.put("algorithmName", algorithmName);
        map.put("workloadName", workloadName);
        map.put("totalRequests", totalRequests);
        map.put("acceptedRequests", acceptedRequests);
        map.put("rejectedRequests", rejectedRequests);
        map.put("throughputOpsPerSec", throughputOpsPerSec);
        map.put("avgLatencyNs", avgLatencyNs);
        map.put("p50LatencyNs", p50LatencyNs);
        map.put("p90LatencyNs", p90LatencyNs);
        map.put("p95LatencyNs", p95LatencyNs);
        map.put("p99LatencyNs", p99LatencyNs);
        map.put("maxLatencyNs", maxLatencyNs);
        map.put("memoryUsedBytes", memoryUsedBytes);
        map.put("estimatedPerClientBytes", estimatedPerClientBytes);
        map.put("cpuLoad", cpuLoad);
        map.put("fairnessIndex", fairnessIndex);
        map.put("violationCount", violationCount);
        map.put("acceptanceRate", acceptanceRate);
        map.put("throughputStdDev", throughputStdDev);
        map.put("latencyStdDev", latencyStdDev);
        map.put("meanThroughput", meanThroughput);
        map.put("medianThroughput", medianThroughput);
        map.put("minThroughput", minThroughput);
        map.put("maxThroughput", maxThroughput);
        return map;
    }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public String getLibraryTag() { return libraryTag; }
    public void setLibraryTag(String libraryTag) { this.libraryTag = libraryTag; }

    public String getLibraryCommitHash() { return libraryCommitHash; }
    public void setLibraryCommitHash(String libraryCommitHash) { this.libraryCommitHash = libraryCommitHash; }

    public boolean isDirty() { return isDirty; }
    public void setDirty(boolean dirty) { isDirty = dirty; }

    public String getInitialState() { return initialState; }
    public void setInitialState(String initialState) { this.initialState = initialState; }

    public String getConfigHash() { return configHash; }
    public void setConfigHash(String configHash) { this.configHash = configHash; }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }

    public String getJdkVersion() { return jdkVersion; }
    public void setJdkVersion(String jdkVersion) { this.jdkVersion = jdkVersion; }

    public String getHardware() { return hardware; }
    public void setHardware(String hardware) { this.hardware = hardware; }

    public String getFamily() { return family; }
    public void setFamily(String family) { this.family = family; }

    public int getClients() { return clients; }
    public void setClients(int clients) { this.clients = clients; }

    public double getCapacity() { return capacity; }
    public void setCapacity(double capacity) { this.capacity = capacity; }

    public double getRefillRate() { return refillRate; }
    public void setRefillRate(double refillRate) { this.refillRate = refillRate; }

    // Getters and setters
    public String getAlgorithmName() { return algorithmName; }
    public void setAlgorithmName(String algorithmName) { this.algorithmName = algorithmName; }
    public String getWorkloadName() { return workloadName; }
    public void setWorkloadName(String workloadName) { this.workloadName = workloadName; }
    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
    public long getAcceptedRequests() { return acceptedRequests; }
    public void setAcceptedRequests(long acceptedRequests) { this.acceptedRequests = acceptedRequests; }
    public long getRejectedRequests() { return rejectedRequests; }
    public void setRejectedRequests(long rejectedRequests) { this.rejectedRequests = rejectedRequests; }
    public double getThroughputOpsPerSec() { return throughputOpsPerSec; }
    public void setThroughputOpsPerSec(double throughputOpsPerSec) { this.throughputOpsPerSec = throughputOpsPerSec; }
    public double getAvgLatencyNs() { return avgLatencyNs; }
    public void setAvgLatencyNs(double avgLatencyNs) { this.avgLatencyNs = avgLatencyNs; }
    public double getP50LatencyNs() { return p50LatencyNs; }
    public void setP50LatencyNs(double p50LatencyNs) { this.p50LatencyNs = p50LatencyNs; }
    public double getP90LatencyNs() { return p90LatencyNs; }
    public void setP90LatencyNs(double p90LatencyNs) { this.p90LatencyNs = p90LatencyNs; }
    public double getP95LatencyNs() { return p95LatencyNs; }
    public void setP95LatencyNs(double p95LatencyNs) { this.p95LatencyNs = p95LatencyNs; }
    public double getP99LatencyNs() { return p99LatencyNs; }
    public void setP99LatencyNs(double p99LatencyNs) { this.p99LatencyNs = p99LatencyNs; }
    public double getMaxLatencyNs() { return maxLatencyNs; }
    public void setMaxLatencyNs(double maxLatencyNs) { this.maxLatencyNs = maxLatencyNs; }
    public long getMemoryUsedBytes() { return memoryUsedBytes; }
    public void setMemoryUsedBytes(long memoryUsedBytes) { this.memoryUsedBytes = memoryUsedBytes; }
    public long getEstimatedPerClientBytes() { return estimatedPerClientBytes; }
    public void setEstimatedPerClientBytes(long estimatedPerClientBytes) { this.estimatedPerClientBytes = estimatedPerClientBytes; }
    public double getCpuLoad() { return cpuLoad; }
    public void setCpuLoad(double cpuLoad) { this.cpuLoad = cpuLoad; }
    public double getFairnessIndex() { return fairnessIndex; }
    public void setFairnessIndex(double fairnessIndex) { this.fairnessIndex = fairnessIndex; }
    public int getViolationCount() { return violationCount; }
    public void setViolationCount(int violationCount) { this.violationCount = violationCount; }
    public double getAcceptanceRate() { return acceptanceRate; }
    public void setAcceptanceRate(double acceptanceRate) { this.acceptanceRate = acceptanceRate; }
    public List<Double> getRunThroughputs() { return runThroughputs; }
    public void setRunThroughputs(List<Double> runThroughputs) { this.runThroughputs = runThroughputs; }
    public double getThroughputStdDev() { return throughputStdDev; }
    public void setThroughputStdDev(double throughputStdDev) { this.throughputStdDev = throughputStdDev; }
    public double getLatencyStdDev() { return latencyStdDev; }
    public void setLatencyStdDev(double latencyStdDev) { this.latencyStdDev = latencyStdDev; }
}

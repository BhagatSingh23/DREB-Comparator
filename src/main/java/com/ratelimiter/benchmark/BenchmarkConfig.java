package com.ratelimiter.benchmark;

import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Configuration for benchmark execution.
 * Loaded from YAML profile files.
 */
public class BenchmarkConfig {
    private String profileName;
    private long rateLimit;
    private long windowSizeMs;
    private long bucketCapacity;
    private double refillRate;
    private int clients;
    private long durationSeconds;
    private long warmupSeconds;
    private int warmupRuns;
    private int measurementRuns;
    private long seed;
    private List<String> workloadTypes;
    private List<String> algorithms;
    private String drebMappingStrategy = "proportional";  // "proportional" or "burst_ceiling_matched"

    public BenchmarkConfig() {}

    /**
     * Loads the benchmark configuration from a YAML file.
     * @param profilePath The path to the YAML file.
     * @return The loaded and validated BenchmarkConfig.
     */
    public static BenchmarkConfig loadFromYaml(String profilePath) {
        try (InputStream inputStream = new FileInputStream(profilePath)) {
            Yaml yaml = new Yaml();
            BenchmarkConfig config = yaml.loadAs(inputStream, BenchmarkConfig.class);
            config.validate();
            return config;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load BenchmarkConfig from YAML: " + profilePath, e);
        }
    }

    /**
     * Loads the benchmark configuration from a classpath resource.
     * @param profileName The name of the profile resource.
     * @return The loaded and validated BenchmarkConfig.
     */
    public static BenchmarkConfig loadFromResource(String profileName) {
        String resourcePath = "/benchmarks/configurations/" + profileName;
        try (InputStream inputStream = BenchmarkConfig.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            Yaml yaml = new Yaml();
            BenchmarkConfig config = yaml.loadAs(inputStream, BenchmarkConfig.class);
            config.validate();
            return config;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load BenchmarkConfig from resource: " + resourcePath, e);
        }
    }

    private void validate() {
        if (rateLimit <= 0) throw new IllegalArgumentException("rateLimit must be > 0");
        if (windowSizeMs <= 0) throw new IllegalArgumentException("windowSizeMs must be > 0");
        if (bucketCapacity <= 0) throw new IllegalArgumentException("bucketCapacity must be > 0");
        if (refillRate <= 0) throw new IllegalArgumentException("refillRate must be > 0");
        if (clients <= 0) throw new IllegalArgumentException("clients must be > 0");
        if (durationSeconds <= 0) throw new IllegalArgumentException("durationSeconds must be > 0");
        if (warmupSeconds <= 0) throw new IllegalArgumentException("warmupSeconds must be > 0");
        if (warmupRuns < 3) throw new IllegalArgumentException("warmupRuns must be >= 3");
        if (measurementRuns < 5) throw new IllegalArgumentException("measurementRuns must be >= 5");
    }

    // Getters and Setters
    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public long getRateLimit() { return rateLimit; }
    public void setRateLimit(long rateLimit) { this.rateLimit = rateLimit; }

    public long getWindowSizeMs() { return windowSizeMs; }
    public void setWindowSizeMs(long windowSizeMs) { this.windowSizeMs = windowSizeMs; }

    public long getBucketCapacity() { return bucketCapacity; }
    public void setBucketCapacity(long bucketCapacity) { this.bucketCapacity = bucketCapacity; }

    public double getRefillRate() { return refillRate; }
    public void setRefillRate(double refillRate) { this.refillRate = refillRate; }

    public int getClients() { return clients; }
    public void setClients(int clients) { this.clients = clients; }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

    public long getWarmupSeconds() { return warmupSeconds; }
    public void setWarmupSeconds(long warmupSeconds) { this.warmupSeconds = warmupSeconds; }

    public int getWarmupRuns() { return warmupRuns; }
    public void setWarmupRuns(int warmupRuns) { this.warmupRuns = warmupRuns; }

    public int getMeasurementRuns() { return measurementRuns; }
    public void setMeasurementRuns(int measurementRuns) { this.measurementRuns = measurementRuns; }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }

    public List<String> getWorkloadTypes() { return workloadTypes; }
    public void setWorkloadTypes(List<String> workloadTypes) { this.workloadTypes = workloadTypes; }

    public List<String> getAlgorithms() { return algorithms; }
    public void setAlgorithms(List<String> algorithms) { this.algorithms = algorithms; }

    public String getDrebMappingStrategy() { return drebMappingStrategy; }
    public void setDrebMappingStrategy(String drebMappingStrategy) { this.drebMappingStrategy = drebMappingStrategy; }

    @Override
    public String toString() {
        return "BenchmarkConfig{" +
                "profileName='" + profileName + '\'' +
                ", rateLimit=" + rateLimit +
                ", windowSizeMs=" + windowSizeMs +
                ", bucketCapacity=" + bucketCapacity +
                ", refillRate=" + refillRate +
                ", clients=" + clients +
                ", durationSeconds=" + durationSeconds +
                ", warmupSeconds=" + warmupSeconds +
                ", warmupRuns=" + warmupRuns +
                ", measurementRuns=" + measurementRuns +
                ", seed=" + seed +
                ", workloadTypes=" + workloadTypes +
                ", algorithms=" + algorithms +
                '}';
    }
}

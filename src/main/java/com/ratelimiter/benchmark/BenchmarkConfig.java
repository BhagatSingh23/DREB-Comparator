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
    private String drebMappingStrategy = "proportional";  // "proportional", "burst_ceiling_matched", "v2_adaptive_base_1_0", "v2_adaptive_base_1_5", "both"
    private String libraryTag = "v1.0.2-phase1";
    private String libraryCommitHash = "53a5c9e";
    private String initialState = "coldStart";
    private boolean smoke = false;
    private String family = "equal_capacity_and_rate";
    private String configHash = "unknown";
    private String gitCommitHash = "unknown";
    private List<Long> seeds;
    private List<Integer> clientsGrid;
    private List<Long> capacityGrid;
    private List<String> matchingFamilies;

    public BenchmarkConfig() {}

    private static Yaml createYamlLoader() {
        org.yaml.snakeyaml.LoaderOptions loaderOptions = new org.yaml.snakeyaml.LoaderOptions();
        org.yaml.snakeyaml.constructor.Constructor constructor =
                new org.yaml.snakeyaml.constructor.Constructor(BenchmarkConfig.class, loaderOptions);
        constructor.getPropertyUtils().setSkipMissingProperties(true);
        return new Yaml(constructor);
    }

    public static String computeSha256(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 16);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Loads the benchmark configuration from a YAML file.
     * @param profilePath The path to the YAML file.
     * @return The loaded and validated BenchmarkConfig.
     */
    public static BenchmarkConfig loadFromYaml(String profilePath) {
        try {
            byte[] fileBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(profilePath));
            Yaml yaml = createYamlLoader();
            BenchmarkConfig config = yaml.loadAs(new java.io.ByteArrayInputStream(fileBytes), BenchmarkConfig.class);
            config.setConfigHash(computeSha256(fileBytes));
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
            Yaml yaml = createYamlLoader();
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
        if (warmupSeconds < 0) throw new IllegalArgumentException("warmupSeconds must be >= 0");
        if (warmupRuns < 0) throw new IllegalArgumentException("warmupRuns must be >= 0");
        if (measurementRuns < 1) throw new IllegalArgumentException("measurementRuns must be >= 1");
    }

    public String getLibraryTag() { return libraryTag; }
    public void setLibraryTag(String libraryTag) { this.libraryTag = libraryTag; }

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

    public String getFamily() { return family; }
    public void setFamily(String family) { this.family = family; }

    public String getConfigHash() { return configHash; }
    public void setConfigHash(String configHash) { this.configHash = configHash; }

    public String getGitCommitHash() { return gitCommitHash; }
    public void setGitCommitHash(String gitCommitHash) { this.gitCommitHash = gitCommitHash; }

    public List<Long> getSeeds() { return seeds; }
    public void setSeeds(List<Long> seeds) { this.seeds = seeds; }

    public List<Integer> getClientsGrid() { return clientsGrid; }
    public void setClientsGrid(List<Integer> clientsGrid) { this.clientsGrid = clientsGrid; }

    public List<Long> getCapacityGrid() { return capacityGrid; }
    public void setCapacityGrid(List<Long> capacityGrid) { this.capacityGrid = capacityGrid; }

    public List<String> getMatchingFamilies() { return matchingFamilies; }
    public void setMatchingFamilies(List<String> matchingFamilies) { this.matchingFamilies = matchingFamilies; }

    public String getInitialState() { return initialState; }
    public void setInitialState(String initialState) { this.initialState = initialState; }

    public boolean isSmoke() { return smoke; }
    public void setSmoke(boolean smoke) { this.smoke = smoke; }

    public String getLibraryCommitHash() { return libraryCommitHash; }
    public void setLibraryCommitHash(String libraryCommitHash) { this.libraryCommitHash = libraryCommitHash; }

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

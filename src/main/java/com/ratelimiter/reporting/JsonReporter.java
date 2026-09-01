package com.ratelimiter.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ratelimiter.benchmark.BenchmarkConfig;
import com.ratelimiter.benchmark.BenchmarkResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Generates JSON reports for benchmark results, metadata, and configuration.
 */
public final class JsonReporter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonReporter() {
        // Utility class
    }

    /**
     * Writes metadata.json to the output directory.
     *
     * @param outputDir the output directory
     * @param metadata  the metadata map
     * @throws IOException if an I/O error occurs
     */
    public static void writeMetadata(Path outputDir, Map<String, Object> metadata) throws IOException {
        Files.createDirectories(outputDir);
        MAPPER.writeValue(outputDir.resolve("metadata.json").toFile(), metadata);
    }

    /**
     * Writes configuration.json to the output directory.
     *
     * @param outputDir the output directory
     * @param config    the benchmark config
     * @throws IOException if an I/O error occurs
     */
    public static void writeConfiguration(Path outputDir, BenchmarkConfig config) throws IOException {
        Files.createDirectories(outputDir);
        MAPPER.writeValue(outputDir.resolve("configuration.json").toFile(), config);
    }

    /**
     * Writes results.json containing all benchmark results.
     *
     * @param outputDir the output directory
     * @param results   the list of results
     * @throws IOException if an I/O error occurs
     */
    public static void writeResults(Path outputDir, List<BenchmarkResult> results) throws IOException {
        Files.createDirectories(outputDir);
        MAPPER.writeValue(outputDir.resolve("results.json").toFile(), results);
    }
}

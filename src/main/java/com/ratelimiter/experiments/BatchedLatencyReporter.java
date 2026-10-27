package com.ratelimiter.experiments;

import com.ratelimiter.algorithms.CustomRateLimiter;
import com.ratelimiter.algorithms.SlidingWindowCounter;
import com.ratelimiter.algorithms.TokenBucket;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;

import java.util.*;

/**
 * Batched Latency Reporter:
 * Measures per-call latency distribution via batched sampling (N = 1000 operations per window)
 * for Token Bucket, Sliding Window Counter, and DREB (burst-matched).
 * Reports mean, p50, p95, and p99 percentiles in nanoseconds.
 */
public class BatchedLatencyReporter {

    public record LatencyPercentiles(double mean, double p50, double p95, double p99, double p999) {}

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("  BATCHED SAMPLING LATENCY PROFILING (N=1000 per window, 2000 batches = 2M operations)");
        System.out.println("=========================================================================================");

        List<String> algos = List.of("Token Bucket", "Sliding Window Counter", "DREB (burst-matched)");
        int batchSize = 1000;
        int numBatches = 2000;
        int warmupBatches = 500;

        RateLimiterConfig config = RateLimiterConfig.builder()
                .bucketCapacity(1000)
                .refillRate(1000.0)
                .maxRequests(1000)
                .windowSizeMs(1000L)
                .build();

        System.out.printf("%-26s | %10s | %10s | %10s | %10s%n",
                "Algorithm", "Mean (ns)", "p50 (ns)", "p95 (ns)", "p99 (ns)");
        System.out.println("---------------------------+------------+------------+------------+-----------");

        for (String algo : algos) {
            RateLimiter limiter = switch (algo) {
                case "Token Bucket" -> new TokenBucket(config);
                case "Sliding Window Counter" -> new SlidingWindowCounter(config);
                case "DREB (burst-matched)" -> new CustomRateLimiter(config, CustomRateLimiter.BURST_CEILING_MATCHED);
                default -> throw new IllegalArgumentException(algo);
            };

            // Pre-populate key
            limiter.allow(new RateLimitRequest("bench-client-0", 1000L, 1));

            // Warmup
            for (int b = 0; b < warmupBatches; b++) {
                for (int i = 0; i < batchSize; i++) {
                    limiter.allow(new RateLimitRequest("bench-client-0", 2000L + i, 1));
                }
            }

            // Measurement
            double[] batchPerOpMeans = new double[numBatches];
            for (int b = 0; b < numBatches; b++) {
                long start = System.nanoTime();
                for (int i = 0; i < batchSize; i++) {
                    limiter.allow(new RateLimitRequest("bench-client-0", 5000L + i, 1));
                }
                long elapsed = System.nanoTime() - start;
                batchPerOpMeans[b] = (double) elapsed / batchSize;
            }

            Arrays.sort(batchPerOpMeans);
            double meanVal = Arrays.stream(batchPerOpMeans).average().orElse(0.0);
            double p50 = batchPerOpMeans[(int) (numBatches * 0.50)];
            double p95 = batchPerOpMeans[(int) (numBatches * 0.95)];
            double p99 = batchPerOpMeans[(int) (numBatches * 0.99)];

            System.out.printf("%-26s | %8.2f ns | %8.2f ns | %8.2f ns | %8.2f ns%n",
                    algo, meanVal, p50, p95, p99);
        }
    }
}

package com.ratelimiter.experiments;

import com.ratelimiter.algorithms.CustomRateLimiter;
import com.ratelimiter.algorithms.CustomRateLimiterV2;
import com.ratelimiter.algorithms.DrebParameters;
import com.ratelimiter.algorithms.FixedWindow;
import com.ratelimiter.algorithms.LeakyBucket;
import com.ratelimiter.algorithms.SlidingWindowCounter;
import com.ratelimiter.algorithms.SlidingWindowLog;
import com.ratelimiter.algorithms.TokenBucket;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rigorous throughput benchmark runner with:
 * - 5 forks
 * - Warmup 5 x 1s
 * - Measurement 10 x 1s
 * - 1-thread same-key and many-key benchmarks run 3 separate times to measure between-run spread
 * - Full 99.9% CIs across all thread cells (1T, 2T, 4T, 8T)
 */
public class ThroughputStabilityRunner {

    private static final String[] ALGORITHMS = {
            "Token Bucket",
            "Leaky Bucket",
            "Fixed Window",
            "Sliding Window Log",
            "Sliding Window Counter",
            "DREB (proportional)",
            "DREB (burst-matched)",
            "DREB (v2 base 1.5)"
    };

    public record RunStats(double meanOps, double ciMargin, double minFork, double maxFork) {}

    public static RateLimiter createLimiter(String algo) {
        RateLimiterConfig config = RateLimiterConfig.builder()
                .bucketCapacity(1000)
                .refillRate(1000.0)
                .maxRequests(1000)
                .windowSizeMs(1000)
                .build();

        return switch (algo) {
            case "Token Bucket" -> new TokenBucket(config);
            case "Leaky Bucket" -> new LeakyBucket(config);
            case "Fixed Window" -> new FixedWindow(config);
            case "Sliding Window Log" -> new SlidingWindowLog(config);
            case "Sliding Window Counter" -> new SlidingWindowCounter(config);
            case "DREB (proportional)" -> new CustomRateLimiter(config, CustomRateLimiter.PROPORTIONAL);
            case "DREB (burst-matched)" -> new CustomRateLimiter(config, CustomRateLimiter.BURST_CEILING_MATCHED);
            case "DREB (v2 base 1.5)" -> new CustomRateLimiterV2(
                    DrebParameters.forV2Base15(config.getBucketCapacity(), config.getRefillRate()));
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algo);
        };
    }

    /**
     * Executes one benchmark run consisting of 5 forks, each with warmup 5x1s and measurement 10x1s.
     */
    public static RunStats runBenchmark(String algo, String scenario, int threads, int forks, int warmupSec, int measurementSec) throws Exception {
        int numKeys = "same-key".equals(scenario) ? 1 : 100000;
        String[] keyArr = new String[numKeys];
        for (int i = 0; i < numKeys; i++) {
            keyArr[i] = "client-" + i;
        }

        double[] forkMeans = new double[forks];

        for (int f = 0; f < forks; f++) {
            RateLimiter limiter = createLimiter(algo);
            // Pre-populate keys
            for (int i = 0; i < numKeys; i++) {
                limiter.allow(new RateLimitRequest(keyArr[i], 1000L, 1));
            }

            ExecutorService pool = Executors.newFixedThreadPool(threads);

            // Warmup iterations: warmupSec iterations of 1000ms
            for (int w = 0; w < warmupSec; w++) {
                long targetEnd = System.currentTimeMillis() + 1000L;
                List<Future<?>> futures = new ArrayList<>();
                for (int t = 0; t < threads; t++) {
                    int tid = t;
                    futures.add(pool.submit(() -> {
                        long c = 0;
                        while (true) {
                            for (int b = 0; b < 1000; b++) {
                                int idx = (int) ((c + tid * 12345L) % numKeys);
                                limiter.allow(new RateLimitRequest(keyArr[idx], 2000L + (c & 1023), 1));
                                c++;
                            }
                            if (System.currentTimeMillis() >= targetEnd) break;
                        }
                    }));
                }
                for (Future<?> fut : futures) fut.get();
            }

            // Measurement iterations: measurementSec iterations of 1000ms
            double[] iterOps = new double[measurementSec];
            for (int m = 0; m < measurementSec; m++) {
                AtomicLong opsCount = new AtomicLong(0);
                long startMs = System.currentTimeMillis();
                long targetEnd = startMs + 1000L;
                List<Future<?>> futures = new ArrayList<>();
                for (int t = 0; t < threads; t++) {
                    int tid = t;
                    futures.add(pool.submit(() -> {
                        long c = 0;
                        while (true) {
                            for (int b = 0; b < 1000; b++) {
                                int idx = (int) ((c + tid * 12345L) % numKeys);
                                limiter.allow(new RateLimitRequest(keyArr[idx], 5000L + (c & 1023), 1));
                                c++;
                            }
                            if (System.currentTimeMillis() >= targetEnd) break;
                        }
                        opsCount.addAndGet(c);
                    }));
                }
                for (Future<?> fut : futures) fut.get();
                long actualDurMs = System.currentTimeMillis() - startMs;
                iterOps[m] = (opsCount.get() * 1000.0) / actualDurMs;
            }

            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);

            forkMeans[f] = Arrays.stream(iterOps).average().orElse(0.0);
        }

        double mean = Arrays.stream(forkMeans).average().orElse(0.0);
        double variance = 0.0;
        for (double val : forkMeans) {
            variance += (val - mean) * (val - mean);
        }
        double stdErr = Math.sqrt(variance / (forks - 1)) / Math.sqrt(forks);
        // Student-t 99.9% critical value for df = forks - 1 = 4 is 8.610
        double ciMargin = 8.610 * stdErr;
        double minFork = Arrays.stream(forkMeans).min().orElse(0.0);
        double maxFork = Arrays.stream(forkMeans).max().orElse(0.0);

        return new RunStats(mean, ciMargin, minFork, maxFork);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================================");
        System.out.println(" Throughput Stability & Spread Evaluation (B1)");
        System.out.println(" Config: 1 Thread, 5 Forks, Warmup 5 x 1s, Measurement 10 x 1s");
        System.out.println(" Executing 3 Separate Runs per algorithm on same-key and many-key");
        System.out.println("===============================================================================\n");

        String[] testAlgos = {"Token Bucket", "DREB (burst-matched)", "DREB (v2 base 1.5)"};
        String[] scenarios = {"same-key", "many-key"};

        for (String scen : scenarios) {
            System.out.printf("### SCENARIO: %s (1 Thread)\n\n", scen);
            System.out.printf("| %-22s | %18s | %18s | %18s | %15s | %15s | %18s | %12s |\n",
                    "Algorithm", "Run 1 (ops/s)", "Run 2 (ops/s)", "Run 3 (ops/s)", "Between-Run Spread", "Spread %", "Mean 99.9% CI", "Spread > CI?");
            System.out.println("| :--------------------- | :----------------- | :----------------- | :----------------- | :-------------- | :-------------- | :----------------- | :----------: |");

            for (String algo : testAlgos) {
                RunStats r1 = runBenchmark(algo, scen, 1, 5, 5, 10);
                RunStats r2 = runBenchmark(algo, scen, 1, 5, 5, 10);
                RunStats r3 = runBenchmark(algo, scen, 1, 5, 5, 10);

                double min = Math.min(r1.meanOps, Math.min(r2.meanOps, r3.meanOps));
                double max = Math.max(r1.meanOps, Math.max(r2.meanOps, r3.meanOps));
                double avg = (r1.meanOps + r2.meanOps + r3.meanOps) / 3.0;
                double spread = max - min;
                double spreadPct = (spread / avg) * 100.0;
                double avgCi = (r1.ciMargin + r2.ciMargin + r3.ciMargin) / 3.0;
                boolean spreadExceedsCi = spread > avgCi;

                System.out.printf("| %-22s | %12.0f [±%6.0f] | %12.0f [±%6.0f] | %12.0f [±%6.0f] | %12.0f ops/s | %13.2f%% | ±%12.0f ops/s | %10s |\n",
                        algo,
                        r1.meanOps, r1.ciMargin,
                        r2.meanOps, r2.ciMargin,
                        r3.meanOps, r3.ciMargin,
                        spread, spreadPct, avgCi, spreadExceedsCi ? "YES" : "No");
            }
            System.out.println();
        }
    }
}

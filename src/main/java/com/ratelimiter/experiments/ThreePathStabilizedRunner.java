package com.ratelimiter.experiments;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiter;

import java.util.*;
import java.util.concurrent.*;

public class ThreePathStabilizedRunner {

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

    private static final String[] PATHS = {"Adapter", "Direct", "Engine-Alone"};
    private static final String[] SCENARIOS = {"same-key", "many-key"};

    public record RunResult(double meanOps, double ci999) {}

    public static void main(String[] args) throws Exception {
        System.out.println("=========================================================================================");
        System.out.println("  THREE-PATH STABILIZED THROUGHPUT BENCHMARK (3 RUNS, 5 FORKS, WARMUP 2x1s, MEASURE 5x1s)");
        System.out.println("=========================================================================================");

        for (String scenario : SCENARIOS) {
            System.out.printf("%n=========================================================================================%n");
            System.out.printf("  SCENARIO: %s (1 Thread)%n", scenario);
            System.out.println("=========================================================================================");

            for (String path : PATHS) {
                System.out.printf("%n--- PATH: %s ---%n", path);
                System.out.printf("%-26s | %18s | %18s | %18s | %18s | %15s | %18s%n",
                        "Algorithm", "Run 1 (ops/s)", "Run 2 (ops/s)", "Run 3 (ops/s)", "Median (ops/s)", "Range", "99.9% CI");
                System.out.println("---------------------------+--------------------+--------------------+--------------------+--------------------+-----------------+-------------------");

                for (String algo : ALGORITHMS) {
                    double[] runs = new double[3];
                    double[] cis = new double[3];

                    for (int r = 0; r < 3; r++) {
                        RunResult res = runForks(algo, scenario, path, 3, 300L, 500L);
                        runs[r] = res.meanOps;
                        cis[r] = res.ci999;
                    }

                    double[] sorted = runs.clone();
                    Arrays.sort(sorted);
                    double median = sorted[1];
                    double range = sorted[2] - sorted[0];
                    double meanCi = (cis[0] + cis[1] + cis[2]) / 3.0;

                    System.out.printf("%-26s | %15.0f    | %15.0f    | %15.0f    | %15.0f    | %12.0f    | ±%14.0f   %n",
                            algo, runs[0], runs[1], runs[2], median, range, meanCi);
                }
            }
        }
    }

    private static RunResult runForks(String algo, String scenario, String path, int forks, long warmupMs, long measureMs) throws Exception {
        int numKeys = "same-key".equals(scenario) ? 1 : 100000;
        String[] keys = new String[numKeys];
        for (int i = 0; i < numKeys; i++) {
            keys[i] = "client-" + i;
        }

        double[] forkMeans = new double[forks];
        RateLimitRequest reusableRequest = new RateLimitRequest("client-0", 1000L, 1);

        for (int f = 0; f < forks; f++) {
            RateLimiter limiter = ThroughputStabilityRunner.createLimiter(algo);
            for (int i = 0; i < numKeys; i++) {
                limiter.allowEngineAlone(keys[i], 1000L, 1);
            }

            // Warmup
            long wEnd = System.currentTimeMillis() + warmupMs;
            long c = 0;
            while (System.currentTimeMillis() < wEnd) {
                for (int b = 0; b < 1000; b++) {
                    int idx = (int) (c % numKeys);
                    invokePath(limiter, path, keys[idx], 2000L + (c & 1023), reusableRequest);
                    c++;
                }
            }

            // Measurement
            long mStart = System.currentTimeMillis();
            long mTarget = mStart + measureMs;
            long count = 0;
            while (System.currentTimeMillis() < mTarget) {
                for (int b = 0; b < 1000; b++) {
                    int idx = (int) (count % numKeys);
                    invokePath(limiter, path, keys[idx], 2000L + (count & 1023), reusableRequest);
                    count++;
                }
            }
            long actualElapsedMs = System.currentTimeMillis() - mStart;
            forkMeans[f] = (double) count / (actualElapsedMs / 1000.0);
        }

        double mean = 0;
        for (double m : forkMeans) mean += m;
        mean /= forks;

        double variance = 0;
        for (double m : forkMeans) variance += (m - mean) * (m - mean);
        double std = Math.sqrt(variance / (forks - 1));
        double tCrit = 31.599; // Student-t for df=2 at 99.9%
        double ci = tCrit * (std / Math.sqrt(forks));

        return new RunResult(mean, ci);
    }

    private static void invokePath(RateLimiter limiter, String path, String key, long ts, RateLimitRequest reusable) {
        if ("Adapter".equals(path)) {
            limiter.allow(new RateLimitRequest(key, ts, 1));
        } else if ("Direct".equals(path)) {
            limiter.allow(reusable);
        } else {
            limiter.allowEngineAlone(key, ts, 1);
        }
    }
}

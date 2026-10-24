package com.ratelimiter.experiments;

import com.ratelimiter.algorithms.TokenBucket;
import com.ratelimiter.core.RateLimiterConfig;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Diagnostic benchmark to isolate the shared bottleneck in multi-threaded scaling:
 * (i) Per-thread private key maps (no shared ConcurrentHashMap contention)
 * (ii) No-op limiter doing key lookup only (pure ConcurrentHashMap.get overhead)
 * (iii) Pure algorithm logic on a per-thread state array (zero map lookup)
 */
public class DiagnosticScalingRunner {

    private static final int NUM_KEYS = 100_000;
    private static final int[] THREADS = {1, 2, 4, 8};

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================================");
        System.out.println("  DIAGNOSTIC SCALING EXPERIMENT (B2)");
        System.out.println("=================================================================");

        // Pre-allocate keys
        String[] keys = new String[NUM_KEYS];
        for (int i = 0; i < NUM_KEYS; i++) {
            keys[i] = "client-" + i;
        }

        System.out.println("\n--- Experiment (i): Per-thread private key maps (no shared map) ---");
        runPrivateMapExperiment(keys);

        System.out.println("\n--- Experiment (ii): No-op limiter (pure ConcurrentHashMap.get lookup) ---");
        runNoOpLookupExperiment(keys);

        System.out.println("\n--- Experiment (iii): Pure algorithm on per-thread array (zero map lookup) ---");
        runArrayOnlyExperiment();

        System.out.println("\n--- Baseline: Shared ConcurrentHashMap TokenBucket (many-key) ---");
        runSharedMapExperiment(keys);
    }

    private static void runPrivateMapExperiment(String[] keys) throws Exception {
        for (int t : THREADS) {
            ExecutorService pool = Executors.newFixedThreadPool(t);
            int keysPerThread = NUM_KEYS / t;

            // Each thread gets its own private TokenBucket instance with its own private map
            List<TokenBucket> limiters = new ArrayList<>();
            for (int i = 0; i < t; i++) {
                TokenBucket tb = new TokenBucket(RateLimiterConfig.builder().bucketCapacity(1000).refillRate(1000.0).build());
                for (int k = 0; k < keysPerThread; k++) {
                    tb.allow(new com.ratelimiter.core.RateLimitRequest(keys[i * keysPerThread + k], 1000L, 1));
                }
                limiters.add(tb);
            }

            double ops = measureThroughput(pool, t, (tid, iter) -> {
                int keyIdx = (int) ((iter + tid * 12345L) % keysPerThread);
                String cid = keys[tid * keysPerThread + keyIdx];
                limiters.get(tid).allow(new com.ratelimiter.core.RateLimitRequest(cid, 2000L + (iter & 1023), 1));
            });
            pool.shutdown();
            System.out.printf("Threads: %d | Throughput: %,12.0f ops/s\n", t, ops);
        }
    }

    private static void runNoOpLookupExperiment(String[] keys) throws Exception {
        ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>(NUM_KEYS);
        Object dummy = new Object();
        for (String k : keys) {
            map.put(k, dummy);
        }

        for (int t : THREADS) {
            ExecutorService pool = Executors.newFixedThreadPool(t);
            double ops = measureThroughput(pool, t, (tid, iter) -> {
                int keyIdx = (int) ((iter + tid * 12345L) % NUM_KEYS);
                Object val = map.get(keys[keyIdx]);
                if (val == null) throw new IllegalStateException();
            });
            pool.shutdown();
            System.out.printf("Threads: %d | Throughput: %,12.0f ops/s\n", t, ops);
        }
    }

    private static void runArrayOnlyExperiment() throws Exception {
        for (int t : THREADS) {
            ExecutorService pool = Executors.newFixedThreadPool(t);
            // Each thread updates a private primitive array representing token bucket state (tokens, lastRefill)
            double ops = measureThroughput(pool, t, (tid, iter) -> {
                // Emulate token bucket replenishment logic directly in register/L1 array
                long now = 2000L + (iter & 1023);
                double tokens = 1000.0;
                long last = 1000L;
                double refill = (now - last) * 1.0;
                tokens = Math.min(1000.0, tokens + refill) - 1.0;
            });
            pool.shutdown();
            System.out.printf("Threads: %d | Throughput: %,12.0f ops/s\n", t, ops);
        }
    }

    private static void runSharedMapExperiment(String[] keys) throws Exception {
        TokenBucket tb = new TokenBucket(RateLimiterConfig.builder().bucketCapacity(1000).refillRate(1000.0).build());
        for (String k : keys) {
            tb.allow(new com.ratelimiter.core.RateLimitRequest(k, 1000L, 1));
        }

        for (int t : THREADS) {
            ExecutorService pool = Executors.newFixedThreadPool(t);
            double ops = measureThroughput(pool, t, (tid, iter) -> {
                int keyIdx = (int) ((iter + tid * 12345L) % NUM_KEYS);
                tb.allow(new com.ratelimiter.core.RateLimitRequest(keys[keyIdx], 2000L + (iter & 1023), 1));
            });
            pool.shutdown();
            System.out.printf("Threads: %d | Throughput: %,12.0f ops/s\n", t, ops);
        }
    }

    private interface BenchmarkOp {
        void execute(int tid, long iter);
    }

    private static double measureThroughput(ExecutorService pool, int threads, BenchmarkOp op) throws Exception {
        // Warmup 500ms
        long wEnd = System.currentTimeMillis() + 500L;
        List<Future<?>> wFutures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            int tid = t;
            wFutures.add(pool.submit(() -> {
                long c = 0;
                while (System.currentTimeMillis() < wEnd) {
                    for (int b = 0; b < 1000; b++) {
                        op.execute(tid, c++);
                    }
                }
            }));
        }
        for (Future<?> f : wFutures) f.get();

        // 3 measurement intervals of 500ms
        int intervals = 3;
        double sum = 0;
        for (int i = 0; i < intervals; i++) {
            AtomicLong totalOps = new AtomicLong(0);
            long startMs = System.currentTimeMillis();
            long targetEnd = startMs + 500L;
            List<Future<?>> mFutures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                int tid = t;
                mFutures.add(pool.submit(() -> {
                    long c = 0;
                    while (System.currentTimeMillis() < targetEnd) {
                        for (int b = 0; b < 1000; b++) {
                            op.execute(tid, c++);
                        }
                    }
                    totalOps.addAndGet(c);
                }));
            }
            for (Future<?> f : mFutures) f.get();
            long dur = System.currentTimeMillis() - startMs;
            sum += (totalOps.get() * 1000.0) / dur;
        }
        return sum / intervals;
    }
}

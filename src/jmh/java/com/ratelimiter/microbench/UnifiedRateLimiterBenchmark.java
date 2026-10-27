package com.ratelimiter.microbench;

import com.ratelimiter.algorithms.*;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Unified JMH microbenchmark for rate limiting algorithms across 3 execution paths:
 * 1. Adapter: limiter.allow(new RateLimitRequest(key, ts, 1))
 * 2. Direct: limiter.allowDirect(key, ts, 1)
 * 3. Engine-Alone: limiter.allowEngineAlone(key, ts, 1)
 *
 * All paths share identical index counter advance, key selection, and advancing timestamps.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(5)
@State(Scope.Thread)
public class UnifiedRateLimiterBenchmark {

    @Param({
            "token_bucket",
            "leaky_bucket",
            "fixed_window",
            "sliding_window_log",
            "sliding_window_counter",
            "dreb_proportional",
            "dreb_burst_matched",
            "dreb_v2_base15",
            "static_dual_bucket"
    })
    public String algorithm;

    @Param({"same-key", "many-key"})
    public String scenario = "same-key";

    private RateLimiter limiter;
    private String[] keys;
    private int numKeys;
    private long counter;
    private long timestamp;

    @Setup(Level.Trial)
    public void setup() {
        RateLimiterConfig config = RateLimiterConfig.builder()
                .maxRequests(1000)
                .windowSizeMs(1000)
                .bucketCapacity(1000)
                .refillRate(1000.0)
                .build();

        limiter = createLimiter(algorithm, config);

        if ("same-key".equalsIgnoreCase(scenario)) {
            numKeys = 1;
            keys = new String[]{"bench-client-0"};
        } else {
            numKeys = 100_000;
            keys = new String[numKeys];
            for (int i = 0; i < numKeys; i++) {
                keys[i] = "bench-client-" + i;
            }
        }

        counter = 0L;
        timestamp = 1_000_000L;

        // Pre-warm state for all keys
        for (int i = 0; i < numKeys; i++) {
            limiter.allowEngineAlone(keys[i], 1000L, 1);
        }
    }

    public static RateLimiter createLimiter(String algo, RateLimiterConfig config) {
        return switch (algo) {
            case "token_bucket" -> new TokenBucket(config);
            case "leaky_bucket" -> new LeakyBucket(config);
            case "fixed_window" -> new FixedWindow(config);
            case "sliding_window_log" -> new SlidingWindowLog(config);
            case "sliding_window_counter" -> new SlidingWindowCounter(config);
            case "dreb_proportional" -> new CustomRateLimiter(config, CustomRateLimiter.PROPORTIONAL);
            case "dreb_burst_matched" -> new CustomRateLimiter(config, CustomRateLimiter.BURST_CEILING_MATCHED);
            case "dreb_v2_base15" -> new CustomRateLimiterV2(
                    DrebParameters.forV2Base15(config.getBucketCapacity(), config.getRefillRate()));
            case "static_dual_bucket" -> new CustomRateLimiter("DualBucket (B=3C/7)",
                    3.0 * config.getBucketCapacity() / 7.0,
                    5.0 * config.getRefillRate() / 3.0,
                    9.0 * config.getBucketCapacity() / 7.0,
                    config.getRefillRate(),
                    0.0, 0.0, 1.5);
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algo);
        };
    }

    @Benchmark
    public void benchmarkAdapter(Blackhole bh) {
        int idx = (int) ((counter++ & 0x7fffffff) % numKeys);
        long ts = timestamp++;
        String key = keys[idx];
        bh.consume(limiter.allow(new RateLimitRequest(key, ts, 1)));
    }

    @Benchmark
    public void benchmarkDirect(Blackhole bh) {
        int idx = (int) ((counter++ & 0x7fffffff) % numKeys);
        long ts = timestamp++;
        String key = keys[idx];
        bh.consume(limiter.allowDirect(key, ts, 1));
    }

    @Benchmark
    public void benchmarkEngineAlone(Blackhole bh) {
        int idx = (int) ((counter++ & 0x7fffffff) % numKeys);
        long ts = timestamp++;
        String key = keys[idx];
        bh.consume(limiter.allowEngineAlone(key, ts, 1));
    }
}

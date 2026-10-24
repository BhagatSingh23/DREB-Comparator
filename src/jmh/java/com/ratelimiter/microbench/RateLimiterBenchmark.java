package com.ratelimiter.microbench;

import com.ratelimiter.algorithms.FixedWindow;
import com.ratelimiter.algorithms.LeakyBucket;
import com.ratelimiter.algorithms.SlidingWindowCounter;
import com.ratelimiter.algorithms.SlidingWindowLog;
import com.ratelimiter.algorithms.TokenBucket;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Official JMH microbenchmark for per-algorithm allow() throughput,
 * latency, and allocation profiling with -prof gc and raw JSON export.
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(5)
@State(Scope.Benchmark)
public class RateLimiterBenchmark {

    @Param({
            "token_bucket",
            "leaky_bucket",
            "fixed_window",
            "sliding_window_log",
            "sliding_window_counter",
            "dreb_proportional",
            "dreb_burst_matched",
            "dreb_v2_base15"
    })
    private String algorithm;

    private RateLimiter limiter;

    @Setup(Level.Trial)
    public void setup() {
        RateLimiterConfig config = RateLimiterConfig.builder()
                .maxRequests(1000)
                .windowSizeMs(1000)
                .bucketCapacity(1000)
                .refillRate(1000.0)
                .build();

        limiter = createLimiter(algorithm, config);
        // Pre-warm limiter
        limiter.allow(new RateLimitRequest("bench-client", 1000L, 1));
    }

    public static RateLimiter createLimiter(String algo, RateLimiterConfig config) {
        return switch (algo) {
            case "token_bucket" -> new TokenBucket(config);
            case "leaky_bucket" -> new LeakyBucket(config);
            case "fixed_window" -> new FixedWindow(config);
            case "sliding_window_log" -> new SlidingWindowLog(config);
            case "sliding_window_counter" -> new SlidingWindowCounter(config);
            case "dreb_proportional" -> new com.ratelimiter.algorithms.CustomRateLimiter(config, com.ratelimiter.algorithms.CustomRateLimiter.PROPORTIONAL);
            case "dreb_burst_matched" -> new com.ratelimiter.algorithms.CustomRateLimiter(config, com.ratelimiter.algorithms.CustomRateLimiter.BURST_CEILING_MATCHED);
            case "dreb_v2_base15" -> new com.ratelimiter.algorithms.CustomRateLimiterV2(
                    com.ratelimiter.algorithms.DrebParameters.forV2Base15(config.getBucketCapacity(), config.getRefillRate()));
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algo);
        };
    }

    @State(Scope.Thread)
    public static class ThreadState {
        public long counter = 2000L;
        public final RateLimitRequest reusableRequest = new RateLimitRequest("bench-client", 3000L, 1);
    }

    /**
     * Adapter path: allocates RateLimitRequest per call.
     */
    @Benchmark
    public RateLimitResponse benchmarkAdapterAllow(ThreadState ts, Blackhole bh) {
        RateLimitRequest request = new RateLimitRequest("bench-client", ts.counter++);
        RateLimitResponse response = limiter.allow(request);
        bh.consume(response);
        return response;
    }

    /**
     * Algorithm direct path: reuses request object, isolating algorithm state allocation.
     */
    @Benchmark
    public RateLimitResponse benchmarkDirectAllow(ThreadState ts, Blackhole bh) {
        RateLimitResponse response = limiter.allow(ts.reusableRequest);
        bh.consume(response);
        return response;
    }
}

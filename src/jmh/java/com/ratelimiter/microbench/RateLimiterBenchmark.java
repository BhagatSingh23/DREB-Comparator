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
import java.util.concurrent.atomic.AtomicLong;

/**
 * JMH microbenchmark for low-level per-algorithm {@code allow()} throughput,
 * latency, and allocation profiling.
 *
 * <p>Run with: {@code java -jar target/jmh-benchmarks.jar -prof gc}
 *
 * <p>This is the low-level complement to the workload-simulator benchmarks.
 * Results are separate artifacts — see {@code ResultAggregator} for merging.
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class RateLimiterBenchmark {

    @Param({"token_bucket", "leaky_bucket", "fixed_window", "sliding_window_log", "sliding_window_counter", "custom"})
    private String algorithm;

    private RateLimiter limiter;
    private AtomicLong counter;

    @Setup(Level.Trial)
    public void setup() {
        RateLimiterConfig config = RateLimiterConfig.builder()
                .maxRequests(1000)
                .windowSizeMs(1000)
                .bucketCapacity(1000)
                .refillRate(1000.0)
                .build();

        limiter = createLimiter(algorithm, config);
        counter = new AtomicLong(System.currentTimeMillis());
    }

    private static RateLimiter createLimiter(String algo, RateLimiterConfig config) {
        return switch (algo) {
            case "token_bucket" -> new TokenBucket(config);
            case "leaky_bucket" -> new LeakyBucket(config);
            case "fixed_window" -> new FixedWindow(config);
            case "sliding_window_log" -> new SlidingWindowLog(config);
            case "sliding_window_counter" -> new SlidingWindowCounter(config);
            case "custom" -> new com.ratelimiter.algorithms.CustomRateLimiter(config);
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algo);
        };
    }

    /**
     * Benchmarks a single-client {@code allow()} call.
     */
    @Benchmark
    public RateLimitResponse benchmarkAllow(Blackhole bh) {
        RateLimitRequest request = new RateLimitRequest("bench-client", counter.incrementAndGet());
        RateLimitResponse response = limiter.allow(request);
        bh.consume(response);
        return response;
    }

    /**
     * Benchmarks {@code allow()} with 100 rotating client IDs to stress
     * per-client state management.
     */
    @Benchmark
    public RateLimitResponse benchmarkAllowMultiClient(Blackhole bh) {
        long ts = counter.incrementAndGet();
        String clientId = "bench-client-" + (ts % 100);
        RateLimitRequest request = new RateLimitRequest(clientId, ts);
        RateLimitResponse response = limiter.allow(request);
        bh.consume(response);
        return response;
    }
}

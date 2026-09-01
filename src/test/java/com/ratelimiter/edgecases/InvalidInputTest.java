package com.ratelimiter.edgecases;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiterConfig;
import com.ratelimiter.core.RateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class InvalidInputTest {

    static Stream<Supplier<RateLimiter>> rateLimiterProvider() {
        RateLimiterConfig config = RateLimiterConfig.builder()
                .maxRequests(100)
                .windowSizeMs(1000)
                .bucketCapacity(100)
                .refillRate(10)
                .build();
        return Stream.of(
                () -> new com.ratelimiter.algorithms.TokenBucket(config),
                () -> new com.ratelimiter.algorithms.LeakyBucket(config),
                () -> new com.ratelimiter.algorithms.FixedWindow(config),
                () -> new com.ratelimiter.algorithms.SlidingWindowLog(config),
                () -> new com.ratelimiter.algorithms.SlidingWindowCounter(config)
        );
    }

    @Test
    public void testNullClientId() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimitRequest(null, 100));
    }

    @Test
    public void testEmptyClientId() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimitRequest("", 100));
    }

    @Test
    public void testNegativeTimestamp() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimitRequest("c", -1));
    }

    @Test
    public void testZeroCost() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimitRequest("c", 100, 0));
    }

    @Test
    public void testNegativeCost() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimitRequest("c", 100, -5));
    }

    @ParameterizedTest
    @MethodSource("rateLimiterProvider")
    public void testNullRequest(Supplier<RateLimiter> supplier) {
        assertThrows(Exception.class, () -> supplier.get().allow(null));
    }

    @Test
    public void testZeroWindowConfig() {
        assertThrows(IllegalArgumentException.class, () -> RateLimiterConfig.builder().windowSizeMs(0).build());
    }

    @Test
    public void testNegativeWindowConfig() {
        assertThrows(IllegalArgumentException.class, () -> RateLimiterConfig.builder().windowSizeMs(-1).build());
    }

    @Test
    public void testZeroRateConfig() {
        assertThrows(IllegalArgumentException.class, () -> RateLimiterConfig.builder().maxRequests(0).build());
    }

    @Test
    public void testNegativeRateConfig() {
        assertThrows(IllegalArgumentException.class, () -> RateLimiterConfig.builder().maxRequests(-1).build());
    }

    @Test
    public void testZeroCapacityConfig() {
        assertThrows(IllegalArgumentException.class, () -> RateLimiterConfig.builder().bucketCapacity(0).build());
    }

    @Test
    public void testZeroRefillRate() {
        assertThrows(IllegalArgumentException.class, () -> RateLimiterConfig.builder().refillRate(0).build());
    }

    @Test
    public void testNegativeRefillRate() {
        assertThrows(IllegalArgumentException.class, () -> RateLimiterConfig.builder().refillRate(-1).build());
    }

    @Test
    public void testExtremelyLargeValues() {
        RateLimiterConfig.builder().maxRequests(Long.MAX_VALUE / 2).build();
    }
}

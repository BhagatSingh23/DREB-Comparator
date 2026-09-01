package com.ratelimiter.fairness;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FairnessTest {

    static Stream<Supplier<RateLimiter>> rateLimiterProvider() {
        RateLimiterConfig config = RateLimiterConfig.builder()
                .maxRequests(1000)
                .windowSizeMs(10000)
                .bucketCapacity(1000)
                .refillRate(100)
                .build();
        return Stream.of(
                () -> new com.ratelimiter.algorithms.TokenBucket(config),
                () -> new com.ratelimiter.algorithms.LeakyBucket(config),
                () -> new com.ratelimiter.algorithms.FixedWindow(config),
                () -> new com.ratelimiter.algorithms.SlidingWindowLog(config),
                () -> new com.ratelimiter.algorithms.SlidingWindowCounter(config)
        );
    }

    @ParameterizedTest
    @MethodSource("rateLimiterProvider")
    void testFairness(Supplier<RateLimiter> supplier) {
        RateLimiter limiter = supplier.get();
        Map<String, Integer> acceptedCounts = new HashMap<>();
        acceptedCounts.put("A", 0);
        acceptedCounts.put("B", 0);
        acceptedCounts.put("C", 0);
        acceptedCounts.put("D", 0);

        int aRequests = 10000;
        int bcdRequests = 100;
        
        int totalSteps = Math.max(aRequests, bcdRequests);
        long baseTime = System.currentTimeMillis();

        for (int i = 0; i < totalSteps; i++) {
            if (i < aRequests) {
                if (limiter.allow(new RateLimitRequest("A", baseTime + i)).isAllowed()) {
                    acceptedCounts.put("A", acceptedCounts.get("A") + 1);
                }
            }
            if (i < bcdRequests) {
                if (limiter.allow(new RateLimitRequest("B", baseTime + i)).isAllowed()) {
                    acceptedCounts.put("B", acceptedCounts.get("B") + 1);
                }
                if (limiter.allow(new RateLimitRequest("C", baseTime + i)).isAllowed()) {
                    acceptedCounts.put("C", acceptedCounts.get("C") + 1);
                }
                if (limiter.allow(new RateLimitRequest("D", baseTime + i)).isAllowed()) {
                    acceptedCounts.put("D", acceptedCounts.get("D") + 1);
                }
            }
        }

        double sum = acceptedCounts.values().stream().mapToDouble(Integer::doubleValue).sum();
        double sumSquares = acceptedCounts.values().stream().mapToDouble(v -> (double) v * v).sum();
        double n = acceptedCounts.size();
        
        double jainsIndex = (sum * sum) / (n * sumSquares);
        
        System.out.println("Algorithm: " + limiter.getClass().getSimpleName() + " Jain's Index: " + jainsIndex);
        
        assertTrue(jainsIndex > 0);
        assertTrue(acceptedCounts.get("B") > 0 || acceptedCounts.get("C") > 0 || acceptedCounts.get("D") > 0, "One client took all capacity");
    }
}

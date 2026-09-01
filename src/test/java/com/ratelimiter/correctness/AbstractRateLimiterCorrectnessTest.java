package com.ratelimiter.correctness;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractRateLimiterCorrectnessTest {

    protected abstract RateLimiter createLimiter(RateLimiterConfig config);
    protected abstract String algorithmName();

    protected RateLimiterConfig defaultConfig() {
        return RateLimiterConfig.builder()
                .maxRequests(10)
                .windowSizeMs(1000)
                .bucketCapacity(10)
                .refillRate(10.0)
                .build();
    }

    @Test
    @DisplayName("Test zero requests")
    void testZeroRequests() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertNotNull(limiter);
        assertEquals(algorithmName(), limiter.name());
    }

    @Test
    @DisplayName("Test single request")
    void testSingleRequest() {
        RateLimiter limiter = createLimiter(defaultConfig());
        RateLimitResponse response = limiter.allow(new RateLimitRequest("client1", 1000));
        assertTrue(response.isAllowed());
    }

    @Test
    @DisplayName("Test exactly at limit")
    void testExactlyAtLimit() {
        RateLimiter limiter = createLimiter(defaultConfig());
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allow(new RateLimitRequest("client1", 1000 + i)).isAllowed());
        }
    }

    @Test
    @DisplayName("Test one over limit")
    void testOneOverLimit() {
        RateLimiter limiter = createLimiter(defaultConfig());
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allow(new RateLimitRequest("client1", 1000 + i)).isAllowed());
        }
        assertFalse(limiter.allow(new RateLimitRequest("client1", 1010)).isAllowed());
    }

    @Test
    @DisplayName("Test long running traffic")
    void testLongRunningTraffic() {
        RateLimiter limiter = createLimiter(defaultConfig());
        for (int j = 0; j < 5; j++) {
            for (int i = 0; i < 10; i++) {
                assertTrue(limiter.allow(new RateLimitRequest("client1", 1000 + j * 1000 + i)).isAllowed());
            }
            assertFalse(limiter.allow(new RateLimitRequest("client1", 1000 + j * 1000 + 10)).isAllowed());
        }
    }

    @Test
    @DisplayName("Test multiple clients")
    void testMultipleClients() {
        RateLimiter limiter = createLimiter(defaultConfig());
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allow(new RateLimitRequest("client1", 1000 + i)).isAllowed());
            assertTrue(limiter.allow(new RateLimitRequest("client2", 1000 + i)).isAllowed());
            assertTrue(limiter.allow(new RateLimitRequest("client3", 1000 + i)).isAllowed());
        }
        assertFalse(limiter.allow(new RateLimitRequest("client1", 1010)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("client2", 1010)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("client3", 1010)).isAllowed());
    }

    @Test
    @DisplayName("Test cost basic")
    void testCostBasic() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("client1", 1000, 5)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("client1", 1001, 6)).isAllowed());
    }

    @Test
    @DisplayName("Test cost exact limit")
    void testCostExactLimit() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("client1", 1000, 10)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("client1", 1001, 1)).isAllowed());
    }

    @Test
    @DisplayName("Test cost exceeds limit")
    void testCostExceedsLimit() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertFalse(limiter.allow(new RateLimitRequest("client1", 1000, 15)).isAllowed());
    }

    @Test
    @DisplayName("Test burst at time zero")
    void testBurstAtTimeZero() {
        RateLimiter limiter = createLimiter(defaultConfig());
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allow(new RateLimitRequest("client1", 0)).isAllowed());
        }
        assertFalse(limiter.allow(new RateLimitRequest("client1", 0)).isAllowed());
    }

    @Test
    @DisplayName("Test max allowed burst")
    void testMaxAllowedBurst() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("client1", 1000, 10)).isAllowed());
    }

    @Test
    @DisplayName("Test burst larger than capacity")
    void testBurstLargerThanCapacity() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertFalse(limiter.allow(new RateLimitRequest("client1", 1000, 11)).isAllowed());
    }

    @Test
    @DisplayName("Test repeated bursts")
    void testRepeatedBursts() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("client1", 1000, 10)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("client1", 1001, 10)).isAllowed());
        assertTrue(limiter.allow(new RateLimitRequest("client1", 2000, 10)).isAllowed());
    }

    @Test
    @DisplayName("Test idle period")
    void testIdlePeriod() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("client1", 1000, 10)).isAllowed());
        // long gap
        assertTrue(limiter.allow(new RateLimitRequest("client1", 10000, 10)).isAllowed());
    }

    @Test
    @DisplayName("Test identical timestamps")
    void testIdenticalTimestamps() {
        RateLimiter limiter = createLimiter(defaultConfig());
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allow(new RateLimitRequest("client1", 1000)).isAllowed());
        }
        assertFalse(limiter.allow(new RateLimitRequest("client1", 1000)).isAllowed());
    }

    @Test
    @DisplayName("Test very small intervals")
    void testVerySmallIntervals() {
        RateLimiter limiter = createLimiter(defaultConfig());
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allow(new RateLimitRequest("client1", 1000 + i)).isAllowed());
        }
        assertFalse(limiter.allow(new RateLimitRequest("client1", 1010)).isAllowed());
    }

    @Test
    @DisplayName("Test response fields")
    void testResponseFields() {
        RateLimiter limiter = createLimiter(defaultConfig());
        RateLimitResponse r1 = limiter.allow(new RateLimitRequest("client1", 1000, 5));
        assertTrue(r1.isAllowed());
        if (r1.getRemainingCapacity() != -1) {
            assertTrue(r1.getRemainingCapacity() >= 0);
        }

        RateLimitResponse r2 = limiter.allow(new RateLimitRequest("client1", 1001, 6));
        assertFalse(r2.isAllowed());
        if (r2.getRetryAfterMs() != -1) {
            assertTrue(r2.getRetryAfterMs() > 0);
        }
        assertNotNull(r2.getReason());
    }
}

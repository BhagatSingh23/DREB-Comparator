package com.ratelimiter.correctness;

import com.ratelimiter.algorithms.LeakyBucket;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LeakyBucketCorrectnessTest extends AbstractRateLimiterCorrectnessTest {

    @Override
    protected RateLimiter createLimiter(RateLimiterConfig config) {
        return new LeakyBucket(config);
    }

    @Override
    protected String algorithmName() {
        return "LeakyBucket";
    }

    @Test
    @DisplayName("Test empty allowance")
    void testEmptyAllowance() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 10)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("c1", 1001, 1)).isAllowed());
    }

    @Test
    @DisplayName("Test full allowance")
    void testFullAllowance() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 10)).isAllowed());
    }

    @Test
    @DisplayName("Test continuous traffic")
    void testContinuousTraffic() {
        RateLimiter limiter = createLimiter(defaultConfig());
        // maxRequests = 10, windowSizeMs = 1000 -> 1 request per 100ms
        for (int i = 0; i < 20; i++) {
            assertTrue(limiter.allow(new RateLimitRequest("c1", 1000 + i * 100, 1)).isAllowed());
        }
    }

    @Test
    @DisplayName("Test burst traffic")
    void testBurstTraffic() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertFalse(limiter.allow(new RateLimitRequest("c1", 1000, 11)).isAllowed());
    }

    @Test
    @DisplayName("Test varying arrival rates")
    void testVaryingArrivalRates() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 5)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("c1", 1050, 6)).isAllowed()); // not enough leaked yet
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1500, 5)).isAllowed());
    }
}

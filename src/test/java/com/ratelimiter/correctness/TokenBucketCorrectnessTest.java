package com.ratelimiter.correctness;

import com.ratelimiter.algorithms.TokenBucket;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TokenBucketCorrectnessTest extends AbstractRateLimiterCorrectnessTest {

    @Override
    protected RateLimiter createLimiter(RateLimiterConfig config) {
        return new TokenBucket(config);
    }

    @Override
    protected String algorithmName() {
        return "TokenBucket";
    }

    @Test
    @DisplayName("Test empty bucket")
    void testEmptyBucket() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 10)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("c1", 1001, 1)).isAllowed());
    }

    @Test
    @DisplayName("Test full bucket")
    void testFullBucket() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 10)).isAllowed());
    }

    @Test
    @DisplayName("Test partial bucket")
    void testPartialBucket() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 5)).isAllowed());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 5)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("c1", 1000, 1)).isAllowed());
    }

    @Test
    @DisplayName("Test exact refill")
    void testExactRefill() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 10)).isAllowed());
        // 10 tokens per second -> 1 token per 100ms
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1100, 1)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("c1", 1101, 1)).isAllowed());
    }

    @Test
    @DisplayName("Test fractional refill timing")
    void testFractionalRefillTiming() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 10)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("c1", 1050, 1)).isAllowed()); // 0.5 tokens
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1100, 1)).isAllowed());
    }

    @Test
    @DisplayName("Test huge burst")
    void testHugeBurst() {
        RateLimiterConfig config = RateLimiterConfig.builder().bucketCapacity(100).refillRate(10.0).maxRequests(100).windowSizeMs(1000).build();
        RateLimiter limiter = createLimiter(config);
        assertTrue(limiter.allow(new RateLimitRequest("c1", 0, 100)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("c1", 0, 1)).isAllowed());
    }

    @Test
    @DisplayName("Test faster than refill")
    void testFasterThanRefill() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 10)).isAllowed());
        for (int i = 0; i < 10; i++) {
            assertFalse(limiter.allow(new RateLimitRequest("c1", 1000 + i * 10, 1)).isAllowed());
        }
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1100, 1)).isAllowed());
    }

    @Test
    @DisplayName("Test slower than refill")
    void testSlowerThanRefill() {
        RateLimiter limiter = createLimiter(defaultConfig());
        for (int i = 0; i < 20; i++) {
            assertTrue(limiter.allow(new RateLimitRequest("c1", 1000 + i * 200, 1)).isAllowed());
        }
    }

    @Test
    @DisplayName("Test burst after refill")
    void testBurstAfterRefill() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 10)).isAllowed());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 2000, 10)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("c1", 2000, 1)).isAllowed());
    }
}

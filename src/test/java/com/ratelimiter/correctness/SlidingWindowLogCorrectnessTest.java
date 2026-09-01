package com.ratelimiter.correctness;

import com.ratelimiter.algorithms.SlidingWindowLog;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SlidingWindowLogCorrectnessTest extends AbstractRateLimiterCorrectnessTest {

    @Override
    protected RateLimiter createLimiter(RateLimiterConfig config) {
        return new SlidingWindowLog(config);
    }

    @Override
    protected String algorithmName() {
        return "SlidingWindowLog";
    }

    @Test
    @DisplayName("Test empty history")
    void testEmptyHistory() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000)).isAllowed());
    }

    @Test
    @DisplayName("Test timestamp entering window")
    void testTimestampEnteringWindow() {
        RateLimiter limiter = createLimiter(defaultConfig());
        for(int i=0; i<10; i++){
            assertTrue(limiter.allow(new RateLimitRequest("c1", 1000 + i)).isAllowed());
        }
        assertFalse(limiter.allow(new RateLimitRequest("c1", 1010)).isAllowed());
    }

    @Test
    @DisplayName("Test timestamp leaving window")
    void testTimestampLeavingWindow() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 10)).isAllowed());
        // 1000 is evicted at 2000
        assertTrue(limiter.allow(new RateLimitRequest("c1", 2000, 1)).isAllowed());
    }

    @Test
    @DisplayName("Test boundary behavior epsilon")
    void testBoundaryBehaviorEpsilon() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 10)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("c1", 1999, 1)).isAllowed());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 2000, 1)).isAllowed());
    }

    @Test
    @DisplayName("Test high volume")
    void testHighVolume() {
        RateLimiter limiter = createLimiter(defaultConfig());
        int allowed = 0;
        for (int i = 0; i < 10000; i++) {
            if (limiter.allow(new RateLimitRequest("c1", 1000 + i)).isAllowed()) {
                allowed++;
            }
        }
        assertEquals(100, allowed);
    }

    @Test
    @DisplayName("Test large window")
    void testLargeWindow() {
        RateLimiterConfig config = RateLimiterConfig.builder().maxRequests(10).windowSizeMs(60000).build();
        RateLimiter limiter = createLimiter(config);
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 10)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("c1", 30000, 1)).isAllowed());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 61000, 10)).isAllowed());
    }
}

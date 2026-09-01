package com.ratelimiter.correctness;

import com.ratelimiter.algorithms.FixedWindow;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FixedWindowCorrectnessTest extends AbstractRateLimiterCorrectnessTest {

    @Override
    protected RateLimiter createLimiter(RateLimiterConfig config) {
        return new FixedWindow(config);
    }

    @Override
    protected String algorithmName() {
        return "FixedWindow";
    }

    @Test
    @DisplayName("Test window boundary")
    void testWindowBoundary() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 10)).isAllowed());
    }

    @Test
    @DisplayName("Test window end")
    void testWindowEnd() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1999, 10)).isAllowed());
        assertFalse(limiter.allow(new RateLimitRequest("c1", 1999, 1)).isAllowed());
    }

    @Test
    @DisplayName("Test window end plus 1")
    void testWindowEndPlus1() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1000, 10)).isAllowed());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 2000, 10)).isAllowed());
    }

    @Test
    @DisplayName("Test before and after reset")
    void testBeforeAndAfterReset() {
        RateLimiter limiter = createLimiter(defaultConfig());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 1999, 10)).isAllowed());
        assertTrue(limiter.allow(new RateLimitRequest("c1", 2000, 10)).isAllowed());
    }

    @Test
    @DisplayName("Test multiple windows sequential")
    void testMultipleWindowsSequential() {
        RateLimiter limiter = createLimiter(defaultConfig());
        for (int i = 1; i <= 5; i++) {
            assertTrue(limiter.allow(new RateLimitRequest("c1", i * 1000, 10)).isAllowed());
            assertFalse(limiter.allow(new RateLimitRequest("c1", i * 1000 + 500, 1)).isAllowed());
        }
    }
}

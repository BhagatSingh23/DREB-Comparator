package com.ratelimiter.algorithms;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomRateLimiterSmokeTest {

    @Test
    @DisplayName("Cold-start Idle-then-Burst (paper defaults)")
    void coldStartIdleThenBurst() {
        // Use the paper's raw defaults via the 7-arg constructor
        CustomRateLimiter limiter = new CustomRateLimiter(20, 5, 60, 3, 2.0, 40, 1.5);
        String client = "cold-start-client";

        int accepted = runBurstAt(limiter, client, 60_000L);
        System.out.println("Cold-start Idle-then-Burst: " + accepted + "/30 accepted");
        assertTrue(accepted >= 26 && accepted <= 28, "Unexpected cold-start acceptance: " + accepted + "/30");
    }

    @Test
    @DisplayName("Pre-warmed Idle-then-Burst (paper defaults)")
    void preWarmedIdleThenBurst() {
        CustomRateLimiter limiter = new CustomRateLimiter(20, 5, 60, 3, 2.0, 40, 1.5);
        String client = "pre-warmed-client";

        limiter.allow(new RateLimitRequest(client, 0L, 1)); // touch at t=0

        int accepted = runBurstAt(limiter, client, 60_000L);
        System.out.println("Pre-warmed Idle-then-Burst: " + accepted + "/30 accepted");
        assertTrue(accepted == 30 || accepted == 29, "Unexpected pre-warmed acceptance: " + accepted + "/30");
    }

    @Test
    @DisplayName("Config-mapped DREB accepts burst proportional to capacity")
    void configMappedBurst() {
        // With bucketCapacity=100, refillRate=100 (same as smoke.yaml),
        // DREB gets sustainedMax=100, burstMax≈33. A burst of 33 at t=0
        // should be fully accepted (burst bucket starts full, sustained starts at 100).
        RateLimiterConfig config = RateLimiterConfig.builder()
                .maxRequests(100).windowSizeMs(1000).bucketCapacity(100).refillRate(100.0).build();
        CustomRateLimiter limiter = new CustomRateLimiter(config);

        int accepted = 0;
        for (int i = 0; i < 33; i++) {
            if (limiter.allow(new RateLimitRequest("c1", 0L, 1)).isAllowed()) accepted++;
        }
        System.out.println("Config-mapped burst: " + accepted + "/33 accepted");
        assertTrue(accepted == 33, "Config-mapped DREB should accept 33 burst requests, got: " + accepted);
    }

    private static int runBurstAt(CustomRateLimiter limiter, String client, long burstStartMs) {
        int accepted = 0;
        for (int i = 0; i < 30; i++) {
            long ts = burstStartMs + (long) (i * 50);
            RateLimitResponse r = limiter.allow(new RateLimitRequest(client, ts, 1));
            if (r.isAllowed()) accepted++;
        }
        return accepted;
    }
}

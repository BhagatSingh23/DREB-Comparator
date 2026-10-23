package com.ratelimiter.algorithms;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiterConfig;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V2BothOffSanityTest {

    @Test
    void testV2BothOffMatchesV1BurstMatchedIdentically() {
        double capacity = 1000.0;
        double refillRate = 1000.0;

        RateLimiterConfig stdConfig = RateLimiterConfig.builder()
                .bucketCapacity((long) capacity)
                .refillRate(refillRate)
                .maxRequests((long) capacity)
                .windowSizeMs(1000L)
                .build();

        CustomRateLimiter v1 = new CustomRateLimiter(stdConfig, CustomRateLimiter.BURST_CEILING_MATCHED);

        DrebParameters v2BothOffParams = DrebParameters.customV2(
                capacity, refillRate, 1.5,
                0.0, 2.0, 1.5,
                0.0, false, false);
        CustomRateLimiterV2 v2BothOff = new CustomRateLimiterV2(v2BothOffParams);

        // Test over 5,000 requests across multiple clients with bursts, idle periods, and steady traffic
        Random rng = new Random(42);
        long tMs = 0L;

        for (int i = 0; i < 5000; i++) {
            tMs += rng.nextInt(50); // up to 50ms forward
            String cid = "client_" + rng.nextInt(10);
            int cost = rng.nextInt(100) == 0 ? rng.nextInt(500) + 1 : 1;

            RateLimitRequest req = new RateLimitRequest(cid, tMs, cost);
            RateLimitResponse res1 = v1.allow(req);
            RateLimitResponse res2 = v2BothOff.allow(req);

            assertEquals(res1.isAllowed(), res2.isAllowed(), "Decision mismatch at step " + i + " client=" + cid);
            assertEquals(res1.getRemainingCapacity(), res2.getRemainingCapacity(), "Remaining mismatch at step " + i);
        }
    }
}

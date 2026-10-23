package com.ratelimiter.algorithms;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.dreb.ratelimit.ClientState;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test verifying that DualBucket with B = 3C/7 equals DREB burst-matched
 * in cold start with idleGain = 0 (sanity check).
 */
class DualBucketSanityTest {

    @Test
    void testDualBucketEqualsDrebBurstMatchedWithIdleGainZero() {
        double c = 1000.0;
        double r = 1000.0;
        double b = 3.0 * c / 7.0;
        double sMax = 9.0 * c / 7.0;
        double bRate = 5.0 * r / 3.0;
        double sRate = r;

        // 1. DualBucket baseline with B = 3C/7, idleGain = 0, idleCap = 0
        CustomRateLimiter dualBucket = new CustomRateLimiter(
                "DualBucket (B=0.43C)",
                b, bRate, sMax, sRate, 0.0, 0.0, 1.5
        );

        // 2. DREB burst-matched with idleGain = 0 (and idleCap = 6C/7 or 0)
        CustomRateLimiter drebZeroIdle = new CustomRateLimiter(
                "DREB-BM-ZeroIdle",
                b, bRate, sMax, sRate, 0.0, 6.0 * c / 7.0, 1.5
        );

        // Cold start initialization
        dualBucket.initializeState("c1", "coldStart", 0.0);
        drebZeroIdle.initializeState("c1", "coldStart", 0.0);

        ClientState dbState = dualBucket.peekState("c1");
        ClientState drebState = drebZeroIdle.peekState("c1");

        assertEquals(b, dbState.burstTokens, 1e-9);
        assertEquals(sMax, dbState.sustainedTokens, 1e-9);
        assertEquals(0.0, dbState.idleCredit, 1e-9);

        assertEquals(dbState.burstTokens, drebState.burstTokens, 1e-9);
        assertEquals(dbState.sustainedTokens, drebState.sustainedTokens, 1e-9);
        assertEquals(dbState.idleCredit, drebState.idleCredit, 1e-9);

        // Run 10,000 requests with pseudo-random inter-arrival times (including long idle periods) and costs
        Random rng = new Random(42);
        long tMs = 0L;

        for (int i = 0; i < 10000; i++) {
            // Jump anywhere from 0 to 5000 ms forward (testing idle intervals > tau)
            tMs += rng.nextInt(500);
            if (rng.nextDouble() < 0.05) {
                tMs += 2000; // 2 seconds idle (exceeds tau = 1.286s)
            }

            int cost = rng.nextDouble() < 0.2 ? rng.nextInt(600) + 1 : 1;
            RateLimitRequest req = new RateLimitRequest("c1", tMs, cost);

            RateLimitResponse res1 = dualBucket.allow(req);
            RateLimitResponse res2 = drebZeroIdle.allow(req);

            assertEquals(res1.isAllowed(), res2.isAllowed(), "Decision mismatch at step " + i + " cost=" + cost + " tMs=" + tMs);
            assertEquals(res1.getRemainingCapacity(), res2.getRemainingCapacity(), "Remaining capacity mismatch at step " + i);

            // Assert idle credit never accumulates in either limiter
            assertEquals(0.0, dualBucket.peekState("c1").idleCredit, 1e-9);
            assertEquals(0.0, drebZeroIdle.peekState("c1").idleCredit, 1e-9);
        }
    }

    @Test
    void testInitialStateColdStartAndPreWarmedForDualBucket() {
        double c = 50000.0;
        double r = 50000.0;
        double b = 0.30 * c;
        double sMax = 9.0 * c / 7.0;

        CustomRateLimiter db = new CustomRateLimiter(
                "DualBucket (B=0.30C)",
                b, 5.0 * r / 3.0, sMax, r, 0.0, 0.0, 1.5
        );

        db.initializeState("client_cold", "coldStart", 0.0);
        ClientState sCold = db.peekState("client_cold");
        assertEquals(b, sCold.burstTokens, 1e-9);
        assertEquals(sMax, sCold.sustainedTokens, 1e-9);
        assertEquals(0.0, sCold.idleCredit, 1e-9);

        db.initializeState("client_pre", "preWarmed", 0.0);
        ClientState sPre = db.peekState("client_pre");
        assertEquals(b, sPre.burstTokens, 1e-9);
        assertEquals(sMax, sPre.sustainedTokens, 1e-9);
        assertEquals(0.0, sPre.idleCredit, 1e-9); // DualBucket idleCap is 0, so idleCredit is 0 even when preWarmed
    }
}

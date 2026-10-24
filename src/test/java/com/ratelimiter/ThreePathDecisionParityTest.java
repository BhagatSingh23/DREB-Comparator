package com.ratelimiter;

import com.ratelimiter.algorithms.*;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test asserting that for any key and any sequence of calls,
 * adapter, direct, and engine-alone produce 100% identical decisions.
 */
public class ThreePathDecisionParityTest {

    private static RateLimiter createLimiter(String algo) {
        RateLimiterConfig config = RateLimiterConfig.builder()
                .maxRequests(1000)
                .windowSizeMs(1000)
                .bucketCapacity(1000)
                .refillRate(1000.0)
                .build();

        return switch (algo) {
            case "token_bucket" -> new TokenBucket(config);
            case "leaky_bucket" -> new LeakyBucket(config);
            case "fixed_window" -> new FixedWindow(config);
            case "sliding_window_log" -> new SlidingWindowLog(config);
            case "sliding_window_counter" -> new SlidingWindowCounter(config);
            case "dreb_proportional" -> new CustomRateLimiter(config, CustomRateLimiter.PROPORTIONAL);
            case "dreb_burst_matched" -> new CustomRateLimiter(config, CustomRateLimiter.BURST_CEILING_MATCHED);
            case "dreb_v2_base15" -> new CustomRateLimiterV2(
                    DrebParameters.forV2Base15(config.getBucketCapacity(), config.getRefillRate()));
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algo);
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "token_bucket",
            "leaky_bucket",
            "fixed_window",
            "sliding_window_log",
            "sliding_window_counter",
            "dreb_proportional",
            "dreb_burst_matched",
            "dreb_v2_base15"
    })
    @DisplayName("Verify identical decisions across adapter, direct, and engine-alone paths")
    void testThreePathDecisionParity(String algorithm) {
        RateLimiter adapterLimiter = createLimiter(algorithm);
        RateLimiter directLimiter = createLimiter(algorithm);
        RateLimiter engineLimiter = createLimiter(algorithm);

        String[] keys = {"client_alpha", "client_beta", "client_gamma", "client_delta", "single_key"};
        Random rng = new Random(424242L);

        long timestampMs = 1000L;
        int totalRequests = 10000;

        for (int i = 0; i < totalRequests; i++) {
            // Pick key
            String key = keys[rng.nextInt(keys.length)];
            // Advancing timestamps with occasional burst / idle
            timestampMs += rng.nextInt(5); // 0 to 4 ms advance
            int cost = 1 + rng.nextInt(50); // cost 1 to 50

            // 1. Adapter call
            RateLimitRequest req = new RateLimitRequest(key, timestampMs, cost);
            RateLimitResponse adapterResp = adapterLimiter.allow(req);

            // 2. Direct call
            RateLimitResponse directResp = directLimiter.allowDirect(key, timestampMs, cost);

            // 3. Engine-alone call
            boolean engineAllowed = engineLimiter.allowEngineAlone(key, timestampMs, cost);

            // Assert exact decision match across all three paths
            assertEquals(adapterResp.isAllowed(), directResp.isAllowed(),
                    String.format("[%s] Adapter vs Direct decision mismatch at op %d (key=%s, ts=%d, cost=%d)",
                            algorithm, i, key, timestampMs, cost));
            assertEquals(adapterResp.isAllowed(), engineAllowed,
                    String.format("[%s] Adapter vs Engine decision mismatch at op %d (key=%s, ts=%d, cost=%d)",
                            algorithm, i, key, timestampMs, cost));

            // Metadata parity between adapter and direct
            assertEquals(adapterResp.getRemainingCapacity(), directResp.getRemainingCapacity(),
                    String.format("[%s] Remaining capacity mismatch at op %d", algorithm, i));
            assertEquals(adapterResp.getRetryAfterMs(), directResp.getRetryAfterMs(),
                    String.format("[%s] Retry-after mismatch at op %d", algorithm, i));
            assertEquals(adapterResp.getReason(), directResp.getReason(),
                    String.format("[%s] Reason mismatch at op %d", algorithm, i));
        }
    }
}

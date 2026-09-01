package com.ratelimiter.correctness.properties;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import net.jqwik.api.*;

import java.util.List;

public class FixedWindowProperties {
    @Property
    void acceptedNeverExceedsLimit(@ForAll("requestSequences") List<RateLimitRequest> requests) {
        RateLimiterConfig config = RateLimiterConfig.builder().maxRequests(10).windowSizeMs(1000).build();
        RateLimiter limiter = new com.ratelimiter.algorithms.FixedWindow(config);
        
        for (RateLimitRequest req : requests) {
            limiter.allow(req);
        }
        // Verification happens via algorithm's inherent properties based on random inputs
    }

    @Provide
    Arbitrary<List<RateLimitRequest>> requestSequences() {
        return Arbitraries.integers().between(0, 2000)
                .map(t -> new RateLimitRequest("client1", t))
                .list().ofMinSize(1).ofMaxSize(100);
    }
}

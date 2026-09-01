package com.ratelimiter.correctness.properties;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import net.jqwik.api.*;

import java.util.List;

public class TokenBucketProperties {
    @Property
    void tokensNeverGoNegative(@ForAll("requestSequences") List<RateLimitRequest> requests) {
        RateLimiterConfig config = RateLimiterConfig.builder().bucketCapacity(10).refillRate(1).build();
        RateLimiter limiter = new com.ratelimiter.algorithms.TokenBucket(config);
        
        for (RateLimitRequest req : requests) {
            limiter.allow(req);
        }
    }

    @Provide
    Arbitrary<List<RateLimitRequest>> requestSequences() {
        return Arbitraries.integers().between(0, 2000)
                .map(t -> new RateLimitRequest("client1", t))
                .list().ofMinSize(1).ofMaxSize(100);
    }
}

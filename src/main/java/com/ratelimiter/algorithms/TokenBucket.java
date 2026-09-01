package com.ratelimiter.algorithms;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Token Bucket algorithm implementation.
 */
public class TokenBucket implements RateLimiter {
    private final long bucketCapacity;
    private final double refillRate;
    private final ConcurrentMap<String, ClientState> states = new ConcurrentHashMap<>();

    public TokenBucket(RateLimiterConfig config) {
        if (config.getBucketCapacity() < 1) {
            throw new IllegalArgumentException("bucketCapacity must be >= 1");
        }
        if (config.getRefillRate() <= 0) {
            throw new IllegalArgumentException("refillRate must be > 0");
        }
        this.bucketCapacity = config.getBucketCapacity();
        this.refillRate = config.getRefillRate();
    }

    @Override
    public RateLimitResponse allow(RateLimitRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        ClientState state = states.computeIfAbsent(request.getClientId(), k -> new ClientState(bucketCapacity, request.getTimestampMs()));
        
        synchronized (state) {
            long now = request.getTimestampMs();
            // Refill tokens
            long elapsed = now - state.lastRefillTimestamp;
            if (elapsed > 0) {
                double tokensToAdd = elapsed * refillRate / 1000.0;
                state.tokens = Math.min(bucketCapacity, state.tokens + tokensToAdd);
                state.lastRefillTimestamp = now;
            }
            
            if (state.tokens + 1e-9 >= request.getCost()) {
                state.tokens -= request.getCost();
                return RateLimitResponse.allowed((long) state.tokens);
            } else {
                long retryAfterMs = (long) Math.ceil((request.getCost() - state.tokens) * 1000.0 / refillRate);
                return RateLimitResponse.rejected(retryAfterMs, "Rate limit exceeded (Token Bucket)");
            }
        }
    }

    @Override
    public String name() {
        return "TokenBucket";
    }

    @Override
    public void reset() {
        states.clear();
    }
    
    private static class ClientState {
        double tokens;
        long lastRefillTimestamp;
        
        ClientState(long initialTokens, long timestamp) {
            this.tokens = initialTokens;
            this.lastRefillTimestamp = timestamp;
        }
    }
}

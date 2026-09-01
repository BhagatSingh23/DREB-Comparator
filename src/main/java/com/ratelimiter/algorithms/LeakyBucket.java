package com.ratelimiter.algorithms;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Leaky Bucket (meter-based / GCRA-style) algorithm implementation.
 */
public class LeakyBucket implements RateLimiter {
    private final long bucketCapacity;
    private final double refillRate;
    private final ConcurrentMap<String, ClientState> states = new ConcurrentHashMap<>();

    public LeakyBucket(RateLimiterConfig config) {
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
            long elapsed = now - state.lastCheckTimestamp;
            
            if (elapsed > 0) {
                double leaked = elapsed * refillRate / 1000.0;
                state.allowance = Math.min(bucketCapacity, state.allowance + leaked);
                state.lastCheckTimestamp = now;
            }
            
            if (state.allowance >= request.getCost()) {
                state.allowance -= request.getCost();
                return RateLimitResponse.allowed((long) state.allowance);
            } else {
                long retryAfterMs = (long) Math.ceil((request.getCost() - state.allowance) * 1000.0 / refillRate);
                return RateLimitResponse.rejected(retryAfterMs, "Rate limit exceeded (Leaky Bucket)");
            }
        }
    }

    @Override
    public String name() {
        return "LeakyBucket";
    }

    @Override
    public void reset() {
        states.clear();
    }
    
    private static class ClientState {
        double allowance;
        long lastCheckTimestamp;
        
        ClientState(long initialAllowance, long timestamp) {
            this.allowance = initialAllowance;
            this.lastCheckTimestamp = timestamp;
        }
    }
}

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
        return allowDirect(request.getClientId(), request.getTimestampMs(), (int) request.getCost());
    }

    @Override
    public RateLimitResponse allowDirect(String clientId, long timestampMs, int cost) {
        ClientState state = states.computeIfAbsent(clientId, k -> new ClientState(bucketCapacity, timestampMs));
        
        synchronized (state) {
            // Refill tokens
            long elapsed = timestampMs - state.lastRefillTimestamp;
            if (elapsed > 0) {
                double tokensToAdd = elapsed * refillRate / 1000.0;
                state.tokens = Math.min(bucketCapacity, state.tokens + tokensToAdd);
                state.lastRefillTimestamp = timestampMs;
            }
            
            if (state.tokens + 1e-9 >= cost) {
                state.tokens -= cost;
                return RateLimitResponse.allowed((long) state.tokens);
            } else {
                long retryAfterMs = (long) Math.ceil((cost - state.tokens) * 1000.0 / refillRate);
                return RateLimitResponse.rejected(retryAfterMs, "Rate limit exceeded (Token Bucket)");
            }
        }
    }

    @Override
    public boolean allowEngineAlone(String clientId, long timestampMs, int cost) {
        ClientState state = states.computeIfAbsent(clientId, k -> new ClientState(bucketCapacity, timestampMs));
        synchronized (state) {
            long elapsed = timestampMs - state.lastRefillTimestamp;
            if (elapsed > 0) {
                double tokensToAdd = elapsed * refillRate / 1000.0;
                state.tokens = Math.min(bucketCapacity, state.tokens + tokensToAdd);
                state.lastRefillTimestamp = timestampMs;
            }
            if (state.tokens + 1e-9 >= cost) {
                state.tokens -= cost;
                return true;
            } else {
                return false;
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

    public double getTokens(String clientId) {
        ClientState s = states.get(clientId);
        return s != null ? s.tokens : (double) bucketCapacity;
    }

    public long getBucketCapacity() {
        return bucketCapacity;
    }

    @Override
    public void initializeState(String clientId, String initialState, double timestampSec) {
        long tsMs = (long) (timestampSec * 1000.0);
        if ("drainedFirst".equalsIgnoreCase(initialState) || "drained-first".equalsIgnoreCase(initialState) || "drained".equalsIgnoreCase(initialState)) {
            ClientState s = new ClientState(0, tsMs);
            s.tokens = 0.0;
            states.put(clientId, s);
        } else {
            // coldStart and preWarmed: starts with full bucket
            ClientState s = new ClientState(bucketCapacity, tsMs);
            states.put(clientId, s);
        }
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

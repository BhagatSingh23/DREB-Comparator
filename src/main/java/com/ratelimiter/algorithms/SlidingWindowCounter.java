package com.ratelimiter.algorithms;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Sliding Window Counter algorithm implementation.
 * Note: This is an APPROXIMATION. It assumes uniform request distribution in the previous window.
 */
public class SlidingWindowCounter implements RateLimiter {
    private final long maxRequests;
    private final long windowSizeMs;
    private final ConcurrentMap<String, ClientState> states = new ConcurrentHashMap<>();

    public SlidingWindowCounter(RateLimiterConfig config) {
        if (config.getMaxRequests() < 1) {
            throw new IllegalArgumentException("maxRequests must be >= 1");
        }
        if (config.getWindowSizeMs() < 1) {
            throw new IllegalArgumentException("windowSizeMs must be >= 1");
        }
        this.maxRequests = config.getMaxRequests();
        this.windowSizeMs = config.getWindowSizeMs();
    }

    @Override
    public RateLimitResponse allow(RateLimitRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        ClientState state = states.computeIfAbsent(request.getClientId(), k -> new ClientState());
        
        synchronized (state) {
            long now = request.getTimestampMs();
            long currentWindowKey = (now / windowSizeMs) * windowSizeMs;
            
            // Advance window if needed
            if (state.currentWindowKey != currentWindowKey) {
                if (currentWindowKey - state.currentWindowKey == windowSizeMs) {
                    state.previousCount = state.currentCount;
                } else {
                    state.previousCount = 0;
                }
                state.currentWindowKey = currentWindowKey;
                state.currentCount = 0;
            }
            
            double overlapFraction = 1.0 - ((double)(now - currentWindowKey) / windowSizeMs);
            long estimatedCount = (long) (state.previousCount * overlapFraction) + state.currentCount;
            
            if (estimatedCount + request.getCost() <= maxRequests) {
                state.currentCount += request.getCost();
                long remainingCapacity = maxRequests - (estimatedCount + request.getCost());
                return RateLimitResponse.allowed(remainingCapacity);
            } else {
                return RateLimitResponse.rejected(-1, "Rate limit exceeded (Sliding Window Counter)");
            }
        }
    }

    @Override
    public boolean allowEngineAlone(String clientId, long timestampMs, int cost) {
        ClientState state = states.computeIfAbsent(clientId, k -> new ClientState());
        synchronized (state) {
            long currentWindowKey = (timestampMs / windowSizeMs) * windowSizeMs;
            if (state.currentWindowKey != currentWindowKey) {
                if (currentWindowKey - state.currentWindowKey == windowSizeMs) {
                    state.previousCount = state.currentCount;
                } else {
                    state.previousCount = 0;
                }
                state.currentWindowKey = currentWindowKey;
                state.currentCount = 0;
            }
            double overlapFraction = 1.0 - ((double)(timestampMs - currentWindowKey) / windowSizeMs);
            long estimatedCount = (long) (state.previousCount * overlapFraction) + state.currentCount;
            if (estimatedCount + cost <= maxRequests) {
                state.currentCount += cost;
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public String name() {
        return "SlidingWindowCounter";
    }

    @Override
    public void reset() {
        states.clear();
    }

    @Override
    public void initializeState(String clientId, String initialState, double timestampSec) {
        long tsMs = (long) (timestampSec * 1000.0);
        if ("drainedFirst".equalsIgnoreCase(initialState) || "drained-first".equalsIgnoreCase(initialState) || "drained".equalsIgnoreCase(initialState)) {
            ClientState s = new ClientState();
            s.currentWindowKey = (tsMs / windowSizeMs) * windowSizeMs;
            s.currentCount = maxRequests;
            s.previousCount = 0;
            states.put(clientId, s);
        } else {
            states.remove(clientId);
        }
    }
    
    private static class ClientState {
        long currentWindowKey = -1;
        long currentCount = 0;
        long previousCount = 0;
    }
}

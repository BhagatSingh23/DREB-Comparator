package com.ratelimiter.algorithms;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Sliding Window Log algorithm implementation.
 * Boundary behavior: Timestamps strictly less than (timestampMs - windowSizeMs) are evicted.
 * Entries exactly at (timestampMs - windowSizeMs) are included in the current window.
 */
public class SlidingWindowLog implements RateLimiter {
    private final long maxRequests;
    private final long windowSizeMs;
    private final ConcurrentMap<String, ClientState> states = new ConcurrentHashMap<>();

    public SlidingWindowLog(RateLimiterConfig config) {
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
            long windowStart = now - windowSizeMs;
            
            // Evict old entries
            while (!state.log.isEmpty() && state.log.getFirst() <= windowStart) {
                state.log.removeFirst();
            }
            
            long currentSize = state.log.size();
            long cost = request.getCost();
            
            if (currentSize + cost <= maxRequests) {
                for (long i = 0; i < cost; i++) {
                    state.log.addLast(now);
                }
                long remainingCapacity = maxRequests - (currentSize + cost);
                return RateLimitResponse.allowed(remainingCapacity);
            } else {
                long retryAfterMs = -1;
                if (!state.log.isEmpty()) {
                    retryAfterMs = state.log.getFirst() + windowSizeMs - now;
                    if (retryAfterMs < 0) {
                        retryAfterMs = 0;
                    }
                }
                return RateLimitResponse.rejected(retryAfterMs, "Rate limit exceeded (Sliding Window Log)");
            }
        }
    }

    @Override
    public String name() {
        return "SlidingWindowLog";
    }

    @Override
    public void reset() {
        states.clear();
    }
    
    private static class ClientState {
        final LinkedList<Long> log = new LinkedList<>();
    }
}

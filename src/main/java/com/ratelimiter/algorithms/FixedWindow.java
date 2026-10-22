package com.ratelimiter.algorithms;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Fixed Window algorithm implementation.
 */
public class FixedWindow implements RateLimiter {
    private final long maxRequests;
    private final long windowSizeMs;
    private final ConcurrentMap<String, ClientState> states = new ConcurrentHashMap<>();

    public FixedWindow(RateLimiterConfig config) {
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
            long currentWindow = (now / windowSizeMs) * windowSizeMs;
            
            if (state.windowStart != currentWindow) {
                state.windowStart = currentWindow;
                state.count = 0;
            }
            
            if (state.count + request.getCost() <= maxRequests) {
                state.count += request.getCost();
                long remainingCapacity = maxRequests - state.count;
                return RateLimitResponse.allowed(remainingCapacity);
            } else {
                long windowEnd = currentWindow + windowSizeMs;
                long retryAfterMs = windowEnd - now;
                return RateLimitResponse.rejected(retryAfterMs, "Rate limit exceeded (Fixed Window)");
            }
        }
    }

    @Override
    public String name() {
        return "FixedWindow";
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
            s.windowStart = (tsMs / windowSizeMs) * windowSizeMs;
            s.count = maxRequests;
            states.put(clientId, s);
        } else {
            states.remove(clientId);
        }
    }
    
    private static class ClientState {
        long windowStart = -1;
        long count = 0;
    }
}

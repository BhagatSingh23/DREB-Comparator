package com.ratelimiter.workload;

import com.ratelimiter.core.RateLimitRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Workload specifically designed to expose weaknesses in rate-limiter algorithms.
 */
public final class AdversarialWorkload implements Workload {

    public static final String ALGO_FIXED_WINDOW = "fixed_window";
    public static final String ALGO_TOKEN_BUCKET = "token_bucket";
    public static final String ALGO_SLIDING_WINDOW = "sliding_window_counter";
    private static final String DEFAULT_CLIENT = "adversarial-client";

    private final String targetAlgorithm;
    private final long maxRequests;
    private final long windowSizeMs;
    private final String clientId;

    public AdversarialWorkload(String targetAlgorithm, long maxRequests, long windowSizeMs, String clientId) {
        this.targetAlgorithm = Objects.requireNonNull(targetAlgorithm, "targetAlgorithm");
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be > 0");
        }
        if (windowSizeMs <= 0) {
            throw new IllegalArgumentException("windowSizeMs must be > 0");
        }
        this.maxRequests = maxRequests;
        this.windowSizeMs = windowSizeMs;
        this.clientId = Objects.requireNonNullElse(clientId, DEFAULT_CLIENT);
    }

    public AdversarialWorkload(String targetAlgorithm, long maxRequests, long windowSizeMs) {
        this(targetAlgorithm, maxRequests, windowSizeMs, DEFAULT_CLIENT);
    }

    @Override
    public List<RateLimitRequest> generate(long durationMs, long seed) {
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }
        
        List<RateLimitRequest> requests = new ArrayList<>();
        
        long currentTimeMs = 0;
        
        switch (targetAlgorithm) {
            case ALGO_FIXED_WINDOW:
                // Requests clustered at the end of one window and start of the next
                while (currentTimeMs < durationMs) {
                    long windowEnd = currentTimeMs + windowSizeMs - 1;
                    long nextWindowStart = currentTimeMs + windowSizeMs;
                    
                    // Add maxRequests at the very end of the current window
                    for (int i = 0; i < maxRequests; i++) {
                        if (windowEnd < durationMs) {
                            requests.add(new RateLimitRequest(clientId, windowEnd));
                        }
                    }
                    // Add maxRequests at the very start of the next window
                    for (int i = 0; i < maxRequests; i++) {
                        if (nextWindowStart < durationMs) {
                            requests.add(new RateLimitRequest(clientId, nextWindowStart));
                        }
                    }
                    currentTimeMs += 2 * windowSizeMs;
                }
                break;
                
            case ALGO_TOKEN_BUCKET:
                // Rapid bursts that exact drain the bucket, then single requests immediately after
                while (currentTimeMs < durationMs) {
                    // Drain the bucket instantly
                    for (int i = 0; i < maxRequests; i++) {
                        requests.add(new RateLimitRequest(clientId, currentTimeMs));
                    }
                    // Single request right after to test refill timing
                    long refillInterval = windowSizeMs / maxRequests;
                    long testTime = currentTimeMs + refillInterval / 2; // before a full token refilled
                    if (testTime < durationMs) {
                        requests.add(new RateLimitRequest(clientId, testTime));
                    }
                    currentTimeMs += windowSizeMs;
                }
                break;
                
            case ALGO_SLIDING_WINDOW:
                // Maximize approximation error by putting all requests at the start of the window
                while (currentTimeMs < durationMs) {
                    // Group all traffic precisely at the start of the current window
                    for (int i = 0; i < maxRequests; i++) {
                        requests.add(new RateLimitRequest(clientId, currentTimeMs));
                    }
                    
                    // And some in the middle
                    long halfWindow = currentTimeMs + windowSizeMs / 2;
                    for (int i = 0; i < maxRequests; i++) {
                        if (halfWindow < durationMs) {
                            requests.add(new RateLimitRequest(clientId, halfWindow));
                        }
                    }
                    
                    currentTimeMs += windowSizeMs;
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unknown adversarial target: " + targetAlgorithm);
        }
        
        // Ensure all requests are bounded by durationMs (already handled mostly) and sort them
        requests.removeIf(req -> req.getTimestampMs() >= durationMs);
        requests.sort((r1, r2) -> Long.compare(r1.getTimestampMs(), r2.getTimestampMs()));
        
        return requests;
    }

    @Override
    public String name() {
        return "AdversarialWorkload(target=" + targetAlgorithm + ", clientId=" + clientId + ")";
    }
}

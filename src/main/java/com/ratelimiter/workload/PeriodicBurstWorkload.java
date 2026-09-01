package com.ratelimiter.workload;

import com.ratelimiter.core.RateLimitRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Workload that generates bursts of requests periodically.
 */
public final class PeriodicBurstWorkload implements Workload {

    private final int burstSize;
    private final long periodMs;
    private final String clientId;

    public PeriodicBurstWorkload(int burstSize, long periodMs, String clientId) {
        if (burstSize < 0) {
            throw new IllegalArgumentException("burstSize cannot be negative");
        }
        if (periodMs <= 0) {
            throw new IllegalArgumentException("periodMs must be > 0");
        }
        this.burstSize = burstSize;
        this.periodMs = periodMs;
        this.clientId = Objects.requireNonNullElse(clientId, "client-0");
    }

    @Override
    public List<RateLimitRequest> generate(long durationMs, long seed) {
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }
        List<RateLimitRequest> requests = new ArrayList<>();
        
        long currentTimeMs = 0;
        while (currentTimeMs < durationMs) {
            for (int i = 0; i < burstSize; i++) {
                requests.add(new RateLimitRequest(clientId, currentTimeMs));
            }
            currentTimeMs += periodMs;
        }
        
        return requests;
    }

    @Override
    public String name() {
        return "PeriodicBurstWorkload(" + burstSize + " requests every " + periodMs + "ms, clientId=" + clientId + ")";
    }
}

package com.ratelimiter.workload;

import com.ratelimiter.core.RateLimitRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Workload that generates a sudden burst of requests at a specific time.
 */
public final class BurstWorkload implements Workload {

    private final int burstSize;
    private final long burstTimeMs;
    private final String clientId;

    public BurstWorkload(int burstSize, long burstTimeMs, String clientId) {
        if (burstSize < 0) {
            throw new IllegalArgumentException("burstSize cannot be negative");
        }
        if (burstTimeMs < 0) {
            throw new IllegalArgumentException("burstTimeMs cannot be negative");
        }
        this.burstSize = burstSize;
        this.burstTimeMs = burstTimeMs;
        this.clientId = Objects.requireNonNullElse(clientId, "client-0");
    }

    public BurstWorkload(int burstSize, String clientId) {
        this(burstSize, 0L, clientId);
    }

    @Override
    public List<RateLimitRequest> generate(long durationMs, long seed) {
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }
        List<RateLimitRequest> requests = new ArrayList<>();
        
        if (burstTimeMs < durationMs) {
            for (int i = 0; i < burstSize; i++) {
                requests.add(new RateLimitRequest(clientId, burstTimeMs));
            }
        }
        
        return requests;
    }

    @Override
    public String name() {
        return "BurstWorkload(" + burstSize + " requests at " + burstTimeMs + "ms, clientId=" + clientId + ")";
    }
}

package com.ratelimiter.workload;

import com.ratelimiter.core.RateLimitRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Workload that generates requests at a constant, fixed interval.
 */
public final class ConstantWorkload implements Workload {

    private final int requestsPerSecond;
    private final String clientId;

    public ConstantWorkload(int requestsPerSecond, String clientId) {
        if (requestsPerSecond <= 0) {
            throw new IllegalArgumentException("requestsPerSecond must be > 0");
        }
        this.requestsPerSecond = requestsPerSecond;
        this.clientId = Objects.requireNonNullElse(clientId, "client-0");
    }

    public ConstantWorkload(int requestsPerSecond) {
        this(requestsPerSecond, "client-0");
    }

    @Override
    public List<RateLimitRequest> generate(long durationMs, long seed) {
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }
        long totalRequests = (durationMs * requestsPerSecond) / 1000;
        List<RateLimitRequest> requests = new ArrayList<>((int) Math.min(totalRequests, Integer.MAX_VALUE));
        
        for (long i = 0; i < totalRequests; i++) {
            long currentTimeMs = (i * 1000L) / requestsPerSecond;
            requests.add(new RateLimitRequest(clientId, currentTimeMs));
        }
        
        return requests;
    }

    @Override
    public String name() {
        return "ConstantWorkload(" + requestsPerSecond + "rps, clientId=" + clientId + ")";
    }
}

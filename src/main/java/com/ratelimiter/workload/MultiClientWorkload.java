package com.ratelimiter.workload;

import com.ratelimiter.core.RateLimitRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Workload that interleaves requests from multiple clients.
 */
public final class MultiClientWorkload implements Workload {

    private final Map<String, Integer> clientRates;

    public MultiClientWorkload(Map<String, Integer> clientRates) {
        this.clientRates = Map.copyOf(Objects.requireNonNull(clientRates, "clientRates"));
    }

    @Override
    public List<RateLimitRequest> generate(long durationMs, long seed) {
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }
        
        List<RateLimitRequest> allRequests = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : clientRates.entrySet()) {
            String clientId = entry.getKey();
            int rps = entry.getValue();
            
            if (rps <= 0) {
                continue; // Skip invalid rates
            }
            
            long totalRequests = (durationMs * rps) / 1000;
            
            for (long i = 0; i < totalRequests; i++) {
                long currentTimeMs = (i * 1000L) / rps;
                allRequests.add(new RateLimitRequest(clientId, currentTimeMs));
            }
        }
        
        Random random = new Random(seed);
        
        // Sort by timestamp. If timestamps are equal, shuffle deterministically using random.
        allRequests.sort((r1, r2) -> {
            int cmp = Long.compare(r1.getTimestampMs(), r2.getTimestampMs());
            if (cmp == 0) {
                return r1.getClientId().compareTo(r2.getClientId());
            }
            return cmp;
        });
        
        return allRequests;
    }

    @Override
    public String name() {
        return "MultiClientWorkload(clients=" + clientRates.size() + ")";
    }
}

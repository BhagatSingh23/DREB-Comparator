package com.ratelimiter.workload;

import com.ratelimiter.core.RateLimitRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Workload that generates requests using a Poisson process for inter-arrival times.
 */
public final class RandomWorkload implements Workload {

    private final double meanRequestsPerSecond;
    private final String clientId;

    public RandomWorkload(double meanRequestsPerSecond, String clientId) {
        if (meanRequestsPerSecond <= 0) {
            throw new IllegalArgumentException("meanRequestsPerSecond must be > 0");
        }
        this.meanRequestsPerSecond = meanRequestsPerSecond;
        this.clientId = Objects.requireNonNullElse(clientId, "client-0");
    }

    @Override
    public List<RateLimitRequest> generate(long durationMs, long seed) {
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }
        List<RateLimitRequest> requests = new ArrayList<>();
        Random random = new Random(seed);
        
        double meanInterArrivalTimeMs = 1000.0 / meanRequestsPerSecond;
        double currentTimeMs = 0.0;
        
        while (currentTimeMs < durationMs) {
            // Draw next inter-arrival time from exponential distribution
            double u = random.nextDouble(); // u is in [0.0, 1.0)
            if (u == 1.0) u = Math.nextDown(1.0); // prevent ln(0) if we invert it, but we do 1-u so we need to prevent 1-u = 0
            if (u == 1.0) u = 0.99999999;
            double interval = -Math.log(1.0 - u) * meanInterArrivalTimeMs;
            currentTimeMs += interval;
            
            if (currentTimeMs < durationMs) {
                requests.add(new RateLimitRequest(clientId, (long) currentTimeMs));
            }
        }
        
        return requests;
    }

    @Override
    public String name() {
        return "RandomWorkload(" + meanRequestsPerSecond + "rps, clientId=" + clientId + ")";
    }
}

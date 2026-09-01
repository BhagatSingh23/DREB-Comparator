package com.ratelimiter.workload;

import com.ratelimiter.core.RateLimitRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Workload that simulates a single client sending requests via a Poisson
 * process at a rate just below the configured refill rate, sustained for the
 * full benchmark duration.
 *
 * <p>This models a "chronic edge-rider" — a client who never bursts but
 * rides just under the nominal limit indefinitely. Standard algorithms
 * typically allow all such traffic. DREB's sustained-bucket should also
 * allow most of it since the rate is below the sustained refill rate.
 * (Matches paper Section 7.1's Chronic Edge-Rider scenario.)
 */
public final class ChronicEdgeRiderWorkload implements Workload {

    private final double requestsPerSecond;
    private final String clientId;

    /**
     * @param requestsPerSecond the Poisson arrival rate (e.g. 0.98 * refillRate)
     * @param clientId          client identifier
     */
    public ChronicEdgeRiderWorkload(double requestsPerSecond, String clientId) {
        if (requestsPerSecond <= 0) {
            throw new IllegalArgumentException("requestsPerSecond must be positive");
        }
        this.requestsPerSecond = requestsPerSecond;
        this.clientId = clientId != null ? clientId : "client-0";
    }

    @Override
    public List<RateLimitRequest> generate(long durationMs, long seed) {
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }

        List<RateLimitRequest> requests = new ArrayList<>();
        Random rng = new Random(seed);

        // Poisson inter-arrival times: -ln(U) / lambda
        double lambdaMs = requestsPerSecond / 1000.0; // convert to per-millisecond
        double currentTimeMs = 0.0;

        while (currentTimeMs < durationMs) {
            requests.add(new RateLimitRequest(clientId, (long) currentTimeMs));
            // Exponential inter-arrival: -ln(U) / lambda
            double u = rng.nextDouble();
            if (u == 0.0) u = Double.MIN_VALUE; // avoid ln(0)
            double intervalMs = -Math.log(u) / lambdaMs;
            currentTimeMs += intervalMs;
        }

        return requests;
    }

    @Override
    public String name() {
        return "ChronicEdgeRiderWorkload(rate=" + requestsPerSecond + "/s, clientId=" + clientId + ")";
    }
}

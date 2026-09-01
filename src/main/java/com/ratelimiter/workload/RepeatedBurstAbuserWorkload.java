package com.ratelimiter.workload;

import com.ratelimiter.core.RateLimitRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Workload that fires repeated bursts with a cycle interval chosen so
 * the long-run average rate exceeds the sustained refill rate.
 *
 * <p>Each cycle fires {@code burstSize} requests at 1ms spacing, then
 * goes silent until the next cycle. The cycle interval is set such that
 * the long-run average rate is {@code targetAvgRate} requests/sec.
 *
 * <p>This models a "repeated-burst abuser" — a client whose individual
 * bursts look locally compliant but whose long-run average deliberately
 * exceeds the sustained limit. DREB's sustained-bucket should progressively
 * drain across cycles, rejecting an increasing fraction over time, while
 * a standard Token Bucket may continue accepting because it only checks
 * instantaneous capacity. (Matches paper Section 7.1's Repeated-Burst
 * Abuser scenario.)
 */
public final class RepeatedBurstAbuserWorkload implements Workload {

    private final int burstSize;
    private final long cycleIntervalMs;
    private final String clientId;

    /**
     * @param burstSize       requests per burst
     * @param cycleIntervalMs milliseconds between burst starts
     * @param clientId        client identifier
     */
    public RepeatedBurstAbuserWorkload(int burstSize, long cycleIntervalMs, String clientId) {
        if (burstSize <= 0) {
            throw new IllegalArgumentException("burstSize must be positive");
        }
        if (cycleIntervalMs <= 0) {
            throw new IllegalArgumentException("cycleIntervalMs must be positive");
        }
        this.burstSize = burstSize;
        this.cycleIntervalMs = cycleIntervalMs;
        this.clientId = clientId != null ? clientId : "client-0";
    }

    @Override
    public List<RateLimitRequest> generate(long durationMs, long seed) {
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }

        List<RateLimitRequest> requests = new ArrayList<>();
        long cycleStart = 0;

        while (cycleStart < durationMs) {
            // Fire burst instantly
            for (int i = 0; i < burstSize; i++) {
                if (cycleStart < durationMs) {
                    requests.add(new RateLimitRequest(clientId, cycleStart));
                }
            }
            cycleStart += cycleIntervalMs;
        }

        return requests;
    }

    @Override
    public String name() {
        return "RepeatedBurstAbuserWorkload(burst=" + burstSize
                + ", cycle=" + cycleIntervalMs + "ms, clientId=" + clientId + ")";
    }
}

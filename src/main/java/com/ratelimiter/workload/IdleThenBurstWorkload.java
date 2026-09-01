package com.ratelimiter.workload;

import com.ratelimiter.core.RateLimitRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Workload that simulates a client sending one request to establish state,
 * remaining idle for a configurable duration, then firing a burst.
 *
 * <p>This workload exists specifically to test idle-credit / idle-reward
 * mechanisms (e.g. DREB's idle-credit accrual, paper Section 4.2). Standard
 * algorithms do not benefit from idle time — a Token Bucket refills to its
 * cap and stops — so they perform identically to a cold-start burst once the
 * bucket is full. An algorithm with idle-credit accrual should accept
 * <em>more</em> burst requests than a plain Token Bucket here, which is the
 * inverse pattern from the {@link BurstWorkload} (cold-start, no idle time).
 *
 * <p>Sequence of generated requests:
 * <ol>
 *   <li>A single "touch" request at {@code t=0} to establish client state
 *       (lazy initialization — identical across all algorithms).</li>
 *   <li>Silence for {@code idleDurationMs}.</li>
 *   <li>{@code burstSize} requests at minimal spacing (1 ms apart, starting
 *       at {@code idleDurationMs}), matching the existing {@code BurstWorkload}'s
 *       fire-as-fast-as-possible pattern.</li>
 * </ol>
 */
public final class IdleThenBurstWorkload implements Workload {

    private final long idleDurationMs;
    private final int burstSize;
    private final String clientId;

    /**
     * @param idleDurationMs how long the client sits idle after the initial touch
     * @param burstSize      number of requests in the post-idle burst
     * @param clientId       client identifier (null defaults to "client-0")
     */
    public IdleThenBurstWorkload(long idleDurationMs, int burstSize, String clientId) {
        if (idleDurationMs < 0) {
            throw new IllegalArgumentException("idleDurationMs cannot be negative");
        }
        if (burstSize < 0) {
            throw new IllegalArgumentException("burstSize cannot be negative");
        }
        this.idleDurationMs = idleDurationMs;
        this.burstSize = burstSize;
        this.clientId = Objects.requireNonNullElse(clientId, "client-0");
    }

    @Override
    public List<RateLimitRequest> generate(long durationMs, long seed) {
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }
        List<RateLimitRequest> requests = new ArrayList<>();

        // 1. Touch at t=0 to establish client state (lazy init)
        requests.add(new RateLimitRequest(clientId, 0L));

        // 2. Burst starting at t=idleDurationMs, 1 ms apart
        for (int i = 0; i < burstSize; i++) {
            long ts = idleDurationMs + i;
            if (ts < durationMs) {
                requests.add(new RateLimitRequest(clientId, ts));
            }
        }

        return requests;
    }

    @Override
    public String name() {
        return "IdleThenBurstWorkload(idle=" + idleDurationMs + "ms, burst=" + burstSize
                + ", clientId=" + clientId + ")";
    }
}

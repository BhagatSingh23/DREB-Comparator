package com.ratelimiter.core;

import java.util.Objects;

/**
 * Immutable request object passed to every {@link RateLimiter#allow} call.
 *
 * <p>Validation is performed eagerly in the constructor: null/empty client IDs,
 * negative timestamps, and non-positive costs all throw
 * {@link IllegalArgumentException}.
 *
 * <p>{@code cost} defaults to 1 and represents the number of "units" this
 * request consumes. Algorithms that do not naturally model variable cost
 * treat {@code cost} as "number of unit-requests" (i.e. decrement capacity
 * by {@code cost}).
 */
public final class RateLimitRequest {

    private final String clientId;
    private final long timestampMs;
    private final long cost;

    /**
     * Creates a request with cost = 1.
     *
     * @param clientId    non-null, non-empty identifier for the client
     * @param timestampMs logical timestamp in milliseconds; must be ≥ 0
     */
    public RateLimitRequest(String clientId, long timestampMs) {
        this(clientId, timestampMs, 1L);
    }

    /**
     * Creates a request with an explicit cost.
     *
     * @param clientId    non-null, non-empty identifier for the client
     * @param timestampMs logical timestamp in milliseconds; must be ≥ 0
     * @param cost        number of capacity units consumed; must be ≥ 1
     * @throws IllegalArgumentException if any argument is invalid
     */
    public RateLimitRequest(String clientId, long timestampMs, long cost) {
        if (clientId == null) {
            throw new IllegalArgumentException("clientId must not be null");
        }
        if (clientId.isEmpty()) {
            throw new IllegalArgumentException("clientId must not be empty");
        }
        if (timestampMs < 0) {
            throw new IllegalArgumentException("timestampMs must be non-negative, got: " + timestampMs);
        }
        if (cost < 1) {
            throw new IllegalArgumentException("cost must be at least 1, got: " + cost);
        }
        this.clientId = clientId;
        this.timestampMs = timestampMs;
        this.cost = cost;
    }

    public String getClientId() {
        return clientId;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public long getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return "RateLimitRequest{clientId='" + clientId + "', timestampMs=" + timestampMs + ", cost=" + cost + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RateLimitRequest that)) return false;
        return timestampMs == that.timestampMs && cost == that.cost && Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, timestampMs, cost);
    }
}

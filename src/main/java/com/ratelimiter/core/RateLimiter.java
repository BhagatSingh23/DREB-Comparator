package com.ratelimiter.core;

/**
 * Common interface for all rate-limiting algorithms.
 *
 * <p>Every algorithm receives an identical {@link RateLimitRequest} and returns
 * a {@link RateLimitResponse} so that the benchmark framework can evaluate them
 * under identical conditions.
 *
 * <p>Implementations <b>must</b> be thread-safe — the benchmark will invoke
 * {@code allow()} concurrently from many (potentially virtual) threads.
 * Per-client synchronization is preferred over a single global lock.
 */
public interface RateLimiter {

    /**
     * Decides whether the given request should be allowed or rejected
     * according to this algorithm's rate-limiting rules.
     *
     * @param request the incoming request to evaluate; must not be null
     * @return a response indicating whether the request was allowed,
     *         remaining capacity (if applicable), retry-after hint, and reason
     * @throws IllegalArgumentException if {@code request} is null or contains invalid fields
     */
    RateLimitResponse allow(RateLimitRequest request);

    /**
     * Direct evaluation without allocating a RateLimitRequest object.
     * Returns RateLimitResponse.
     *
     * @param clientId    client identifier
     * @param timestampMs timestamp in milliseconds
     * @param cost        token cost of request
     * @return RateLimitResponse containing decision and metadata
     */
    default RateLimitResponse allowDirect(String clientId, long timestampMs, int cost) {
        return allow(new RateLimitRequest(clientId, timestampMs, cost));
    }

    /**
     * Returns a human-readable name for this algorithm, used in reports and charts.
     *
     * @return algorithm name, e.g. "Token Bucket", "Fixed Window"
     */
    String name();

    /**
     * Resets all internal state. Useful between benchmark runs to ensure
     * a clean starting point.
     */
    void reset();

    /**
     * Initializes the state for a client to support explicit starting conditions
     * (e.g. "coldStart", "preWarmed", "drainedFirst").
     *
     * @param clientId     client identifier
     * @param initialState "coldStart", "preWarmed", or "drainedFirst"
     * @param timestampSec starting timestamp in seconds
     */
    default void initializeState(String clientId, String initialState, double timestampSec) {
        // default no-op
    }

    /**
     * Primitive engine evaluation returning boolean without RateLimitRequest/Response wrapping.
     * Default implementation delegates to allow(new RateLimitRequest(...)).isAllowed()
     * but algorithm implementations override to execute pure zero-allocation math.
     *
     * @param clientId    client identifier
     * @param timestampMs timestamp in milliseconds
     * @param cost        token cost of request
     * @return true if allowed, false if rejected
     */
    default boolean allowEngineAlone(String clientId, long timestampMs, int cost) {
        return allow(new RateLimitRequest(clientId, timestampMs, cost)).isAllowed();
    }
}

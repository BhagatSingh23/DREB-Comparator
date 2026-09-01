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
}

package com.ratelimiter.core;

/**
 * Immutable response returned by every {@link RateLimiter#allow} call.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code allowed} — whether the request was admitted.</li>
 *   <li>{@code remainingCapacity} — how many more unit-cost requests can be
 *       admitted before the next refill/window-reset. {@code -1} if the
 *       algorithm does not track this.</li>
 *   <li>{@code retryAfterMs} — suggested wait time before retrying.
 *       {@code -1} if not applicable or not computed.</li>
 *   <li>{@code reason} — human-readable rejection reason; {@code null}
 *       when {@code allowed == true}.</li>
 * </ul>
 */
public final class RateLimitResponse {

    private final boolean allowed;
    private final long remainingCapacity;
    private final long retryAfterMs;
    private final String reason;

    /**
     * Convenience factory for an allowed response.
     *
     * @param remainingCapacity remaining capacity after this request
     * @return allowed response
     */
    public static RateLimitResponse allowed(long remainingCapacity) {
        return new RateLimitResponse(true, remainingCapacity, -1, null);
    }

    /**
     * Convenience factory for a rejected response.
     *
     * @param retryAfterMs suggested retry delay in ms (-1 if unknown)
     * @param reason       human-readable reason for rejection
     * @return rejected response
     */
    public static RateLimitResponse rejected(long retryAfterMs, String reason) {
        return new RateLimitResponse(false, 0, retryAfterMs, reason);
    }

    /**
     * Full constructor.
     *
     * @param allowed           whether the request was admitted
     * @param remainingCapacity remaining capacity; -1 if not tracked
     * @param retryAfterMs      suggested retry delay; -1 if not applicable
     * @param reason            rejection reason; null if allowed
     */
    public RateLimitResponse(boolean allowed, long remainingCapacity, long retryAfterMs, String reason) {
        this.allowed = allowed;
        this.remainingCapacity = remainingCapacity;
        this.retryAfterMs = retryAfterMs;
        this.reason = reason;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getRemainingCapacity() {
        return remainingCapacity;
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "RateLimitResponse{allowed=" + allowed
                + ", remainingCapacity=" + remainingCapacity
                + ", retryAfterMs=" + retryAfterMs
                + ", reason='" + reason + "'}";
    }
}

package com.ratelimiter.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks correctness violations of rate limiting windows.
 */
public class AccuracyMetrics {

    /**
     * Represents a single correctness violation.
     */
    public record Violation(long windowId, long acceptedCount, long limit) {
    }

    private final List<Violation> violations = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * Records the result of a window evaluation.
     * A violation is recorded if acceptedCount > limit.
     * 
     * @param windowId the identifier for the window
     * @param acceptedCount the number of accepted requests in the window
     * @param limit the configured limit for the window
     */
    public void recordWindowResult(long windowId, long acceptedCount, long limit) {
        if (acceptedCount > limit) {
            violations.add(new Violation(windowId, acceptedCount, limit));
        }
    }

    /**
     * Returns the total number of correctness violations.
     * @return violation count
     */
    public long getViolationCount() {
        return violations.size();
    }

    /**
     * Returns a list of all recorded violations.
     * @return list of violations
     */
    public List<Violation> getViolations() {
        synchronized (violations) {
            return new ArrayList<>(violations);
        }
    }

    /**
     * Computes the overall acceptance rate.
     * 
     * @param totalAccepted total accepted requests
     * @param totalRequests total requests made
     * @return acceptance rate (0.0 to 1.0), or 0.0 if totalRequests is 0
     */
    public double getAcceptanceRate(long totalAccepted, long totalRequests) {
        if (totalRequests <= 0) {
            return 0.0;
        }
        return (double) totalAccepted / totalRequests;
    }

    /**
     * Resets the metrics.
     */
    public void reset() {
        violations.clear();
    }
}

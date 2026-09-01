package com.ratelimiter.metrics;

import java.util.Map;

/**
 * Computes fairness metrics using Jain's Fairness Index.
 */
public class FairnessMetrics {

    /**
     * Computes Jain's Fairness Index for the given map of accepted requests per client.
     * <p>
     * Formula: J(x1..xn) = (Σxi)² / (n · Σxi²)
     * </p>
     * 
     * @param clientAcceptedCounts map of client IDs to accepted request counts
     * @return fairness index between 1/n and 1.0. 1.0 represents perfect fairness.
     */
    public static double jainsIndex(Map<String, Long> clientAcceptedCounts) {
        if (clientAcceptedCounts == null || clientAcceptedCounts.isEmpty() || clientAcceptedCounts.size() == 1) {
            return 1.0;
        }
        
        long[] values = new long[clientAcceptedCounts.size()];
        int idx = 0;
        for (Long count : clientAcceptedCounts.values()) {
            values[idx++] = (count == null) ? 0L : count;
        }
        
        return jainsIndex(values);
    }

    /**
     * Computes Jain's Fairness Index for the given array of values.
     * <p>
     * Formula: J(x1..xn) = (Σxi)² / (n · Σxi²)
     * </p>
     * 
     * @param values array of allocation values
     * @return fairness index between 1/n and 1.0. 1.0 represents perfect fairness.
     */
    public static double jainsIndex(long[] values) {
        if (values == null || values.length <= 1) {
            return 1.0;
        }
        
        double sum = 0.0;
        double sumOfSquares = 0.0;
        
        for (long value : values) {
            double v = (double) value;
            sum += v;
            sumOfSquares += (v * v);
        }
        
        if (sum == 0.0) {
            return 1.0;
        }
        
        int n = values.length;
        return (sum * sum) / (n * sumOfSquares);
    }
}

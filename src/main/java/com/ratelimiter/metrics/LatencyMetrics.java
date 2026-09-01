package com.ratelimiter.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Collects and computes latency metrics.
 */
public class LatencyMetrics {

    private final List<Long> measurements = new ArrayList<>();

    /**
     * Records a latency measurement.
     * @param nanos latency in nanoseconds
     */
    public synchronized void record(long nanos) {
        measurements.add(nanos);
    }

    /**
     * Returns the average latency.
     * @return average latency in nanoseconds
     */
    public synchronized double getAverage() {
        if (measurements.isEmpty()) {
            return 0.0;
        }
        long sum = 0;
        for (Long val : measurements) {
            sum += val;
        }
        return (double) sum / measurements.size();
    }

    /**
     * Returns the latency percentile.
     * @param p percentile (e.g., 99.0 for p99)
     * @return percentile latency in nanoseconds
     */
    public synchronized long getPercentile(double p) {
        if (measurements.isEmpty()) {
            return 0L;
        }
        long[] sorted = getSortedArray();
        int index = (int) Math.ceil((p / 100.0) * sorted.length) - 1;
        index = Math.max(0, Math.min(index, sorted.length - 1));
        return sorted[index];
    }
    
    /**
     * Returns the median (p50) latency.
     * @return median latency in nanoseconds
     */
    public long getP50() {
        return getPercentile(50.0);
    }
    
    /**
     * Returns the p90 latency.
     * @return p90 latency in nanoseconds
     */
    public long getP90() {
        return getPercentile(90.0);
    }
    
    /**
     * Returns the p95 latency.
     * @return p95 latency in nanoseconds
     */
    public long getP95() {
        return getPercentile(95.0);
    }
    
    /**
     * Returns the p99 latency.
     * @return p99 latency in nanoseconds
     */
    public long getP99() {
        return getPercentile(99.0);
    }

    /**
     * Returns the maximum latency.
     * @return maximum latency in nanoseconds
     */
    public synchronized long getMax() {
        if (measurements.isEmpty()) {
            return 0L;
        }
        long max = Long.MIN_VALUE;
        for (Long val : measurements) {
            if (val > max) {
                max = val;
            }
        }
        return max;
    }

    /**
     * Returns the minimum latency.
     * @return minimum latency in nanoseconds
     */
    public synchronized long getMin() {
        if (measurements.isEmpty()) {
            return 0L;
        }
        long min = Long.MAX_VALUE;
        for (Long val : measurements) {
            if (val < min) {
                min = val;
            }
        }
        return min;
    }

    /**
     * Returns the total count of measurements.
     * @return total count
     */
    public synchronized long getCount() {
        return measurements.size();
    }

    /**
     * Resets the metrics.
     */
    public synchronized void reset() {
        measurements.clear();
    }
    
    private long[] getSortedArray() {
        long[] array = new long[measurements.size()];
        for (int i = 0; i < measurements.size(); i++) {
            array[i] = measurements.get(i);
        }
        Arrays.sort(array);
        return array;
    }
}

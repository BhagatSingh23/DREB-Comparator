package com.ratelimiter.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and computes throughput metrics.
 */
public class ThroughputMetrics {

    private final AtomicLong totalOps = new AtomicLong(0);
    private final AtomicLong startNanos = new AtomicLong(0);
    private final AtomicLong endNanos = new AtomicLong(0);
    
    /**
     * Starts the throughput measurement.
     */
    public void start() {
        startNanos.set(System.nanoTime());
    }
    
    /**
     * Stops the throughput measurement.
     */
    public void stop() {
        endNanos.set(System.nanoTime());
    }

    /**
     * Records a single operation.
     */
    public void recordOperation() {
        totalOps.incrementAndGet();
    }
    
    /**
     * Records a specific number of operations.
     * @param ops operations count
     */
    public void recordOperations(long ops) {
        totalOps.addAndGet(ops);
    }

    /**
     * Returns the total number of operations recorded.
     * @return total operations
     */
    public long getTotalOps() {
        return totalOps.get();
    }

    /**
     * Computes the operations per second.
     * @return operations per second, or 0.0 if duration is 0.
     */
    public double getOpsPerSecond() {
        long durationNanos = getDurationNanos();
        if (durationNanos == 0) {
            return 0.0;
        }
        double durationSeconds = durationNanos / 1_000_000_000.0;
        return totalOps.get() / durationSeconds;
    }

    private long getDurationNanos() {
        long start = startNanos.get();
        long end = endNanos.get();
        if (start == 0) {
            return 0;
        }
        if (end == 0) {
            return System.nanoTime() - start;
        }
        return end - start;
    }

    /**
     * Resets the metrics.
     */
    public void reset() {
        totalOps.set(0);
        startNanos.set(0);
        endNanos.set(0);
    }
}

package com.ratelimiter.metrics;

/**
 * Collects and computes memory metrics.
 */
public class MemoryMetrics {

    private long beforeHeapBytes = 0;
    private long afterHeapBytes = 0;

    /**
     * Captures the heap usage before the benchmark.
     */
    public void captureBeforeSnapshot() {
        System.gc(); // Suggest GC to get a cleaner baseline
        Runtime runtime = Runtime.getRuntime();
        beforeHeapBytes = runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Captures the heap usage after the benchmark.
     */
    public void captureAfterSnapshot() {
        System.gc(); // Suggest GC to measure retained heap
        Runtime runtime = Runtime.getRuntime();
        afterHeapBytes = runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Returns the heap memory used after the benchmark.
     * @return used memory in bytes
     */
    public long getUsedMemoryBytes() {
        return afterHeapBytes;
    }

    /**
     * Returns the difference between memory usage after and before the benchmark.
     * @return peak memory delta in bytes
     */
    public long getPeakMemoryDelta() {
        return Math.max(0, afterHeapBytes - beforeHeapBytes);
    }

    /**
     * Estimates the memory size of per-client state.
     * <p>
     * Limitation: this is a rough estimate based on overall heap growth, not precise instrumentation.
     * Background tasks, GC behaviour, and other overhead can skew this value.
     * </p>
     * 
     * @param clientCount number of clients
     * @return estimated bytes per client
     */
    public long getEstimatedPerClientBytes(int clientCount) {
        if (clientCount <= 0) {
            return 0;
        }
        return getPeakMemoryDelta() / clientCount;
    }

    /**
     * Resets the metrics and clears snapshots.
     */
    public void reset() {
        beforeHeapBytes = 0;
        afterHeapBytes = 0;
    }
}

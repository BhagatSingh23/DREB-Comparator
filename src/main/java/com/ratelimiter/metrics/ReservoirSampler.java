package com.ratelimiter.metrics;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fixed-size reservoir sampler for latency measurements using Algorithm R
 * (Vitter, 1985). Maintains a uniform random sample of at most
 * {@code capacity} elements from an arbitrarily large stream, so memory
 * usage stays O(capacity) regardless of total request count.
 *
 * <p>100,000 samples is sufficient for accurate p50/p95/p99 estimation
 * even at millions of total requests (standard error < 0.1% at these
 * percentiles).
 *
 * <p>Thread-safe via synchronization — suitable for the concurrent
 * execution path.
 */
public final class ReservoirSampler {

    public static final int DEFAULT_CAPACITY = 100_000;

    private final long[] reservoir;
    private final int capacity;
    private long count;          // total elements seen
    private double runningSum;   // for exact mean computation

    public ReservoirSampler() {
        this(DEFAULT_CAPACITY);
    }

    public ReservoirSampler(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
        this.capacity = capacity;
        this.reservoir = new long[capacity];
        this.count = 0;
        this.runningSum = 0;
    }

    /**
     * Records a latency sample. If the reservoir is not yet full, appends
     * directly. Otherwise uses reservoir sampling (Algorithm R) to decide
     * whether to replace an existing element.
     */
    public synchronized void add(long value) {
        runningSum += value;
        if (count < capacity) {
            reservoir[(int) count] = value;
        } else {
            // Replace element at random index with probability capacity/count
            long j = ThreadLocalRandom.current().nextLong(count + 1);
            if (j < capacity) {
                reservoir[(int) j] = value;
            }
        }
        count++;
    }

    /** Total number of elements seen (not just stored). */
    public synchronized long getCount() {
        return count;
    }

    /** Exact arithmetic mean of ALL elements seen (not just sampled). */
    public synchronized double getMean() {
        return count == 0 ? 0 : runningSum / count;
    }

    /**
     * Returns a sorted copy of the reservoir for percentile computation.
     * The array length is {@code min(count, capacity)}.
     */
    public synchronized long[] getSortedSample() {
        int size = (int) Math.min(count, capacity);
        long[] copy = Arrays.copyOf(reservoir, size);
        Arrays.sort(copy);
        return copy;
    }

    /** Returns the exact sum of all elements seen. */
    public synchronized double getSum() {
        return runningSum;
    }
}

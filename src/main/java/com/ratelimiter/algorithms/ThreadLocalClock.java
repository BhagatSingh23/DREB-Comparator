package com.ratelimiter.algorithms;

import com.dreb.ratelimit.Clock;

/**
 * Thread-safe clock implementation that maintains per-thread time state.
 * Eliminates data races when multiple threads concurrently execute
 * rate-limiting decisions with distinct request timestamps.
 */
public final class ThreadLocalClock implements Clock {

    private final ThreadLocal<double[]> currentTime = ThreadLocal.withInitial(() -> new double[1]);

    public ThreadLocalClock() {
    }

    public ThreadLocalClock(double initialTime) {
        currentTime.get()[0] = initialTime;
    }

    public void set(double timestampSeconds) {
        currentTime.get()[0] = timestampSeconds;
    }

    @Override
    public double now() {
        return currentTime.get()[0];
    }
}

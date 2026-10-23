package com.ratelimiter.algorithms;

import com.dreb.ratelimit.Clock;

/**
 * Thread-safe clock implementation that maintains per-thread time state.
 * Eliminates data races when multiple threads concurrently execute
 * rate-limiting decisions with distinct request timestamps.
 */
public final class ThreadLocalClock implements Clock {

    private final ThreadLocal<Double> currentTime = ThreadLocal.withInitial(() -> 0.0);

    public ThreadLocalClock() {
    }

    public ThreadLocalClock(double initialTime) {
        currentTime.set(initialTime);
    }

    public void set(double timestampSeconds) {
        currentTime.set(timestampSeconds);
    }

    @Override
    public double now() {
        return currentTime.get();
    }
}

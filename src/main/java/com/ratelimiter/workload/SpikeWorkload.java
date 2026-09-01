package com.ratelimiter.workload;

import com.ratelimiter.core.RateLimitRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Workload that operates at a base rate and generates a massive spike for a duration.
 */
public final class SpikeWorkload implements Workload {

    private final int baseRatePerSecond;
    private final int spikeMultiplier;
    private final long spikeStartMs;
    private final long spikeDurationMs;
    private final String clientId;

    public SpikeWorkload(int baseRatePerSecond, int spikeMultiplier, long spikeStartMs, long spikeDurationMs, String clientId) {
        if (baseRatePerSecond <= 0) {
            throw new IllegalArgumentException("baseRatePerSecond must be > 0");
        }
        if (spikeMultiplier <= 0) {
            throw new IllegalArgumentException("spikeMultiplier must be > 0");
        }
        if (spikeStartMs < 0 || spikeDurationMs < 0) {
            throw new IllegalArgumentException("spikeStartMs and spikeDurationMs cannot be negative");
        }
        this.baseRatePerSecond = baseRatePerSecond;
        this.spikeMultiplier = spikeMultiplier;
        this.spikeStartMs = spikeStartMs;
        this.spikeDurationMs = spikeDurationMs;
        this.clientId = Objects.requireNonNullElse(clientId, "client-0");
    }

    @Override
    public List<RateLimitRequest> generate(long durationMs, long seed) {
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }
        List<RateLimitRequest> requests = new ArrayList<>();
        
        double baseIntervalMs = 1000.0 / baseRatePerSecond;
        double spikeIntervalMs = 1000.0 / (baseRatePerSecond * (long) spikeMultiplier);
        
        double currentTimeMs = 0;
        long spikeEndMs = spikeStartMs + spikeDurationMs;
        
        while (currentTimeMs < durationMs) {
            requests.add(new RateLimitRequest(clientId, (long) currentTimeMs));
            
            if (currentTimeMs >= spikeStartMs && currentTimeMs < spikeEndMs) {
                currentTimeMs += spikeIntervalMs;
            } else {
                currentTimeMs += baseIntervalMs;
            }
        }
        
        return requests;
    }

    @Override
    public String name() {
        return "SpikeWorkload(base=" + baseRatePerSecond + "rps, multiplier=" + spikeMultiplier + "x, clientId=" + clientId + ")";
    }
}

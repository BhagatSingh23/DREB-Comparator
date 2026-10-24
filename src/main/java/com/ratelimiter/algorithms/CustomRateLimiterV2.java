package com.ratelimiter.algorithms;

import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiterConfig;

import com.dreb.ratelimit.AdaptiveIdleCreditPolicy;
import com.dreb.ratelimit.ClientState;
import com.dreb.ratelimit.ConstantRateRefillStrategy;
import com.dreb.ratelimit.Decision;
import com.dreb.ratelimit.DREBLimiterV2;
import com.dreb.ratelimit.InMemoryStateStore;
import com.dreb.ratelimit.ManualClock;

/**
 * Adapter wiring DREB v2 (abuse-adaptive) into the benchmark framework.
 */
public final class CustomRateLimiterV2 implements RateLimiter {

    private final DREBLimiterV2 dreb;
    private final ThreadLocalClock clock;
    private final InMemoryStateStore store;
    private final DrebParameters params;

    public CustomRateLimiterV2(RateLimiterConfig config) {
        this(config, DrebParameters.Strategy.V2_ADAPTIVE_BASE_1_0);
    }

    public CustomRateLimiterV2(RateLimiterConfig config, DrebParameters.Strategy strategy) {
        this(DrebParameters.fromConfig(config, strategy));
    }

    public CustomRateLimiterV2(DrebParameters params) {
        this.params = java.util.Objects.requireNonNull(params, "params must not be null");
        this.clock = new ThreadLocalClock(0.0);
        this.store = new InMemoryStateStore();

        ConstantRateRefillStrategy refillStrategy = params.createRefillStrategy();
        AdaptiveIdleCreditPolicy idlePolicy = (AdaptiveIdleCreditPolicy) params.createIdlePolicy();

        this.dreb = new DREBLimiterV2(store, clock, refillStrategy, idlePolicy,
                params.sustainedMax(), params.sustainedCostGain(),
                params.sustainedCostScalingEnabled(), false);
    }

    @Override
    public RateLimitResponse allow(RateLimitRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        int cost = (int) Math.min(Integer.MAX_VALUE, request.getCost());
        return allowDirect(request.getClientId(), request.getTimestampMs(), cost);
    }

    @Override
    public RateLimitResponse allowDirect(String clientId, long timestampMs, int cost) {
        return allow(clientId, cost, timestampMs / 1000.0);
    }

    /**
     * Thread-safe direct allow method passing explicit timestamp in seconds.
     */
    public RateLimitResponse allow(String clientId, int cost, double nowSeconds) {
        clock.set(nowSeconds);
        Decision decision = dreb.allow(clientId, cost);

        if (decision == Decision.ALLOW) {
            ClientState after = dreb.peekState(clientId);
            double effDecay = params.baseDecay() + (params.decayScalingEnabled() ? (params.abuseGain() * after.abuseScore) : 0.0);
            double effBurst = after.burstTokens + (effDecay > 0 ? (after.idleCredit / effDecay) : after.idleCredit);
            long remaining = (long) Math.floor(Math.min(after.sustainedTokens, effBurst));
            return new RateLimitResponse(true, remaining, -1, null);
        }

        ClientState state = dreb.peekState(clientId);
        String reason = state.sustainedTokens < cost
                ? "sustained_bucket_exhausted"
                : "burst_and_idle_credit_exhausted";
        return new RateLimitResponse(false, 0, -1, reason);
    }

    @Override
    public boolean allowEngineAlone(String clientId, long timestampMs, int cost) {
        clock.set(timestampMs / 1000.0);
        return dreb.allow(clientId, cost) == Decision.ALLOW;
    }

    @Override
    public String name() {
        if (params.strategy() == DrebParameters.Strategy.V2_ADAPTIVE_BASE_1_5) {
            return "Custom (v2-adaptive-base1.5)";
        }
        return "Custom (v2-adaptive)";
    }

    public DrebParameters getParameters() {
        return params;
    }

    public ClientState peekState(String clientId) {
        return dreb.peekState(clientId);
    }

    @Override
    public void reset() {
        store.clear();
    }

    @Override
    public void initializeState(String clientId, String initialState, double timestampSec) {
        if ("preWarmed".equalsIgnoreCase(initialState) || "pre-warmed".equalsIgnoreCase(initialState)) {
            ClientState s = new ClientState(params.burstMax(), params.sustainedMax(), params.idleCap(), 0.0, timestampSec);
            store.save(clientId, s);
        } else if ("drainedFirst".equalsIgnoreCase(initialState) || "drained-first".equalsIgnoreCase(initialState) || "drained".equalsIgnoreCase(initialState)) {
            ClientState s = new ClientState(0.0, 0.0, 0.0, 0.0, timestampSec);
            store.save(clientId, s);
        } else {
            // coldStart: full burst bucket, zero idle credit, zero abuse
            ClientState s = new ClientState(params.burstMax(), params.sustainedMax(), 0.0, 0.0, timestampSec);
            store.save(clientId, s);
        }
    }
}

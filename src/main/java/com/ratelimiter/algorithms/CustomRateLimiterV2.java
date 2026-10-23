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
    private final ManualClock clock;
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
        this.clock = new ManualClock(0.0);
        this.store = new InMemoryStateStore();

        ConstantRateRefillStrategy refillStrategy = params.createRefillStrategy();
        AdaptiveIdleCreditPolicy idlePolicy = (AdaptiveIdleCreditPolicy) params.createIdlePolicy();

        this.dreb = new DREBLimiterV2(store, clock, refillStrategy, idlePolicy,
                params.sustainedMax(), params.sustainedCostGain(),
                params.sustainedCostScalingEnabled(), false);
    }

    @Override
    public RateLimitResponse allow(RateLimitRequest request) {
        clock.set(request.getTimestampMs() / 1000.0);

        int cost = (int) Math.min(Integer.MAX_VALUE, request.getCost());
        Decision decision = dreb.allow(request.getClientId(), cost);

        if (decision == Decision.ALLOW) {
            ClientState after = dreb.peekState(request.getClientId());
            double effDecay = params.baseDecay() + (params.decayScalingEnabled() ? (params.abuseGain() * after.abuseScore) : 0.0);
            double effBurst = after.burstTokens + (effDecay > 0 ? (after.idleCredit / effDecay) : after.idleCredit);
            long remaining = (long) Math.floor(Math.min(after.sustainedTokens, effBurst));
            return new RateLimitResponse(true, remaining, -1, null);
        }

        ClientState state = dreb.peekState(request.getClientId());
        String reason = state.sustainedTokens < cost
                ? "sustained_bucket_exhausted"
                : "burst_and_idle_credit_exhausted";
        return new RateLimitResponse(false, 0, -1, reason);
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

package com.ratelimiter.algorithms;

import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiterConfig;

import com.dreb.ratelimit.ClientState;
import com.dreb.ratelimit.ConstantRateRefillStrategy;
import com.dreb.ratelimit.Decision;
import com.dreb.ratelimit.DREBLimiterV2;
import com.dreb.ratelimit.InMemoryStateStore;
import com.dreb.ratelimit.AdaptiveIdleCreditPolicy;
import com.dreb.ratelimit.ManualClock;

public final class CustomRateLimiterV2 implements RateLimiter {

    private final DREBLimiterV2 dreb;
    private final ManualClock clock;
    private final InMemoryStateStore store;

    public CustomRateLimiterV2(RateLimiterConfig config) {
        double capacity = config.getBucketCapacity();
        double refillRate = config.getRefillRate();

        // Use the burst_ceiling_matched base logic per instructions
        double burstMax      = capacity * 3.0 / 7.0;
        double idleCap       = capacity * 6.0 / 7.0;
        double sustainedMax  = capacity * 9.0 / 7.0;
        double sustainedRate = refillRate;
        double burstRate     = sustainedRate * 5.0 / 3.0;
        double idleGain      = sustainedRate * 2.0 / 3.0;

        // V2 specific parameters
        double baseDecay = 1.0;
        double abuseGain = 0.3;
        double abuseIncrement = 2.0;
        double abuseDecayRate = 1.5;
        double sustainedCostGain = 0.35;

        this.clock = new ManualClock(0.0);
        this.store = new InMemoryStateStore();
        
        ConstantRateRefillStrategy refillStrategy =
                new ConstantRateRefillStrategy(burstMax, burstRate, sustainedMax, sustainedRate);
                
        AdaptiveIdleCreditPolicy idlePolicy =
                new AdaptiveIdleCreditPolicy(idleGain, idleCap, baseDecay, abuseGain, abuseIncrement, abuseDecayRate);
                
        this.dreb = new DREBLimiterV2(store, clock, refillStrategy, idlePolicy, sustainedMax, sustainedCostGain);
    }

    @Override
    public RateLimitResponse allow(RateLimitRequest request) {
        clock.set(request.getTimestampMs() / 1000.0);

        int cost = (int) Math.min(Integer.MAX_VALUE, request.getCost());
        Decision decision = dreb.allow(request.getClientId(), cost);

        if (decision == Decision.ALLOW) {
            ClientState after = dreb.peekState(request.getClientId());
            long remaining = (long) Math.floor(
                    Math.min(after.sustainedTokens, after.burstTokens + after.idleCredit));
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
        return "Custom (v2-adaptive)";
    }

    @Override
    public void reset() {
    }
}

package com.ratelimiter.algorithms;

import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiterConfig;

import com.dreb.ratelimit.ClientState;
import com.dreb.ratelimit.ConstantRateRefillStrategy;
import com.dreb.ratelimit.Decision;
import com.dreb.ratelimit.DREBLimiter;
import com.dreb.ratelimit.InMemoryStateStore;
import com.dreb.ratelimit.LinearIdleCreditPolicy;
import com.dreb.ratelimit.ManualClock;

/**
 * Adapter wiring the DREB algorithm (Dual-Rate Elastic Bucket) into the
 * benchmark framework's {@link RateLimiter} contract.
 */
public final class CustomRateLimiter implements RateLimiter {

    /** Mapping strategy names. */
    public static final String PROPORTIONAL = "proportional";
    public static final String BURST_CEILING_MATCHED = "burst_ceiling_matched";

    private final DREBLimiter dreb;
    private final ThreadLocalClock clock;
    private final InMemoryStateStore store;
    private final String mappingStrategy;
    private final DrebParameters params;
    private final double rawBurstMax;
    private final double rawSustainedMax;
    private final double rawIdleCap;
    private final double rawIdleDecay;

    /**
     * Constructs a DREB limiter using the {@code proportional} mapping.
     */
    public CustomRateLimiter(RateLimiterConfig config) {
        this(config, PROPORTIONAL);
    }

    /**
     * Constructs a DREB limiter using the specified mapping strategy.
     *
     * @param config   the framework's generic rate-limiter config
     * @param strategy one of {@link #PROPORTIONAL} or {@link #BURST_CEILING_MATCHED}
     */
    public CustomRateLimiter(RateLimiterConfig config, String strategy) {
        this(config, strategy, false);
    }

    public CustomRateLimiter(RateLimiterConfig config, String strategy, boolean legacyOverdraw) {
        this.mappingStrategy = strategy;
        DrebParameters.Strategy strat = DrebParameters.Strategy.fromString(strategy);
        this.params = DrebParameters.fromConfig(config, strat);
        this.rawBurstMax = params.burstMax();
        this.rawSustainedMax = params.sustainedMax();
        this.rawIdleCap = params.idleCap();
        this.rawIdleDecay = params.idleDecay();

        this.clock = new ThreadLocalClock(0.0);
        this.store = new InMemoryStateStore();
        this.dreb = new DREBLimiter(store, clock, params.createRefillStrategy(), params.createIdlePolicy(), params.sustainedMax(), legacyOverdraw);
    }

    /**
     * Raw-parameter constructor for direct testing with the paper's exact
     * values or any other custom configuration.
     */
    public CustomRateLimiter(double burstMax, double burstRate,
                              double sustainedMax, double sustainedRate,
                              double idleGain, double idleCap, double idleDecay) {
        this("Custom", burstMax, burstRate, sustainedMax, sustainedRate, idleGain, idleCap, idleDecay);
    }

    /**
     * Named raw-parameter constructor for custom variants like DualBucket.
     */
    public CustomRateLimiter(String name, double burstMax, double burstRate,
                              double sustainedMax, double sustainedRate,
                              double idleGain, double idleCap, double idleDecay) {
        this.mappingStrategy = name;
        this.params = null;
        this.rawBurstMax = burstMax;
        this.rawSustainedMax = sustainedMax;
        this.rawIdleCap = idleCap;
        this.rawIdleDecay = idleDecay;
        this.clock = new ThreadLocalClock(0.0);
        this.store = new InMemoryStateStore();
        ConstantRateRefillStrategy refillStrategy =
                new ConstantRateRefillStrategy(burstMax, burstRate, sustainedMax, sustainedRate);
        LinearIdleCreditPolicy idlePolicy =
                new LinearIdleCreditPolicy(idleGain, idleCap, idleDecay);
        this.dreb = new DREBLimiter(store, clock, refillStrategy, idlePolicy, sustainedMax);
    }

    @Override
    public RateLimitResponse allow(RateLimitRequest request) {
        int cost = (int) Math.min(Integer.MAX_VALUE, request.getCost());
        return allow(request.getClientId(), cost, request.getTimestampMs() / 1000.0);
    }

    /**
     * Thread-safe direct allow method passing explicit timestamp in seconds.
     */
    public RateLimitResponse allow(String clientId, int cost, double nowSeconds) {
        clock.set(nowSeconds);
        Decision decision = dreb.allow(clientId, cost);

        if (decision == Decision.ALLOW) {
            ClientState after = dreb.peekState(clientId);
            double effDecay = (params != null) ? params.idleDecay() : rawIdleDecay;
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
        if (BURST_CEILING_MATCHED.equals(mappingStrategy)) {
            return "Custom (burst-matched)";
        }
        if (PROPORTIONAL.equals(mappingStrategy)) {
            return "Custom (proportional)";
        }
        return mappingStrategy != null ? mappingStrategy : "Custom";
    }

    /** Returns the mapping strategy used by this instance. */
    public String getMappingStrategy() {
        return mappingStrategy;
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
        double bMax = (params != null) ? params.burstMax() : rawBurstMax;
        double sMax = (params != null) ? params.sustainedMax() : rawSustainedMax;
        double idCap = (params != null) ? params.idleCap() : rawIdleCap;

        if ("preWarmed".equalsIgnoreCase(initialState) || "pre-warmed".equalsIgnoreCase(initialState)) {
            ClientState s = new ClientState(bMax, sMax, idCap, timestampSec);
            store.save(clientId, s);
        } else if ("drainedFirst".equalsIgnoreCase(initialState) || "drained-first".equalsIgnoreCase(initialState) || "drained".equalsIgnoreCase(initialState)) {
            ClientState s = new ClientState(0.0, 0.0, 0.0, timestampSec);
            store.save(clientId, s);
        } else {
            // coldStart: full burst bucket, zero idle credit
            ClientState s = new ClientState(bMax, sMax, 0.0, timestampSec);
            store.save(clientId, s);
        }
    }
}

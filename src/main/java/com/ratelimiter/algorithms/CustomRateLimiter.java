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
 *
 * <p>Two config-to-DREB parameter mappings are supported, selectable via
 * {@code drebMappingStrategy} in the YAML config:
 *
 * <h3>{@code proportional} (default)</h3>
 * Anchors {@code sustainedMax = capacity}. The paper's original ratios
 * scale every other parameter from there. This means DREB's effective
 * instantaneous burst ceiling ({@code burstMax + idleCap/idleDecay}) is
 * strictly less than {@code capacity}, so DREB will always accept fewer
 * burst requests than a standard Token Bucket with the same capacity.
 *
 * <h3>{@code burst_ceiling_matched}</h3>
 * Anchors the decay-adjusted effective burst ceiling to {@code capacity}:
 * {@code burstMax + idleCap/idleDecay = capacity}. This means after a
 * full idle period, DREB can absorb the same total burst as a Token
 * Bucket with the same capacity. {@code sustainedMax} is intentionally
 * set above capacity (128.6% of it) to preserve the paper's ratios.
 *
 * <p>Both mappings exist because "same rate limit" is ambiguous for a
 * dual-bucket design: does it mean the same sustained ceiling (proportional)
 * or the same maximum instantaneous burst capacity (burst_ceiling_matched)?
 * Reporting results under both makes that sensitivity visible rather
 * than hidden behind a single arbitrary choice.
 */
public final class CustomRateLimiter implements RateLimiter {

    /** Mapping strategy names. */
    public static final String PROPORTIONAL = "proportional";
    public static final String BURST_CEILING_MATCHED = "burst_ceiling_matched";

    private final DREBLimiter dreb;
    private final ManualClock clock;
    private final InMemoryStateStore store;
    private final String mappingStrategy;

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
        this.mappingStrategy = strategy;

        double capacity = config.getBucketCapacity();
        double refillRate = config.getRefillRate();

        double burstMax, burstRate, sustainedMax, sustainedRate, idleGain, idleCap, idleDecay;

        if (BURST_CEILING_MATCHED.equals(strategy)) {
            // Anchor: burstMax + idleCap/idleDecay = capacity
            burstMax      = capacity * 3.0 / 7.0;
            idleCap       = capacity * 6.0 / 7.0;    // = 2 * burstMax
            sustainedMax  = capacity * 9.0 / 7.0;    // = 3 * burstMax
            sustainedRate = refillRate;
            burstRate     = sustainedRate * 5.0 / 3.0;
            idleGain      = sustainedRate * 2.0 / 3.0;
            idleDecay     = 1.5;
        } else {
            // "proportional" — anchor: sustainedMax = capacity
            sustainedMax  = capacity;
            sustainedRate = refillRate;
            burstMax      = sustainedMax / 3.0;
            burstRate     = sustainedRate * 5.0 / 3.0;
            idleGain      = sustainedRate * 2.0 / 3.0;
            idleCap       = sustainedMax * 2.0 / 3.0;
            idleDecay     = 1.5;
        }

        this.clock = new ManualClock(0.0);
        this.store = new InMemoryStateStore();
        ConstantRateRefillStrategy refillStrategy =
                new ConstantRateRefillStrategy(burstMax, burstRate, sustainedMax, sustainedRate);
        LinearIdleCreditPolicy idlePolicy =
                new LinearIdleCreditPolicy(idleGain, idleCap, idleDecay);
        this.dreb = new DREBLimiter(store, clock, refillStrategy, idlePolicy, sustainedMax);
    }

    /**
     * Raw-parameter constructor for direct testing with the paper's exact
     * values or any other custom configuration.
     */
    public CustomRateLimiter(double burstMax, double burstRate,
                              double sustainedMax, double sustainedRate,
                              double idleGain, double idleCap, double idleDecay) {
        this.mappingStrategy = "raw";
        this.clock = new ManualClock(0.0);
        this.store = new InMemoryStateStore();
        ConstantRateRefillStrategy refillStrategy =
                new ConstantRateRefillStrategy(burstMax, burstRate, sustainedMax, sustainedRate);
        LinearIdleCreditPolicy idlePolicy =
                new LinearIdleCreditPolicy(idleGain, idleCap, idleDecay);
        this.dreb = new DREBLimiter(store, clock, refillStrategy, idlePolicy, sustainedMax);
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
        if (BURST_CEILING_MATCHED.equals(mappingStrategy)) {
            return "Custom (burst-matched)";
        }
        if (PROPORTIONAL.equals(mappingStrategy)) {
            return "Custom (proportional)";
        }
        return "Custom";
    }

    /** Returns the mapping strategy used by this instance. */
    public String getMappingStrategy() {
        return mappingStrategy;
    }

    @Override
    public void reset() {
        // The benchmark creates a new instance per run via the Supplier
        // in the registry, so state is effectively fresh each time.
    }
}

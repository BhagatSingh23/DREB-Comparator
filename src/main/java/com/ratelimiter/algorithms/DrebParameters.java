package com.ratelimiter.algorithms;

import com.dreb.ratelimit.AdaptiveIdleCreditPolicy;
import com.dreb.ratelimit.ConstantRateRefillStrategy;
import com.dreb.ratelimit.IdleCreditPolicy;
import com.dreb.ratelimit.LinearIdleCreditPolicy;
import com.ratelimiter.core.RateLimiterConfig;

import java.util.Objects;

/**
 * Single source of truth mapping externally meaningful (capacity C, refill rate R, strategy)
 * inputs to all internal DREB parameters for v1 and v2.
 *
 * <p>Replaces all scattered magic fractions in {@link CustomRateLimiter} and {@link CustomRateLimiterV2}.
 */
public final class DrebParameters {

    public enum Strategy {
        /**
         * v1 Proportional mapping:
         * S_max = C, B_max = C/3, idle_cap = 2C/3, idle_decay = 1.5.
         * Instantaneous unit-cost burst ceiling = 7C/9 ≈ 0.778C.
         */
        PROPORTIONAL("proportional", false),

        /**
         * v1 Burst-ceiling-matched mapping:
         * S_max = 9C/7, B_max = 3C/7, idle_cap = 6C/7, idle_decay = 1.5.
         * Instantaneous unit-cost burst ceiling = B_max + idle_cap/idle_decay = C.
         */
        BURST_CEILING_MATCHED("burst_ceiling_matched", false),

        /**
         * v2 Adaptive with baseDecay = 1.0 (prototype baseline):
         * Uses burst_ceiling_matched bucket capacities, but baseDecay = 1.0 so a client
         * bursting after genuine idleness pays no extra penalty multiplier on idle credit.
         */
        V2_ADAPTIVE_BASE_1_0("v2_adaptive_base_1_0", true),

        /**
         * v2 Adaptive with baseDecay = 1.5 (strict burst-matched base policy):
         * Exactly matches v1's burst-matched idle_decay (1.5) at abuseScore = 0.
         */
        V2_ADAPTIVE_BASE_1_5("v2_adaptive_base_1_5", true);

        private final String configName;
        private final boolean v2;

        Strategy(String configName, boolean v2) {
            this.configName = configName;
            this.v2 = v2;
        }

        public String configName() {
            return configName;
        }

        public boolean isV2() {
            return v2;
        }

        public static Strategy fromString(String name) {
            if (name == null || name.isBlank()) {
                return PROPORTIONAL;
            }
            String norm = name.trim().toLowerCase();
            return switch (norm) {
                case "proportional" -> PROPORTIONAL;
                case "burst_ceiling_matched", "burst-matched", "burst_matched" -> BURST_CEILING_MATCHED;
                case "v2_adaptive", "v2-adaptive", "v2", "v2_adaptive_base_1_0" -> V2_ADAPTIVE_BASE_1_0;
                case "v2_adaptive_base_1_5", "v2_base_1_5", "v2_strict" -> V2_ADAPTIVE_BASE_1_5;
                case "both", "all" -> PROPORTIONAL;
                default -> throw new IllegalArgumentException("Unknown DREB mapping strategy: " + name);
            };
        }
    }

    private final Strategy strategy;
    private final double capacity;
    private final double refillRate;

    // Derived bucket capacities & rates
    private final double burstMax;
    private final double burstRate;
    private final double sustainedMax;
    private final double sustainedRate;
    private final double idleGain;
    private final double idleCap;
    private final double idleDecay; // effective base decay

    // v2 Adaptive parameters
    private final double baseDecay;
    private final double abuseGain;
    private final double abuseIncrement;
    private final double abuseDecayRate;
    private final double sustainedCostGain;
    private final boolean decayScalingEnabled;
    private final boolean sustainedCostScalingEnabled;

    private DrebParameters(Strategy strategy, double capacity, double refillRate,
                           double baseDecay, double abuseGain, double abuseIncrement,
                           double abuseDecayRate, double sustainedCostGain,
                           boolean decayScalingEnabled, boolean sustainedCostScalingEnabled) {
        if (capacity <= 0.0) {
            throw new IllegalArgumentException("Capacity C must be > 0, got: " + capacity);
        }
        if (refillRate <= 0.0) {
            throw new IllegalArgumentException("Refill rate R must be > 0, got: " + refillRate);
        }
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
        this.capacity = capacity;
        this.refillRate = refillRate;

        this.sustainedRate = refillRate;
        this.burstRate = refillRate * 5.0 / 3.0;
        this.idleGain = refillRate * 2.0 / 3.0;

        if (strategy == Strategy.PROPORTIONAL) {
            this.sustainedMax = capacity;
            this.burstMax = capacity / 3.0;
            this.idleCap = capacity * 2.0 / 3.0;
            this.idleDecay = 1.5;
            this.baseDecay = 1.5;
        } else {
            // BURST_CEILING_MATCHED, V2_ADAPTIVE_BASE_1_0, V2_ADAPTIVE_BASE_1_5
            this.burstMax = capacity * 3.0 / 7.0;
            this.idleCap = capacity * 6.0 / 7.0;
            this.sustainedMax = capacity * 9.0 / 7.0;
            this.idleDecay = baseDecay;
            this.baseDecay = baseDecay;
        }

        this.abuseGain = abuseGain;
        this.abuseIncrement = abuseIncrement;
        this.abuseDecayRate = abuseDecayRate;
        this.sustainedCostGain = sustainedCostGain;
        this.decayScalingEnabled = decayScalingEnabled;
        this.sustainedCostScalingEnabled = sustainedCostScalingEnabled;
    }

    public static DrebParameters forProportional(double capacity, double refillRate) {
        return new DrebParameters(Strategy.PROPORTIONAL, capacity, refillRate, 1.5, 0, 0, 0, 0, false, false);
    }

    public static DrebParameters forBurstCeilingMatched(double capacity, double refillRate) {
        return new DrebParameters(Strategy.BURST_CEILING_MATCHED, capacity, refillRate, 1.5, 0, 0, 0, 0, false, false);
    }

    public static DrebParameters forV2Base10(double capacity, double refillRate) {
        return new DrebParameters(Strategy.V2_ADAPTIVE_BASE_1_0, capacity, refillRate,
                1.0, 0.3, 2.0, 1.5, 0.35, true, true);
    }

    public static DrebParameters forV2Base15(double capacity, double refillRate) {
        return new DrebParameters(Strategy.V2_ADAPTIVE_BASE_1_5, capacity, refillRate,
                1.5, 0.3, 2.0, 1.5, 0.35, true, true);
    }

    public static DrebParameters fromStrategy(Strategy strategy, double capacity, double refillRate) {
        return switch (strategy) {
            case PROPORTIONAL -> forProportional(capacity, refillRate);
            case BURST_CEILING_MATCHED -> forBurstCeilingMatched(capacity, refillRate);
            case V2_ADAPTIVE_BASE_1_0 -> forV2Base10(capacity, refillRate);
            case V2_ADAPTIVE_BASE_1_5 -> forV2Base15(capacity, refillRate);
        };
    }

    public static DrebParameters fromConfig(RateLimiterConfig config, Strategy strategy) {
        double c = config.getBucketCapacity();
        double r = config.getRefillRate();
        return fromStrategy(strategy, c, r);
    }

    public ConstantRateRefillStrategy createRefillStrategy() {
        return new ConstantRateRefillStrategy(burstMax, burstRate, sustainedMax, sustainedRate);
    }

    public IdleCreditPolicy createIdlePolicy() {
        if (strategy.isV2()) {
            return new AdaptiveIdleCreditPolicy(idleGain, idleCap, baseDecay,
                    abuseGain, abuseIncrement, abuseDecayRate, decayScalingEnabled);
        }
        return new LinearIdleCreditPolicy(idleGain, idleCap, idleDecay);
    }

    /** Maximum unit-cost burst requests accepted from full state. */
    public double maxInstantaneousBurst() {
        return burstMax + (idleCap / idleDecay);
    }

    /** Cumulative admission bound over interval T: S_max + S_rate * T. */
    public double cumulativeAdmissionBound(double durationT) {
        return sustainedMax + sustainedRate * durationT;
    }

    public Strategy strategy() { return strategy; }
    public double capacity() { return capacity; }
    public double refillRate() { return refillRate; }
    public double burstMax() { return burstMax; }
    public double burstRate() { return burstRate; }
    public double sustainedMax() { return sustainedMax; }
    public double sustainedRate() { return sustainedRate; }
    public double idleGain() { return idleGain; }
    public double idleCap() { return idleCap; }
    public double idleDecay() { return idleDecay; }
    public double baseDecay() { return baseDecay; }
    public double abuseGain() { return abuseGain; }
    public double abuseIncrement() { return abuseIncrement; }
    public double abuseDecayRate() { return abuseDecayRate; }
    public double sustainedCostGain() { return sustainedCostGain; }
    public boolean decayScalingEnabled() { return decayScalingEnabled; }
    public boolean sustainedCostScalingEnabled() { return sustainedCostScalingEnabled; }
}

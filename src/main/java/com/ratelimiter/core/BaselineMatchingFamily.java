package com.ratelimiter.core;

import com.ratelimiter.algorithms.DrebParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the three explicit baseline-matching comparison families defined in Section 6.3:
 * 1. EQUAL_CAPACITY_AND_RATE: equal nominal capacity C and sustained rate R
 * 2. EQUAL_INSTANTANEOUS_BURST: matched unit-cost instantaneous burst ceiling
 * 3. EQUAL_CUMULATIVE_ENVELOPE: matched cumulative admission bound (S_max + R * T)
 */
public enum BaselineMatchingFamily {

    EQUAL_CAPACITY_AND_RATE(
            "equal_capacity_and_rate",
            "Equal Nominal Capacity C and Sustained Refill Rate R"
    ),

    EQUAL_INSTANTANEOUS_BURST(
            "equal_instantaneous_burst",
            "Equal Maximum Instantaneous Burst (Unit Cost)"
    ),

    EQUAL_CUMULATIVE_ENVELOPE(
            "equal_cumulative_envelope",
            "Equal Cumulative Admission Envelope S_max + R * T"
    );

    private final String id;
    private final String description;

    BaselineMatchingFamily(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String description() {
        return description;
    }

    public record AlgorithmParameters(
            String algorithmName,
            double configuredCapacity,
            double configuredRate,
            double sustainedMax,
            double sustainedRate,
            double burstMax,
            double burstRate,
            double idleCap,
            double idleDecay,
            double coldStartMaxBurst,
            double fullCreditMaxBurst,
            double cumulativeBoundT30
    ) {}

    public static List<AlgorithmParameters> deriveParameters(BaselineMatchingFamily family, double targetC, double targetR) {
        List<AlgorithmParameters> params = new ArrayList<>();
        double durationT = 30.0;

        // 1. TB-large (Standard Token Bucket)
        double tbLargeCapacity = targetC;
        params.add(new AlgorithmParameters(
                "Token Bucket (TB-large)", tbLargeCapacity, targetR,
                tbLargeCapacity, targetR, tbLargeCapacity, targetR, 0.0, 1.0,
                tbLargeCapacity, tbLargeCapacity, tbLargeCapacity + targetR * durationT
        ));

        // 2. TB-small (Token Bucket matched to headline DREB burstMax = 3C/7)
        double tbSmallCapacity = targetC * 3.0 / 7.0;
        params.add(new AlgorithmParameters(
                "Token Bucket (TB-small)", tbSmallCapacity, targetR,
                tbSmallCapacity, targetR, tbSmallCapacity, targetR, 0.0, 1.0,
                tbSmallCapacity, tbSmallCapacity, tbSmallCapacity + targetR * durationT
        ));

        // 3. Leaky Bucket
        params.add(new AlgorithmParameters(
                "Leaky Bucket", targetC, targetR,
                targetC, targetR, 0.0, targetR, 0.0, 1.0,
                0.0, 0.0, targetC + targetR * durationT
        ));

        // 4. Fixed Window
        params.add(new AlgorithmParameters(
                "Fixed Window", targetC, targetR,
                targetC, targetR, targetC, targetR, 0.0, 1.0,
                targetC, targetC, targetC + targetR * durationT
        ));

        // 5. Sliding Window Log
        params.add(new AlgorithmParameters(
                "Sliding Window Log", targetC, targetR,
                targetC, targetR, targetC, targetR, 0.0, 1.0,
                targetC, targetC, targetC + targetR * durationT
        ));

        // 6. Sliding Window Counter
        params.add(new AlgorithmParameters(
                "Sliding Window Counter", targetC, targetR,
                targetC, targetR, targetC, targetR, 0.0, 1.0,
                targetC, targetC, targetC + targetR * durationT
        ));

        // 7. DREB Proportional
        double propInputC = switch (family) {
            case EQUAL_CAPACITY_AND_RATE, EQUAL_CUMULATIVE_ENVELOPE -> targetC;
            case EQUAL_INSTANTANEOUS_BURST -> targetC * 9.0 / 7.0; // scale up so full-credit burst 7C'/9 == targetC
        };
        DrebParameters prop = DrebParameters.forProportional(propInputC, targetR);
        params.add(new AlgorithmParameters(
                "DREB (proportional)", propInputC, targetR,
                prop.sustainedMax(), prop.sustainedRate(),
                prop.burstMax(), prop.burstRate(),
                prop.idleCap(), prop.idleDecay(),
                prop.burstMax(), prop.maxInstantaneousBurst(), prop.cumulativeAdmissionBound(durationT)
        ));

        // 8. DREB Burst-Matched
        double bmInputC = switch (family) {
            case EQUAL_CAPACITY_AND_RATE, EQUAL_INSTANTANEOUS_BURST -> targetC;
            case EQUAL_CUMULATIVE_ENVELOPE -> targetC * 7.0 / 9.0; // scale down so S_max == targetC
        };
        DrebParameters bm = DrebParameters.forBurstCeilingMatched(bmInputC, targetR);
        params.add(new AlgorithmParameters(
                "DREB (burst-matched)", bmInputC, targetR,
                bm.sustainedMax(), bm.sustainedRate(),
                bm.burstMax(), bm.burstRate(),
                bm.idleCap(), bm.idleDecay(),
                bm.burstMax(), bm.maxInstantaneousBurst(), bm.cumulativeAdmissionBound(durationT)
        ));

        // 9. DREB v2 Headline: Adaptive Base 1.5 (Full-credit max burst = C)
        double v2StrictInputC = bmInputC;
        DrebParameters v2Strict = DrebParameters.forV2Base15(v2StrictInputC, targetR);
        params.add(new AlgorithmParameters(
                "DREB (v2-adaptive-base1.5)", v2StrictInputC, targetR,
                v2Strict.sustainedMax(), v2Strict.sustainedRate(),
                v2Strict.burstMax(), v2Strict.burstRate(),
                v2Strict.idleCap(), v2Strict.baseDecay(),
                v2Strict.burstMax(), v2Strict.maxInstantaneousBurst(), v2Strict.cumulativeAdmissionBound(durationT)
        ));

        // 10. DREB v2 Variant: Adaptive Base 1.0
        double v2Base10InputC = switch (family) {
            case EQUAL_CAPACITY_AND_RATE -> targetC;
            case EQUAL_INSTANTANEOUS_BURST -> targetC * 7.0 / 9.0; // scale C' so full-credit burst (9C'/7) == targetC
            case EQUAL_CUMULATIVE_ENVELOPE -> targetC * 7.0 / 9.0;
        };
        DrebParameters v2 = DrebParameters.forV2Base10(v2Base10InputC, targetR);
        params.add(new AlgorithmParameters(
                "DREB (v2-adaptive-base1.0)", v2Base10InputC, targetR,
                v2.sustainedMax(), v2.sustainedRate(),
                v2.burstMax(), v2.burstRate(),
                v2.idleCap(), v2.baseDecay(),
                v2.burstMax(), v2.maxInstantaneousBurst(), v2.cumulativeAdmissionBound(durationT)
        ));

        return params;
    }

    public static String formatParametersTable(BaselineMatchingFamily family, double c, double r) {
        List<AlgorithmParameters> list = deriveParameters(family, c, r);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("### Family: %s\n", family.description()));
        sb.append(String.format("Target: Capacity C = %.1f, Refill Rate R = %.1f req/s\n\n", c, r));
        sb.append("| Algorithm | Configured C | Sustained Max | Burst Max | Idle Cap | Idle Decay | Cold-start Max Burst | Full-credit Max Burst | Bound (T=30s) |\n");
        sb.append("|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|\n");
        for (AlgorithmParameters p : list) {
            sb.append(String.format("| %s | %.1f | %.1f | %.1f | %.1f | %.2f | %.1f | %.1f | %.1f |\n",
                    p.algorithmName(), p.configuredCapacity(), p.sustainedMax(),
                    p.burstMax(), p.idleCap(), p.idleDecay(),
                    p.coldStartMaxBurst(), p.fullCreditMaxBurst(), p.cumulativeBoundT30()));
        }
        sb.append("\n");
        return sb.toString();
    }

    public static void main(String[] args) {
        double c = 100.0;
        double r = 100.0;
        for (BaselineMatchingFamily family : values()) {
            System.out.println(formatParametersTable(family, c, r));
        }
    }
}

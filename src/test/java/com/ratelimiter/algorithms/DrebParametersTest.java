package com.ratelimiter.algorithms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DrebParameters Specification & Identity Tests")
public class DrebParametersTest {

    @ParameterizedTest
    @ValueSource(doubles = {10.0, 70.0, 100.0, 1000.0, 50000.0})
    @DisplayName("Paper Identity: sustainedMax == C and sustainedRate == R for proportional mapping")
    void testProportionalIdentities(double capacity) {
        double refillRate = 100.0;
        DrebParameters p = DrebParameters.forProportional(capacity, refillRate);

        // Paper Table 3 Identity 1: sustainedMax == C
        assertEquals(capacity, p.sustainedMax(), 1e-9, "sustainedMax must equal C");
        // Paper Table 3 Identity 2: sustainedRate == R
        assertEquals(refillRate, p.sustainedRate(), 1e-9, "sustainedRate must equal R");
        // B_max = C / 3
        assertEquals(capacity / 3.0, p.burstMax(), 1e-9, "burstMax must equal C/3");
        // idleCap = 2C / 3
        assertEquals(capacity * 2.0 / 3.0, p.idleCap(), 1e-9, "idleCap must equal 2C/3");
        // idleDecay = 1.5
        assertEquals(1.5, p.idleDecay(), 1e-9, "idleDecay must be 1.5");
        // Max unit-cost requests accepted instantly from full state = 7C/9
        assertEquals(7.0 * capacity / 9.0, p.maxInstantaneousBurst(), 1e-9, "Instantaneous burst should be 7C/9");
    }

    @ParameterizedTest
    @ValueSource(doubles = {14.0, 70.0, 140.0, 1000.0, 50000.0})
    @DisplayName("Paper Identity: burstMax + idleCap/idleDecay == C for burst-matched mapping")
    void testBurstMatchedIdentities(double capacity) {
        double refillRate = 50.0;
        DrebParameters p = DrebParameters.forBurstCeilingMatched(capacity, refillRate);

        // Paper Table 3 Identity 1: burstMax + idleCap/idleDecay == C
        double effectiveBurstCeiling = p.burstMax() + (p.idleCap() / p.idleDecay());
        assertEquals(capacity, effectiveBurstCeiling, 1e-9, "burstMax + idleCap/idleDecay must equal C");
        // Paper Table 3 Identity 2: sustainedMax == 9C/7
        assertEquals(9.0 * capacity / 7.0, p.sustainedMax(), 1e-9, "sustainedMax must equal 9C/7");
        // Paper Table 3 Identity 3: sustainedRate == R
        assertEquals(refillRate, p.sustainedRate(), 1e-9, "sustainedRate must equal R");
        // B_max = 3C/7
        assertEquals(3.0 * capacity / 7.0, p.burstMax(), 1e-9, "burstMax must equal 3C/7");
        // idleCap = 6C/7 = 2*B_max
        assertEquals(6.0 * capacity / 7.0, p.idleCap(), 1e-9, "idleCap must equal 6C/7");
        assertEquals(2.0 * p.burstMax(), p.idleCap(), 1e-9, "idleCap must equal 2 * burstMax");
        // S_max = 3*B_max
        assertEquals(3.0 * p.burstMax(), p.sustainedMax(), 1e-9, "sustainedMax must equal 3 * burstMax");
    }

    @Test
    @DisplayName("v2 Named Configurations: baseDecay=1.0 (prototype) vs baseDecay=1.5 (strict burst-matched)")
    void testV2NamedConfigurations() {
        double capacity = 70.0;
        double refillRate = 10.0;

        DrebParameters v2Base10 = DrebParameters.forV2Base10(capacity, refillRate);
        assertEquals(1.0, v2Base10.baseDecay(), 1e-9);
        assertEquals(0.3, v2Base10.abuseGain(), 1e-9);
        assertEquals(2.0, v2Base10.abuseIncrement(), 1e-9);
        assertEquals(1.5, v2Base10.abuseDecayRate(), 1e-9);
        assertEquals(0.35, v2Base10.sustainedCostGain(), 1e-9);

        DrebParameters v2Base15 = DrebParameters.forV2Base15(capacity, refillRate);
        assertEquals(1.5, v2Base15.baseDecay(), 1e-9);
        assertEquals(0.3, v2Base15.abuseGain(), 1e-9);
        assertEquals(2.0, v2Base15.abuseIncrement(), 1e-9);
        assertEquals(1.5, v2Base15.abuseDecayRate(), 1e-9);
        assertEquals(0.35, v2Base15.sustainedCostGain(), 1e-9);

        // At baseDecay=1.5, the zero-abuse burst capacity matches v1 burst-matched
        assertEquals(capacity, v2Base15.maxInstantaneousBurst(), 1e-9);
    }
}

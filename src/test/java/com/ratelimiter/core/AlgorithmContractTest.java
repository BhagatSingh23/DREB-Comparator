package com.ratelimiter.core;

import com.ratelimiter.algorithms.DrebParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Algorithm Contract Violation Predicate Tests")
public class AlgorithmContractTest {

    @Test
    @DisplayName("Token Bucket Contract: zero violations on conforming stream, catches burst violation")
    void testTokenBucketContract() {
        AlgorithmContract.TokenBucketContract contract = new AlgorithmContract.TokenBucketContract(100.0, 10.0);

        // Conforming: 50 tokens at t=0, then 1 token every 100ms (10 req/s)
        List<AlgorithmContract.RequestEvent> conforming = new ArrayList<>();
        conforming.add(new AlgorithmContract.RequestEvent(0, 50, true));
        for (int i = 1; i <= 20; i++) {
            conforming.add(new AlgorithmContract.RequestEvent(i * 100, 1, true));
        }
        assertEquals(0, contract.evaluateViolations(conforming));

        // Violating: instantaneous burst of 150 > capacity (100)
        List<AlgorithmContract.RequestEvent> violatingBurst = List.of(
                new AlgorithmContract.RequestEvent(0, 80, true),
                new AlgorithmContract.RequestEvent(0, 30, true) // sum=110 > 100
        );
        assertTrue(contract.evaluateViolations(violatingBurst) > 0);

        // Violating: sustained over-consumption (refilling at 10 tokens/s, consuming 200 tokens over 2s with cap 100)
        List<AlgorithmContract.RequestEvent> violatingEnvelope = new ArrayList<>();
        violatingEnvelope.add(new AlgorithmContract.RequestEvent(0, 100, true));
        for (int i = 1; i <= 100; i++) {
            violatingEnvelope.add(new AlgorithmContract.RequestEvent(i * 20, 1, true)); // 50 req/s
        }
        assertTrue(contract.evaluateViolations(violatingEnvelope) > 0);
    }

    @Test
    @DisplayName("Fixed Window Contract: zero violations on conforming, catches window boundary violation")
    void testFixedWindowContract() {
        AlgorithmContract.FixedWindowContract contract = new AlgorithmContract.FixedWindowContract(50, 1000);

        List<AlgorithmContract.RequestEvent> conforming = List.of(
                new AlgorithmContract.RequestEvent(100, 25, true),
                new AlgorithmContract.RequestEvent(900, 25, true),
                new AlgorithmContract.RequestEvent(1100, 50, true) // new window [1000, 2000)
        );
        assertEquals(0, contract.evaluateViolations(conforming));

        List<AlgorithmContract.RequestEvent> violating = List.of(
                new AlgorithmContract.RequestEvent(100, 30, true),
                new AlgorithmContract.RequestEvent(500, 30, true) // sum=60 in window 0 > 50
        );
        assertEquals(1, contract.evaluateViolations(violating));
    }

    @Test
    @DisplayName("Sliding Window Log Contract: catches sliding window violation")
    void testSlidingWindowLogContract() {
        AlgorithmContract.SlidingWindowLogContract contract = new AlgorithmContract.SlidingWindowLogContract(50, 1000);

        // Conforming: spread across time
        List<AlgorithmContract.RequestEvent> conforming = List.of(
                new AlgorithmContract.RequestEvent(0, 25, true),
                new AlgorithmContract.RequestEvent(1100, 25, true)
        );
        assertEquals(0, contract.evaluateViolations(conforming));

        // Violating: 30 at t=800 and 30 at t=1200 -> in sliding window [201, 1200], sum is 60 > 50
        List<AlgorithmContract.RequestEvent> violating = List.of(
                new AlgorithmContract.RequestEvent(800, 30, true),
                new AlgorithmContract.RequestEvent(1200, 30, true)
        );
        assertEquals(1, contract.evaluateViolations(violating));
    }

    @Test
    @DisplayName("DREB Contract: respects burst ceiling and sustained envelope")
    void testDrebContract() {
        DrebParameters params = DrebParameters.forBurstCeilingMatched(70.0, 10.0);
        AlgorithmContract.DrebContract contract = new AlgorithmContract.DrebContract(params);

        // Burst-matched C=70: burst ceiling = 70, sustainedMax = 90
        List<AlgorithmContract.RequestEvent> conforming = List.of(
                new AlgorithmContract.RequestEvent(0, 70, true),
                new AlgorithmContract.RequestEvent(1000, 10, true),
                new AlgorithmContract.RequestEvent(2000, 10, true)
        );
        assertEquals(0, contract.evaluateViolations(conforming));

        // Violating: instantaneous burst of 75 > burst ceiling 70
        List<AlgorithmContract.RequestEvent> violatingBurst = List.of(
                new AlgorithmContract.RequestEvent(0, 75, true)
        );
        assertTrue(contract.evaluateViolations(violatingBurst) > 0);
    }
}

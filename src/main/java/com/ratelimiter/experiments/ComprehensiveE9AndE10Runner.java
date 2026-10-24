package com.ratelimiter.experiments;

import com.ratelimiter.algorithms.CustomRateLimiter;
import com.ratelimiter.algorithms.TokenBucket;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

public class ComprehensiveE9AndE10Runner {

    private static final double CAPACITY = 1000.0;
    private static final double REFILL_RATE = 1000.0;
    private static final double DURATION_SEC = 30.0;
    private static final int TOTAL_CLIENTS = 200;
    private static final double F_ABUSIVE = 0.30; // 60 abusers, 140 legit (70 steady, 70 bursty)
    private static final int NUM_SEEDS = 30;
    private static final long SEED_START = 2000L;

    private static final double[] K_VALUES = {1.10, 1.25, 1.50, 2.00, 4.00};
    private static final double[] ABUSER_BURSTS = {0.30, 0.50, 0.70, 0.95, 1.20};
    private static final String[] INITIAL_STATES = {"coldStart", "preWarmed"};
    private static final double[] WINDOWS_SEC = {0.05, 0.10, 0.25}; // 50ms, 100ms, 250ms

    // S1: 15 B values x 5 Peak rates = 75 policies
    private static final double[] S1_B_FRACS = {
            0.30, 0.35, 0.40, 0.45, 0.50, 0.55, 0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.00
    };
    private static final double[] S1_P_FACTORS = {1.0, 1.25, 5.0 / 3.0, 2.5, 4.0};

    // S2: 7 Capacities at refill R
    private static final double[] S2_CAP_FRACS = {0.30, 3.0 / 7.0, 0.55, 0.70, 0.85, 1.00, 9.0 / 7.0};

    // S3: Capacity 3C/7 with 3 Refill rates
    private static final double[] S3_REFILL_FACTORS = {1.0, 1.1, 1.25};

    public record PolicyDef(
            String id,
            String displayName,
            String family, // "S1", "S2", "S3", "Dynamic"
            double bFrac,
            double pFactor,
            double capFrac,
            double refillFactor
    ) {}

    public record ScenarioConfig(
            double capacity,
            String initialState,
            double k,
            double abuserBurstFrac
    ) {}

    public record RequestEvent(long tMs, int cost) {}

    public record PreGeneratedTrace(
            List<List<RequestEvent>> abuserTraces,
            List<List<RequestEvent>> steadyTraces,
            List<List<RequestEvent>> burstyTraces
    ) {}

    public record TrialResult(
            String policyId,
            long seed,
            double abuserAdmittedRateNorm,
            double burstyFrr,
            double[] peakLoads, // peakLoads[0]=W_50ms, [1]=W_100ms, [2]=W_250ms (full duration)
            double[] peakLoadsFirst5s, // windows starting at t < 5.0s
            double[] peakLoadsSteady // windows starting at t >= 5.0s
    ) {}

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================================");
        System.out.println("  STARTING COMPREHENSIVE E9 SWEEP & E10 (H10) SHORT-WINDOW ANALYSIS");
        System.out.println("===============================================================================");

        List<PolicyDef> policies = buildPolicies();
        System.out.printf("Defined %d policies (85 Static + 1 DREB BM).%n", policies.size());

        List<ScenarioConfig> scenarios = new ArrayList<>();
        for (String init : INITIAL_STATES) {
            for (double k : K_VALUES) {
                for (double bFrac : ABUSER_BURSTS) {
                    scenarios.add(new ScenarioConfig(CAPACITY, init, k, bFrac));
                }
            }
        }
        System.out.printf("Defined %d scenarios (5 k x 5 bursts x 2 initial states).%n", scenarios.size());

        // Run simulations
        long startTime = System.currentTimeMillis();
        Map<ScenarioConfig, Map<String, List<TrialResult>>> allResults = runSimulations(scenarios, policies);
        long elapsedSec = (System.currentTimeMillis() - startTime) / 1000;
        System.out.printf("Completed all %d scenarios in %d seconds.%n%n", scenarios.size(), elapsedSec);

        // Process Part A: Full 50-row sweep table
        processPartA(scenarios, policies, allResults);

        // Process Part D: E10 Peak Window Load & 3-Metric Dominance (H10 Full Duration)
        processPartD("FULL DURATION [0, 60s] (H10)", TrialResult::peakLoads, scenarios, policies, allResults);

        // Process Part D Steady: E10 Steady-State (H10b) (t >= 5s)
        processPartD("STEADY-STATE [5s, 60s] (H10b)", TrialResult::peakLoadsSteady, scenarios, policies, allResults);

        // Process First 5s vs Steady-State comparison
        compareFirst5sVsSteadyState(scenarios, policies, allResults);
    }

    private static List<PolicyDef> buildPolicies() {
        List<PolicyDef> list = new ArrayList<>();
        // S1
        for (double b : S1_B_FRACS) {
            for (double p : S1_P_FACTORS) {
                String id = String.format("S1_B%.2f_P%.2f", b, p);
                String name = String.format("DualBucket(B=%.2fC,P=%.2fR)", b, p);
                list.add(new PolicyDef(id, name, "S1", b, p, 0.0, 0.0));
            }
        }
        // S2
        for (double cap : S2_CAP_FRACS) {
            String id = String.format("S2_C%.3f", cap);
            String name = String.format("TB(C=%.2fC,R=1.00R)", cap);
            list.add(new PolicyDef(id, name, "S2", 0.0, 0.0, cap, 1.0));
        }
        // S3
        for (double ref : S3_REFILL_FACTORS) {
            String id = String.format("S3_R%.2f", ref);
            String name = String.format("TB(C=0.43C,R=%.2fR)", ref);
            list.add(new PolicyDef(id, name, "S3", 0.0, 0.0, 3.0 / 7.0, ref));
        }
        // DREB Burst-matched
        list.add(new PolicyDef("DREB_BM", "DREB (burst-matched)", "Dynamic", 3.0 / 7.0, 5.0 / 3.0, 0.0, 1.0));
        return list;
    }

    private static Map<ScenarioConfig, Map<String, List<TrialResult>>> runSimulations(
            List<ScenarioConfig> scenarios, List<PolicyDef> policies
    ) {
        Map<ScenarioConfig, Map<String, List<TrialResult>>> results = new LinkedHashMap<>();
        double tauSec = (6.0 * CAPACITY / 7.0) / (2.0 * REFILL_RATE / 3.0); // 1.2857 s

        int count = 0;
        for (ScenarioConfig sc : scenarios) {
            count++;
            if (count % 10 == 0 || count == scenarios.size()) {
                System.out.printf("  Progress: [%d / %d] (Current: %s, k=%.2f, burst=%.2fC)%n",
                        count, scenarios.size(), sc.initialState(), sc.k(), sc.abuserBurstFrac());
            }

            // Pre-generate traces for 30 seeds
            PreGeneratedTrace[] traces = new PreGeneratedTrace[NUM_SEEDS];
            for (int s = 0; s < NUM_SEEDS; s++) {
                traces[s] = generateTrace(sc, tauSec, SEED_START + s);
            }

            Map<String, TrialResult[]> rawMap = new HashMap<>();
            for (PolicyDef p : policies) {
                rawMap.put(p.id(), new TrialResult[NUM_SEEDS]);
            }

            IntStream.range(0, NUM_SEEDS).parallel().forEach(s -> {
                long seed = SEED_START + s;
                PreGeneratedTrace trace = traces[s];
                for (PolicyDef p : policies) {
                    RateLimiter limiter = createLimiter(p);
                    TrialResult tr = evaluateTrace(limiter, p.id(), sc, seed, trace);
                    rawMap.get(p.id())[s] = tr;
                }
            });

            Map<String, List<TrialResult>> scenarioRes = new LinkedHashMap<>();
            for (PolicyDef p : policies) {
                scenarioRes.put(p.id(), Arrays.asList(rawMap.get(p.id())));
            }
            results.put(sc, scenarioRes);
        }
        return results;
    }

    private static RateLimiter createLimiter(PolicyDef p) {
        if ("S1".equals(p.family())) {
            double b = p.bFrac() * CAPACITY;
            double burstRate = p.pFactor() * REFILL_RATE;
            return new CustomRateLimiter(p.displayName(), b, burstRate, 9.0 * CAPACITY / 7.0, REFILL_RATE, 0.0, 0.0, 1.5);
        } else if ("S2".equals(p.family())) {
            double cap = p.capFrac() * CAPACITY;
            RateLimiterConfig cfg = RateLimiterConfig.builder()
                    .bucketCapacity((long) Math.round(cap))
                    .refillRate(REFILL_RATE)
                    .maxRequests((long) Math.round(cap))
                    .windowSizeMs(1000L)
                    .build();
            return new TokenBucket(cfg);
        } else if ("S3".equals(p.family())) {
            double cap = (3.0 * CAPACITY / 7.0);
            double ref = p.refillFactor() * REFILL_RATE;
            RateLimiterConfig cfg = RateLimiterConfig.builder()
                    .bucketCapacity((long) Math.round(cap))
                    .refillRate(ref)
                    .maxRequests((long) Math.round(cap))
                    .windowSizeMs(1000L)
                    .build();
            return new TokenBucket(cfg);
        } else {
            RateLimiterConfig cfg = RateLimiterConfig.builder()
                    .bucketCapacity((long) Math.round(CAPACITY))
                    .refillRate(REFILL_RATE)
                    .maxRequests((long) Math.round(CAPACITY))
                    .windowSizeMs(1000L)
                    .build();
            return new CustomRateLimiter(cfg, CustomRateLimiter.BURST_CEILING_MATCHED);
        }
    }

    private static PreGeneratedTrace generateTrace(ScenarioConfig sc, double tauSec, long seed) {
        int numAbusive = (int) Math.round(TOTAL_CLIENTS * F_ABUSIVE); // 60
        int numLegit = TOTAL_CLIENTS - numAbusive;                    // 140
        int numSteady = numLegit / 2;                                 // 70
        int numBursty = numLegit - numSteady;                         // 70

        // 1. Abusers
        List<List<RequestEvent>> abuserTraces = new ArrayList<>();
        int abBurst = (int) Math.round(sc.abuserBurstFrac() * CAPACITY);
        double abDt = (sc.abuserBurstFrac() * CAPACITY) / (sc.k() * REFILL_RATE);
        for (int i = 0; i < numAbusive; i++) {
            List<RequestEvent> evts = new ArrayList<>();
            double t = 0.0;
            while (t < DURATION_SEC) {
                long tMs = Math.round(t * 1000.0);
                evts.add(new RequestEvent(tMs, abBurst));
                t += abDt;
            }
            abuserTraces.add(evts);
        }

        // 2. Steady Poisson
        List<List<RequestEvent>> steadyTraces = new ArrayList<>();
        long stepMs = 50L;
        for (int i = 0; i < numSteady; i++) {
            List<RequestEvent> evts = new ArrayList<>();
            Random stRng = new Random(seed * 10007L + i * 19L);
            double lambda = (0.50 + stRng.nextDouble() * 0.40) * REFILL_RATE;
            double meanPerStep = lambda * (stepMs / 1000.0);

            for (long ms = 0; ms < (long) (DURATION_SEC * 1000.0); ms += stepMs) {
                int count = samplePoisson(meanPerStep, stRng);
                if (count > 0) evts.add(new RequestEvent(ms, count));
            }
            steadyTraces.add(evts);
        }

        // 3. Bursty
        List<List<RequestEvent>> burstyTraces = new ArrayList<>();
        for (int i = 0; i < numBursty; i++) {
            List<RequestEvent> evts = new ArrayList<>();
            Random buRng = new Random(seed * 50021L + i * 23L);
            double t = 0.0;
            while (t < DURATION_SEC) {
                double idleDt = -Math.log(1.0 - buRng.nextDouble()) * (3.0 * tauSec);
                t += idleDt;
                if (t >= DURATION_SEC) break;
                long tMs = Math.round(t * 1000.0);
                double bFrac = 0.30 + buRng.nextDouble() * (0.90 - 0.30);
                int burst = (int) Math.round(bFrac * CAPACITY);
                evts.add(new RequestEvent(tMs, burst));
            }
            burstyTraces.add(evts);
        }

        return new PreGeneratedTrace(abuserTraces, steadyTraces, burstyTraces);
    }

    private static int samplePoisson(double lambda, Random rng) {
        if (lambda <= 0.0) return 0;
        double l = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;
        do {
            k++;
            p *= rng.nextDouble();
        } while (p > l);
        return k - 1;
    }

    private static TrialResult evaluateTrace(
            RateLimiter limiter, String policyId, ScenarioConfig sc, long seed, PreGeneratedTrace trace
    ) {
        String init = sc.initialState();
        int numAbusive = trace.abuserTraces().size();
        int numSteady = trace.steadyTraces().size();
        int numBursty = trace.burstyTraces().size();

        for (int i = 0; i < numAbusive; i++) limiter.initializeState("abuser_" + i, init, 0.0);
        for (int i = 0; i < numSteady; i++) limiter.initializeState("steady_" + i, init, 0.0);
        for (int i = 0; i < numBursty; i++) limiter.initializeState("bursty_" + i, init, 0.0);

        long abAcc = 0;
        double[] peakLoads = new double[WINDOWS_SEC.length];
        double[] peakLoadsFirst5s = new double[WINDOWS_SEC.length];
        double[] peakLoadsSteady = new double[WINDOWS_SEC.length];

        // Evaluate abuser clients and track sliding window peak loads
        for (int i = 0; i < numAbusive; i++) {
            String cid = "abuser_" + i;
            List<RequestEvent> evts = trace.abuserTraces().get(i);
            long[] timestamps = new long[evts.size()];
            long[] admitted = new long[evts.size()];

            for (int eIdx = 0; eIdx < evts.size(); eIdx++) {
                RequestEvent e = evts.get(eIdx);
                long accepted = admitBurst(limiter, cid, e.tMs(), e.cost());
                timestamps[eIdx] = e.tMs();
                admitted[eIdx] = accepted;
                abAcc += accepted;
            }

            // Only need to compute sliding windows for client 0 since all abusers have identical arrival schedules
            if (i == 0) {
                for (int wIdx = 0; wIdx < WINDOWS_SEC.length; wIdx++) {
                    long wMs = Math.round(WINDOWS_SEC[wIdx] * 1000.0);
                    long maxWindowCost = 0;
                    long maxWindowCostFirst5s = 0;
                    long maxWindowCostSteady = 0;
                    int right = 0;
                    long curWindowCost = 0;

                    for (int left = 0; left < evts.size(); left++) {
                        while (right < evts.size() && timestamps[right] <= timestamps[left] + wMs) {
                            curWindowCost += admitted[right];
                            right++;
                        }
                        if (curWindowCost > maxWindowCost) maxWindowCost = curWindowCost;
                        if (timestamps[left] < 5000L) {
                            if (curWindowCost > maxWindowCostFirst5s) maxWindowCostFirst5s = curWindowCost;
                        } else {
                            if (curWindowCost > maxWindowCostSteady) maxWindowCostSteady = curWindowCost;
                        }
                        curWindowCost -= admitted[left];
                    }
                    peakLoads[wIdx] = (double) maxWindowCost / CAPACITY;
                    peakLoadsFirst5s[wIdx] = (double) maxWindowCostFirst5s / CAPACITY;
                    peakLoadsSteady[wIdx] = (double) maxWindowCostSteady / CAPACITY;
                }
            }
        }

        for (int i = 0; i < numSteady; i++) {
            String cid = "steady_" + i;
            for (RequestEvent e : trace.steadyTraces().get(i)) {
                admitBurst(limiter, cid, e.tMs(), e.cost());
            }
        }

        long buOff = 0, buAcc = 0;
        for (int i = 0; i < numBursty; i++) {
            String cid = "bursty_" + i;
            for (RequestEvent e : trace.burstyTraces().get(i)) {
                buOff += e.cost();
                buAcc += admitBurst(limiter, cid, e.tMs(), e.cost());
            }
        }

        double abRateNorm = (double) abAcc / (DURATION_SEC * numAbusive * REFILL_RATE);
        double burstyFrr = buOff > 0 ? (double) (buOff - buAcc) / buOff : 0.0;

        return new TrialResult(policyId, seed, abRateNorm, burstyFrr, peakLoads, peakLoadsFirst5s, peakLoadsSteady);
    }

    private static long admitBurst(RateLimiter limiter, String clientId, long tMs, int burstTokens) {
        long accepted = 0;
        int remaining = burstTokens;
        int batch = Math.min(remaining, 500);
        while (remaining > 0) {
            int toRequest = Math.min(remaining, batch);
            if (limiter.allowEngineAlone(clientId, tMs, toRequest)) {
                accepted += toRequest;
                remaining -= toRequest;
            } else {
                if (batch > 1) {
                    batch = Math.max(1, batch / 2);
                } else {
                    break;
                }
            }
        }
        return accepted;
    }

    private static void processPartA(
            List<ScenarioConfig> scenarios, List<PolicyDef> policies,
            Map<ScenarioConfig, Map<String, List<TrialResult>>> allResults
    ) {
        System.out.println("=================================================================================================================================");
        System.out.println("  PART A: FULL ABUSER-BURST SWEEP TABLE (50 SCENARIOS)");
        System.out.println("=================================================================================================================================");
        System.out.println("| Initial State | Overload k | Burst Size | DREB Rate | DREB Bursty FRR | Verdict | Dominating Static Policy | Delta Rate [95% CI] | Delta Bursty FRR [95% CI] |");
        System.out.println("| :---: | :---: | :---: | :---: | :---: | :---: | :--- | :---: | :---: |");

        for (ScenarioConfig sc : scenarios) {
            Map<String, List<TrialResult>> res = allResults.get(sc);
            List<TrialResult> drebTrials = res.get("DREB_BM");

            double drebRateMean = meanRate(drebTrials);
            double drebFrrMean = meanFrr(drebTrials);

            // Find dominating static policy
            PolicyDef bestDominating = null;
            double bestDomDeltaFrr = 0.0;
            double[] bestDomRateCi = null;
            double[] bestDomFrrCi = null;

            for (PolicyDef p : policies) {
                if ("Dynamic".equals(p.family())) continue;

                List<TrialResult> pTrials = res.get(p.id());
                List<Double> dRates = new ArrayList<>();
                List<Double> dFrrs = new ArrayList<>();

                for (int s = 0; s < NUM_SEEDS; s++) {
                    dRates.add(pTrials.get(s).abuserAdmittedRateNorm() - drebTrials.get(s).abuserAdmittedRateNorm());
                    dFrrs.add((pTrials.get(s).burstyFrr() - drebTrials.get(s).burstyFrr()) * 100.0);
                }

                double[] dRateCi = bootstrap(dRates);
                double[] dFrrCi = bootstrap(dFrrs);

                // Dominance: Rate <= and FRR <= with at least one strictly lower
                boolean betterRate = dRateCi[1] < -1e-6;
                boolean noWorseRate = mean(dRates) <= 1e-6 && dRateCi[1] <= 1e-6;
                boolean betterFrr = dFrrCi[1] < -1e-6;
                boolean noWorseFrr = mean(dFrrs) <= 1e-6 && dFrrCi[1] <= 1e-6;

                if ((betterRate || betterFrr) && noWorseRate && noWorseFrr) {
                    double deltaFrr = mean(dFrrs);
                    if (bestDominating == null || deltaFrr < bestDomDeltaFrr) {
                        bestDominating = p;
                        bestDomDeltaFrr = deltaFrr;
                        bestDomRateCi = dRateCi;
                        bestDomFrrCi = dFrrCi;
                    }
                }
            }

            String verdict = (bestDominating != null) ? "**DOMINATED**" : "**NON-DOMINATED**";
            String domPolicyName = (bestDominating != null) ? bestDominating.displayName() : "None";
            String deltaRateStr = (bestDominating != null)
                    ? String.format("%+.3f [%+.3f, %+.3f]", (bestDomRateCi[0] + bestDomRateCi[1]) / 2, bestDomRateCi[0], bestDomRateCi[1])
                    : "N/A";
            String deltaFrrStr = (bestDominating != null)
                    ? String.format("%+.2f%% [%+.2f%%, %+.2f%%]", bestDomDeltaFrr, bestDomFrrCi[0], bestDomFrrCi[1])
                    : "N/A";

            System.out.printf("| %-10s | %10.2f | %9.2fC | %8.3fR | %14.2f%% | %-15s | %-32s | %-21s | %-26s |%n",
                    sc.initialState(), sc.k(), sc.abuserBurstFrac(), drebRateMean, drebFrrMean,
                    verdict, domPolicyName, deltaRateStr, deltaFrrStr);
        }
    }

    @FunctionalInterface
    interface PeakExtractor {
        double[] extract(TrialResult tr);
    }

    private static void processPartD(
            String sectionTitle,
            PeakExtractor extractor,
            List<ScenarioConfig> scenarios, List<PolicyDef> policies,
            Map<ScenarioConfig, Map<String, List<TrialResult>>> allResults
    ) {
        System.out.println("\n=================================================================================================================================");
        System.out.println("  PART D: " + sectionTitle);
        System.out.println("=================================================================================================================================");
        System.out.printf("%-10s | %4s | %6s | %5s | %-15s | %-30s | %-22s | %-12s%n",
                "InitState", "k", "Burst", "W", "Verdict", "Best Static (Rate & FRR <=)", "Delta PeakLoad [95% CI]", "3-Metric Dom?");
        System.out.println("-----------+------+--------+-------+-----------------+--------------------------------+------------------------+-------------");

        for (int wIdx = 0; wIdx < WINDOWS_SEC.length; wIdx++) {
            double wSec = WINDOWS_SEC[wIdx];
            String wStr = String.format("%.2fC/R", wSec);

            for (ScenarioConfig sc : scenarios) {
                Map<String, List<TrialResult>> res = allResults.get(sc);
                List<TrialResult> drebTrials = res.get("DREB_BM");

                // Against all static policies that are NOT worse on Rate and FRR (Rate <= and FRR <=),
                // does DREB have strictly lower peak load (PeakLoad(Static) - PeakLoad(DREB) > 0)?
                PolicyDef bestNonWorseStatic = null;
                double minDeltaPeakLoad = Double.MAX_VALUE;
                double[] minDeltaPeakCi = null;
                boolean anyStaticBeatsOrEqualsPeak = false;
                boolean any3MetricDominates = false;

                for (PolicyDef p : policies) {
                    if ("Dynamic".equals(p.family())) continue;
                    List<TrialResult> pTrials = res.get(p.id());

                    List<Double> dRates = new ArrayList<>();
                    List<Double> dFrrs = new ArrayList<>();
                    List<Double> dPeaks = new ArrayList<>();

                    for (int s = 0; s < NUM_SEEDS; s++) {
                        dRates.add(pTrials.get(s).abuserAdmittedRateNorm() - drebTrials.get(s).abuserAdmittedRateNorm());
                        dFrrs.add((pTrials.get(s).burstyFrr() - drebTrials.get(s).burstyFrr()) * 100.0);
                        // Candidate minus Reference: Static PeakLoad - DREB PeakLoad
                        dPeaks.add(extractor.extract(pTrials.get(s))[wIdx] - extractor.extract(drebTrials.get(s))[wIdx]);
                    }

                    double[] dRateCi = bootstrap(dRates);
                    double[] dFrrCi = bootstrap(dFrrs);
                    double[] dPeakCi = bootstrap(dPeaks);

                    boolean staticNoWorseRate = mean(dRates) <= 1e-6 && dRateCi[1] <= 1e-6;
                    boolean staticNoWorseFrr = mean(dFrrs) <= 1e-6 && dFrrCi[1] <= 1e-6;

                    if (staticNoWorseRate && staticNoWorseFrr) {
                        double deltaPeakMean = mean(dPeaks);
                        if (deltaPeakMean < minDeltaPeakLoad) {
                            minDeltaPeakLoad = deltaPeakMean;
                            minDeltaPeakCi = dPeakCi;
                            bestNonWorseStatic = p;
                        }

                        // If Static has peak load <= DREB (upper CI <= 0 or mean <= 0), then DREB does NOT have lower peak load!
                        if (dPeakCi[0] <= 1e-6) {
                            anyStaticBeatsOrEqualsPeak = true;
                        }

                        // 3-Metric Dominance: Static has Rate <= DREB, FRR <= DREB, AND PeakLoad <= DREB with at least one strictly lower
                        boolean betterRate = dRateCi[1] < -1e-6;
                        boolean betterFrr = dFrrCi[1] < -1e-6;
                        boolean betterPeak = dPeakCi[1] < -1e-6;
                        if (betterRate || betterFrr || betterPeak) {
                            any3MetricDominates = true;
                        }
                    }
                }

                String verdictH = (bestNonWorseStatic == null)
                        ? "VACUOUS (No static <=)"
                        : (anyStaticBeatsOrEqualsPeak ? "REFUTED" : "SUPPORTED");
                String staticName = (bestNonWorseStatic != null) ? bestNonWorseStatic.displayName() : "None";
                String peakDiffStr = (bestNonWorseStatic != null)
                        ? String.format("%+.3f [%+.3f, %+.3f]", minDeltaPeakLoad, minDeltaPeakCi[0], minDeltaPeakCi[1])
                        : "N/A";
                String domStr = any3MetricDominates ? "YES (Dominated)" : "NO";

                System.out.printf("%-10s | %4.2f | %5.2fC | %7s | %-15s | %-30s | %-22s | %-12s%n",
                        sc.initialState(), sc.k(), sc.abuserBurstFrac(), wStr, verdictH, staticName, peakDiffStr, domStr);
            }
        }

        // Summary Aggregated Table
        System.out.println("\n=================================================================================================================================");
        System.out.println("  SUMMARY BREAKDOWN (" + sectionTitle + "): INITIAL STATE x ABUSER BURST x WINDOW W");
        System.out.println("=================================================================================================================================");
        System.out.println("| Initial State | Abuser Burst | Window W | DREB Strictly Lower (k cells) | Ties (k cells) | DREB Strictly Higher (k cells) | Vacuous (No Static <=) |");
        System.out.println("| :---: | :---: | :---: | :---: | :---: | :---: | :---: |");

        for (String init : INITIAL_STATES) {
            for (double bFrac : ABUSER_BURSTS) {
                for (int wIdx = 0; wIdx < WINDOWS_SEC.length; wIdx++) {
                    double wSec = WINDOWS_SEC[wIdx];
                    String wStr = String.format("%.2fC/R", wSec);
                    int lowerCount = 0;
                    int tieCount = 0;
                    int higherCount = 0;
                    int vacuousCount = 0;

                    for (double k : K_VALUES) {
                        ScenarioConfig sc = new ScenarioConfig(CAPACITY, init, k, bFrac);
                        Map<String, List<TrialResult>> res = allResults.get(sc);
                        List<TrialResult> drebTrials = res.get("DREB_BM");

                        PolicyDef bestNonWorseStatic = null;
                        double minDeltaPeakLoad = Double.MAX_VALUE;
                        double[] minDeltaPeakCi = null;

                        for (PolicyDef p : policies) {
                            if ("Dynamic".equals(p.family())) continue;
                            List<TrialResult> pTrials = res.get(p.id());

                            List<Double> dRates = new ArrayList<>();
                            List<Double> dFrrs = new ArrayList<>();
                            List<Double> dPeaks = new ArrayList<>();

                            for (int s = 0; s < NUM_SEEDS; s++) {
                                dRates.add(pTrials.get(s).abuserAdmittedRateNorm() - drebTrials.get(s).abuserAdmittedRateNorm());
                                dFrrs.add((pTrials.get(s).burstyFrr() - drebTrials.get(s).burstyFrr()) * 100.0);
                                dPeaks.add(extractor.extract(pTrials.get(s))[wIdx] - extractor.extract(drebTrials.get(s))[wIdx]);
                            }

                            double[] dRateCi = bootstrap(dRates);
                            double[] dFrrCi = bootstrap(dFrrs);
                            double[] dPeakCi = bootstrap(dPeaks);

                            boolean staticNoWorseRate = mean(dRates) <= 1e-6 && dRateCi[1] <= 1e-6;
                            boolean staticNoWorseFrr = mean(dFrrs) <= 1e-6 && dFrrCi[1] <= 1e-6;

                            if (staticNoWorseRate && staticNoWorseFrr) {
                                double deltaPeakMean = mean(dPeaks);
                                if (deltaPeakMean < minDeltaPeakLoad) {
                                    minDeltaPeakLoad = deltaPeakMean;
                                    minDeltaPeakCi = dPeakCi;
                                    bestNonWorseStatic = p;
                                }
                            }
                        }

                        if (bestNonWorseStatic == null) {
                            vacuousCount++;
                        } else {
                            if (minDeltaPeakCi[0] > 1e-6) {
                                lowerCount++;
                            } else if (minDeltaPeakCi[1] < -1e-6) {
                                higherCount++;
                            } else {
                                tieCount++;
                            }
                        }
                    }

                    System.out.printf("| %-10s | %5.2fC | %8s | %29d | %14d | %30d | %22d |%n",
                            init, bFrac, wStr, lowerCount, tieCount, higherCount, vacuousCount);
                }
            }
        }

        // Detailed coldStart & preWarmed at bursts >= 0.70C per k
        for (String init : INITIAL_STATES) {
            System.out.println("\n=================================================================================================================================");
            System.out.printf("  DETAILED (%s) AT BURSTS >= 0.70C: DREB PEAK vs BEST NON-WORSE STATIC PEAK PER k [%s]%n", init, sectionTitle);
            System.out.println("=================================================================================================================================");
            System.out.println("| Window W | Overload k | Burst Size | DREB Peak Load | Best Non-Worse Static Policy | Static Peak Load | Paired Delta Peak [95% CI] | Verdict |");
            System.out.println("| :---: | :---: | :---: | :---: | :--- | :---: | :---: | :---: |");

            for (int wIdx = 0; wIdx < WINDOWS_SEC.length; wIdx++) {
                double wSec = WINDOWS_SEC[wIdx];
                String wStr = String.format("%.2fC/R", wSec);

                for (double bFrac : new double[]{0.70, 0.95, 1.20}) {
                    for (double k : K_VALUES) {
                        ScenarioConfig sc = new ScenarioConfig(CAPACITY, init, k, bFrac);
                        Map<String, List<TrialResult>> res = allResults.get(sc);
                        List<TrialResult> drebTrials = res.get("DREB_BM");

                        double drebPeakMean = 0;
                        for (TrialResult tr : drebTrials) drebPeakMean += extractor.extract(tr)[wIdx];
                        drebPeakMean /= drebTrials.size();

                        PolicyDef bestNonWorseStatic = null;
                        double minDeltaPeakLoad = Double.MAX_VALUE;
                        double[] minDeltaPeakCi = null;
                        double bestStaticPeakMean = 0;

                        for (PolicyDef p : policies) {
                            if ("Dynamic".equals(p.family())) continue;
                            List<TrialResult> pTrials = res.get(p.id());

                            List<Double> dRates = new ArrayList<>();
                            List<Double> dFrrs = new ArrayList<>();
                            List<Double> dPeaks = new ArrayList<>();

                            for (int s = 0; s < NUM_SEEDS; s++) {
                                dRates.add(pTrials.get(s).abuserAdmittedRateNorm() - drebTrials.get(s).abuserAdmittedRateNorm());
                                dFrrs.add((pTrials.get(s).burstyFrr() - drebTrials.get(s).burstyFrr()) * 100.0);
                                dPeaks.add(extractor.extract(pTrials.get(s))[wIdx] - extractor.extract(drebTrials.get(s))[wIdx]);
                            }

                            double[] dRateCi = bootstrap(dRates);
                            double[] dFrrCi = bootstrap(dFrrs);
                            double[] dPeakCi = bootstrap(dPeaks);

                            boolean staticNoWorseRate = mean(dRates) <= 1e-6 && dRateCi[1] <= 1e-6;
                            boolean staticNoWorseFrr = mean(dFrrs) <= 1e-6 && dFrrCi[1] <= 1e-6;

                            if (staticNoWorseRate && staticNoWorseFrr) {
                                double deltaPeakMean = mean(dPeaks);
                                if (deltaPeakMean < minDeltaPeakLoad) {
                                    minDeltaPeakLoad = deltaPeakMean;
                                    minDeltaPeakCi = dPeakCi;
                                    bestNonWorseStatic = p;

                                    double pMean = 0;
                                    for (TrialResult tr : pTrials) pMean += extractor.extract(tr)[wIdx];
                                    bestStaticPeakMean = pMean / pTrials.size();
                                }
                            }
                        }

                        String staticName = (bestNonWorseStatic != null) ? bestNonWorseStatic.displayName() : "None (Vacuous)";
                        String staticPeakStr = (bestNonWorseStatic != null) ? String.format("%.3fC", bestStaticPeakMean) : "N/A";
                        String deltaStr = (bestNonWorseStatic != null)
                                ? String.format("%+.3fC [%+.3f, %+.3f]", minDeltaPeakLoad, minDeltaPeakCi[0], minDeltaPeakCi[1])
                                : "N/A";
                        String verdict = (bestNonWorseStatic == null)
                                ? "Vacuous (Non-Dominated)"
                                : (minDeltaPeakCi[0] > 1e-6 ? "SUPPORTED (DREB Lower)" : (minDeltaPeakCi[1] < -1e-6 ? "REFUTED (Static Lower)" : "REFUTED (Tied)"));

                        System.out.printf("| %7s | %10.2f | %9.2fC | %14.3fC | %-28s | %16s | %27s | %-24s |%n",
                                wStr, k, bFrac, drebPeakMean, staticName, staticPeakStr, deltaStr, verdict);
                    }
                }
            }
        }
    }

    private static void compareFirst5sVsSteadyState(
            List<ScenarioConfig> scenarios, List<PolicyDef> policies,
            Map<ScenarioConfig, Map<String, List<TrialResult>>> allResults
    ) {
        System.out.println("\n=================================================================================================================================");
        System.out.println("  FIRST 5s vs STEADY-STATE PEAK LOAD COMPARISON (ATTRIBUTABLE TO FIRST BURST)");
        System.out.println("=================================================================================================================================");
        System.out.println("| InitState | Overload k | Burst Size | Window W | DREB First 5s | DREB Steady | DREB Drop | Static First 5s | Static Steady | Static Drop | Verdict (Steady) |");
        System.out.println("| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |");

        for (String init : INITIAL_STATES) {
            for (double bFrac : ABUSER_BURSTS) {
                for (double k : K_VALUES) {
                    ScenarioConfig sc = new ScenarioConfig(CAPACITY, init, k, bFrac);
                    Map<String, List<TrialResult>> res = allResults.get(sc);
                    List<TrialResult> drebTrials = res.get("DREB_BM");

                    for (int wIdx = 0; wIdx < WINDOWS_SEC.length; wIdx++) {
                        double wSec = WINDOWS_SEC[wIdx];
                        String wStr = String.format("%.2fC/R", wSec);

                        double drebFirst5s = 0, drebSteady = 0;
                        for (TrialResult tr : drebTrials) {
                            drebFirst5s += tr.peakLoadsFirst5s()[wIdx];
                            drebSteady += tr.peakLoadsSteady()[wIdx];
                        }
                        drebFirst5s /= drebTrials.size();
                        drebSteady /= drebTrials.size();

                        // Find best non-worse static policy under steady state
                        PolicyDef bestNonWorseStatic = null;
                        double minDeltaSteady = Double.MAX_VALUE;
                        double[] minDeltaSteadyCi = null;
                        double staticFirst5s = 0, staticSteady = 0;

                        for (PolicyDef p : policies) {
                            if ("Dynamic".equals(p.family())) continue;
                            List<TrialResult> pTrials = res.get(p.id());

                            List<Double> dRates = new ArrayList<>();
                            List<Double> dFrrs = new ArrayList<>();
                            List<Double> dPeaksSteady = new ArrayList<>();

                            for (int s = 0; s < NUM_SEEDS; s++) {
                                dRates.add(pTrials.get(s).abuserAdmittedRateNorm() - drebTrials.get(s).abuserAdmittedRateNorm());
                                dFrrs.add((pTrials.get(s).burstyFrr() - drebTrials.get(s).burstyFrr()) * 100.0);
                                dPeaksSteady.add(pTrials.get(s).peakLoadsSteady()[wIdx] - drebTrials.get(s).peakLoadsSteady()[wIdx]);
                            }

                            double[] dRateCi = bootstrap(dRates);
                            double[] dFrrCi = bootstrap(dFrrs);
                            double[] dPeakSteadyCi = bootstrap(dPeaksSteady);

                            boolean staticNoWorseRate = mean(dRates) <= 1e-6 && dRateCi[1] <= 1e-6;
                            boolean staticNoWorseFrr = mean(dFrrs) <= 1e-6 && dFrrCi[1] <= 1e-6;

                            if (staticNoWorseRate && staticNoWorseFrr) {
                                double deltaMean = mean(dPeaksSteady);
                                if (deltaMean < minDeltaSteady) {
                                    minDeltaSteady = deltaMean;
                                    minDeltaSteadyCi = dPeakSteadyCi;
                                    bestNonWorseStatic = p;

                                    double sF5 = 0, sSt = 0;
                                    for (TrialResult tr : pTrials) {
                                        sF5 += tr.peakLoadsFirst5s()[wIdx];
                                        sSt += tr.peakLoadsSteady()[wIdx];
                                    }
                                    staticFirst5s = sF5 / pTrials.size();
                                    staticSteady = sSt / pTrials.size();
                                }
                            }
                        }

                        String staticF5Str = (bestNonWorseStatic != null) ? String.format("%.3fC", staticFirst5s) : "N/A";
                        String staticStStr = (bestNonWorseStatic != null) ? String.format("%.3fC", staticSteady) : "N/A";
                        String staticDropStr = (bestNonWorseStatic != null) ? String.format("%.3fC", staticFirst5s - staticSteady) : "N/A";
                        String verdictSteady = (bestNonWorseStatic == null)
                                ? "Vacuous"
                                : (minDeltaSteadyCi[0] > 1e-6 ? "DREB Lower" : (minDeltaSteadyCi[1] < -1e-6 ? "Static Lower" : "Tied"));

                        System.out.printf("| %-9s | %10.2f | %9.2fC | %8s | %13.3fC | %11.3fC | %9.3fC | %15s | %13s | %11s | %-16s |%n",
                                init, k, bFrac, wStr, drebFirst5s, drebSteady, (drebFirst5s - drebSteady),
                                staticF5Str, staticStStr, staticDropStr, verdictSteady);
                    }
                }
            }
        }
    }

    private static double meanRate(List<TrialResult> list) {
        double s = 0;
        for (TrialResult tr : list) s += tr.abuserAdmittedRateNorm();
        return s / list.size();
    }

    private static double meanFrr(List<TrialResult> list) {
        double s = 0;
        for (TrialResult tr : list) s += tr.burstyFrr() * 100.0;
        return s / list.size();
    }

    private static double mean(List<Double> list) {
        double s = 0;
        for (double v : list) s += v;
        return s / list.size();
    }

    private static double[] bootstrap(List<Double> samples) {
        int n = samples.size();
        int b = 10000;
        double[] bootMeans = new double[b];
        Random rng = new Random(42);

        for (int i = 0; i < b; i++) {
            double sum = 0.0;
            for (int j = 0; j < n; j++) {
                sum += samples.get(rng.nextInt(n));
            }
            bootMeans[i] = sum / n;
        }

        Arrays.sort(bootMeans);
        int lowIdx = (int) Math.floor(b * 0.025);
        int highIdx = (int) Math.ceil(b * 0.975);
        return new double[]{bootMeans[lowIdx], bootMeans[highIdx]};
    }
}

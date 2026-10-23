package com.ratelimiter.experiments;

import com.ratelimiter.algorithms.CustomRateLimiter;
import com.ratelimiter.algorithms.CustomRateLimiterV2;
import com.ratelimiter.algorithms.DrebParameters;
import com.ratelimiter.algorithms.TokenBucket;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Generalization sweep for Item A3:
 * Evaluates default v2, selected-by-rule (abuseDecayRate=0.25), and v1 burst-matched
 * across k in {1.1, 1.25, 2.0, 4.0} and f in {0.10, 0.50} on seeds 2000-2029.
 */
public final class ExperimentE3Generalization {

    private static final double DURATION_SEC = 120.0;
    private static final int TOTAL_CLIENTS = 200;

    public record GenResult(
            String configName,
            double capacity,
            String initialState,
            double f,
            double k,
            long seed,
            long abOffered,
            long abAccepted,
            double arr,
            double normRate,
            long legitOffered,
            long legitAccepted,
            double frrOverall,
            long burstyOffered,
            long burstyAccepted,
            double frrBursty
    ) {}

    public static void main(String[] args) throws Exception {
        boolean smoke = false;
        for (String a : args) {
            if ("--smoke".equalsIgnoreCase(a) || "-s".equalsIgnoreCase(a)) smoke = true;
        }

        int numSeeds = smoke ? 5 : 30;
        double[] capacities = {1000.0, 50000.0};
        String[] initialStates = {"coldStart", "preWarmed"};
        double[] fractions = {0.10, 0.50};
        double[] kValues = {1.1, 1.25, 2.0, 4.0};

        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<GenResult> results = new CopyOnWriteArrayList<>();
        List<Callable<Void>> tasks = new ArrayList<>();

        for (double c : capacities) {
            for (String is : initialStates) {
                for (double f : fractions) {
                    for (double k : kValues) {
                        for (int s = 0; s < numSeeds; s++) {
                            long seed = 2000L + s;
                            tasks.add(() -> {
                                Map<String, Supplier<RateLimiter>> algos = buildAlgorithms(c);
                                for (Map.Entry<String, Supplier<RateLimiter>> e : algos.entrySet()) {
                                    RateLimiter limiter = e.getValue().get();
                                    limiter.reset();
                                    results.add(runTrial(e.getKey(), c, is, f, k, seed, limiter));
                                }
                                return null;
                            });
                        }
                    }
                }
            }
        }

        System.out.printf("Running %d generalization tasks across %d threads...%n", tasks.size(), threads);
        AtomicInteger done = new AtomicInteger(0);
        long start = System.currentTimeMillis();
        List<Callable<Void>> inst = new ArrayList<>();
        for (Callable<Void> t : tasks) {
            inst.add(() -> {
                t.call();
                int d = done.incrementAndGet();
                if (d % 100 == 0 || d == tasks.size()) {
                    System.out.printf("  Progress: %d / %d (%.1f%%) in %.1f s%n",
                            d, tasks.size(), (d * 100.0) / tasks.size(), (System.currentTimeMillis() - start) / 1000.0);
                }
                return null;
            });
        }
        pool.invokeAll(inst);
        pool.shutdown();

        printGeneralizationReport(results, capacities, initialStates, fractions, kValues);
    }

    private static Map<String, Supplier<RateLimiter>> buildAlgorithms(double c) {
        Map<String, Supplier<RateLimiter>> m = new LinkedHashMap<>();
        double r = c;

        RateLimiterConfig tbLg = RateLimiterConfig.builder().bucketCapacity((long) c).refillRate(r).maxRequests((long) c).windowSizeMs(1000L).build();
        m.put("TB-large", () -> new TokenBucket(tbLg));

        RateLimiterConfig v1 = RateLimiterConfig.builder().bucketCapacity((long) c).refillRate(r).maxRequests((long) c).windowSizeMs(1000L).build();
        m.put("v1 burst-matched", () -> new CustomRateLimiter(v1, CustomRateLimiter.BURST_CEILING_MATCHED));

        m.put("v2 base 1.5 full", () -> new CustomRateLimiterV2(DrebParameters.forV2Base15(c, r)));
        m.put("v2 selected-by-rule (abuseDecayRate=0.25)", () -> new CustomRateLimiterV2(DrebParameters.customV2(c, r, 1.5, 0.3, 2.0, 0.25, 0.35, true, true)));

        return m;
    }

    private static GenResult runTrial(String configName, double c, String initialState, double f, double k, long seed, RateLimiter limiter) {
        double r = c;
        double tauSec = (c * 6.0 / 7.0) / (r * 2.0 / 3.0);

        int numAbusive = (int) Math.round(TOTAL_CLIENTS * f);
        int numLegit = TOTAL_CLIENTS - numAbusive;
        int numSteady = numLegit / 2;
        int numBursty = numLegit - numSteady;

        for (int i = 0; i < TOTAL_CLIENTS; i++) {
            String cid = i < numAbusive ? "abuser_" + i : (i < numAbusive + numSteady ? "steady_" + (i - numAbusive) : "bursty_" + (i - numAbusive - numSteady));
            limiter.initializeState(cid, initialState, 0.0);
        }

        long abOff = 0;
        long abAcc = 0;
        int burstSize = (int) Math.round(0.95 * c);
        double dtAbuser = (0.95 * c) / (k * r);

        for (int i = 0; i < numAbusive; i++) {
            String cid = "abuser_" + i;
            double t = 0.0;
            while (t < DURATION_SEC) {
                long tMs = Math.round(t * 1000.0);
                abOff += burstSize;
                abAcc += admitBurst(limiter, cid, tMs, burstSize);
                t += dtAbuser;
            }
        }
        double arr = abOff > 0 ? (double) (abOff - abAcc) / abOff : 0.0;
        double normRate = (double) abAcc / (numAbusive * r * DURATION_SEC);

        long steadyOff = 0;
        long steadyAcc = 0;
        long stepMs = 50L;
        long numSteps = (long) Math.floor((DURATION_SEC * 1000.0) / stepMs);

        for (int i = 0; i < numSteady; i++) {
            String cid = "steady_" + i;
            Random stRng = new Random(seed * 10007L + i * 19L);
            double rate = (0.50 + stRng.nextDouble() * 0.40) * r;
            double lambda = rate * (stepMs / 1000.0);

            for (long step = 0; step < numSteps; step++) {
                int reqs = samplePoisson(lambda, stRng);
                if (reqs <= 0) continue;
                steadyOff += reqs;
                long tMs = step * stepMs;
                RateLimitRequest req = new RateLimitRequest(cid, tMs, reqs);
                if (limiter.allow(req).isAllowed()) {
                    steadyAcc += reqs;
                } else {
                    steadyAcc += admitBurst(limiter, cid, tMs, reqs);
                }
            }
        }

        long burstyOff = 0;
        long burstyAcc = 0;

        for (int i = 0; i < numBursty; i++) {
            String cid = "bursty_" + i;
            Random buRng = new Random(seed * 50021L + i * 23L);
            double t = 0.0;

            while (t < DURATION_SEC) {
                double idleDt = -Math.log(1.0 - buRng.nextDouble()) * (3.0 * tauSec);
                t += idleDt;
                if (t >= DURATION_SEC) break;

                long tMs = Math.round(t * 1000.0);
                double bFrac = 0.30 + buRng.nextDouble() * 0.60;
                int burst = (int) Math.round(bFrac * c);
                burstyOff += burst;
                burstyAcc += admitBurst(limiter, cid, tMs, burst);
            }
        }

        long legitOff = steadyOff + burstyOff;
        long legitAcc = steadyAcc + burstyAcc;
        double frrOverall = legitOff > 0 ? (double) (legitOff - legitAcc) / legitOff : 0.0;
        double frrBursty = burstyOff > 0 ? (double) (burstyOff - burstyAcc) / burstyOff : 0.0;

        return new GenResult(configName, c, initialState, f, k, seed,
                abOff, abAcc, arr, normRate, legitOff, legitAcc, frrOverall, burstyOff, burstyAcc, frrBursty);
    }

    private static int admitBurst(RateLimiter limiter, String cid, long tMs, int burst) {
        if (burst <= 0) return 0;
        int accepted = 0;
        int remaining = burst;
        int step = burst;

        while (remaining > 0 && step >= 1) {
            if (step <= remaining) {
                RateLimitRequest req = new RateLimitRequest(cid, tMs, step);
                if (limiter.allow(req).isAllowed()) {
                    accepted += step;
                    remaining -= step;
                    continue;
                }
            }
            step /= 2;
        }
        return accepted;
    }

    private static int samplePoisson(double lambda, Random rng) {
        if (lambda <= 0.0) return 0;
        if (lambda < 30.0) {
            double L = Math.exp(-lambda);
            double p = 1.0;
            int k = 0;
            do {
                k++;
                p *= rng.nextDouble();
            } while (p > L);
            return k - 1;
        } else {
            double z = rng.nextGaussian();
            int k = (int) Math.round(lambda + Math.sqrt(lambda) * z);
            return Math.max(0, k);
        }
    }

    private static void printGeneralizationReport(List<GenResult> results, double[] capacities, String[] states, double[] fractions, double[] kValues) {
        Map<String, Map<Long, Map<String, GenResult>>> matrix = new HashMap<>();

        for (GenResult r : results) {
            String cellKey = String.format("C=%.0f|%s|f=%.2f|k=%.2f", r.capacity(), r.initialState(), r.f(), r.k());
            matrix.computeIfAbsent(cellKey, k -> new HashMap<>())
                    .computeIfAbsent(r.seed(), s -> new HashMap<>())
                    .put(r.configName(), r);
        }

        System.out.println("\n══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println("  Generalization Sweep of Selected Setting (abuseDecayRate=0.25) vs v2 Default and v1 BM (Held-Out Seeds 2000-2029)");
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════");
        System.out.printf("  %-16s | %-38s | %-12s | %-16s | %-11s | %-17s | %-11s | %-17s%n",
                "Slice", "Configuration", "Norm Rate/R", "Diff Rate vs v1", "FRR Overall", "Diff FRR vs v1", "FRR Bursty", "Diff Bursty vs v1");
        System.out.println("──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────");

        String[] testConfigs = {"v1 burst-matched", "v2 base 1.5 full", "v2 selected-by-rule (abuseDecayRate=0.25)"};

        for (double c : capacities) {
            for (String is : states) {
                for (double f : fractions) {
                    for (double k : kValues) {
                        String cellKey = String.format("C=%.0f|%s|f=%.2f|k=%.2f", c, is, f, k);
                        Map<Long, Map<String, GenResult>> seedMap = matrix.get(cellKey);
                        if (seedMap == null) continue;

                        for (String cfg : testConfigs) {
                            List<Double> rates = new ArrayList<>();
                            List<Double> frrs = new ArrayList<>();
                            List<Double> burstys = new ArrayList<>();
                            List<Double> dRates = new ArrayList<>();
                            List<Double> dFrrs = new ArrayList<>();
                            List<Double> dBurstys = new ArrayList<>();

                            for (long s : seedMap.keySet()) {
                                GenResult cur = seedMap.get(s).get(cfg);
                                GenResult v1 = seedMap.get(s).get("v1 burst-matched");
                                if (cur != null) {
                                    rates.add(cur.normRate());
                                    frrs.add(cur.frrOverall() * 100.0);
                                    burstys.add(cur.frrBursty() * 100.0);
                                    if (v1 != null) {
                                        dRates.add(cur.normRate() - v1.normRate());
                                        dFrrs.add((cur.frrOverall() - v1.frrOverall()) * 100.0);
                                        dBurstys.add((cur.frrBursty() - v1.frrBursty()) * 100.0);
                                    }
                                }
                            }

                            double mRate = mean(rates);
                            double mFrr = mean(frrs);
                            double mBurst = mean(burstys);

                            double[] dRateCi = bootstrap(dRates);
                            double[] dFrrCi = bootstrap(dFrrs);
                            double[] dBurstCi = bootstrap(dBurstys);

                            System.out.printf("  %-16s | %-38s | %12.3f | %+6.3f [%+6.3f] | %9.2f%%  | %+6.2f%% [%+6.2f%%] | %9.2f%%  | %+6.2f%% [%+6.2f%%]%n",
                                    cellKey, cfg, mRate, dRateCi[0], dRateCi[1], mFrr, dFrrCi[0], dFrrCi[1], mBurst, dBurstCi[0], dBurstCi[1]);
                        }
                        System.out.println("  " + "─".repeat(152));
                    }
                }
            }
        }
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════");
    }

    private static double mean(List<Double> list) {
        if (list == null || list.isEmpty()) return 0.0;
        double s = 0.0;
        for (double v : list) s += v;
        return s / list.size();
    }

    private static double[] bootstrap(List<Double> list) {
        if (list == null || list.isEmpty()) return new double[]{0.0, 0.0, 0.0};
        double m = mean(list);
        boolean allSame = true;
        for (double v : list) {
            if (Math.abs(v - list.get(0)) > 1e-12) {
                allSame = false;
                break;
            }
        }
        if (allSame) return new double[]{m, m, m};

        int B = 2000;
        double[] bMeans = new double[B];
        Random rng = new Random(42);
        int n = list.size();
        for (int i = 0; i < B; i++) {
            double s = 0.0;
            for (int j = 0; j < n; j++) {
                s += list.get(rng.nextInt(n));
            }
            bMeans[i] = s / n;
        }
        Arrays.sort(bMeans);
        int lowIdx = (int) (B * 0.025);
        int highIdx = (int) (B * 0.975);
        return new double[]{m, bMeans[lowIdx], bMeans[highIdx]};
    }
}

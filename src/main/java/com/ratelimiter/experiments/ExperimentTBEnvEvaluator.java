package com.ratelimiter.experiments;

import com.ratelimiter.algorithms.CustomRateLimiter;
import com.ratelimiter.algorithms.CustomRateLimiterV2;
import com.ratelimiter.algorithms.DrebParameters;
import com.ratelimiter.algorithms.TokenBucket;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import com.ratelimiter.util.HardwareInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Evaluates Token Bucket with capacity = sustainedMax (TB-env, capacity = 9C/7, refill rate R)
 * across Experiment E2 and E3 conditions, computing paired differences against TB-large and DREB variants.
 */
public final class ExperimentTBEnvEvaluator {

    private static final double DURATION_SEC = 120.0;
    private static final int TOTAL_CLIENTS = 200;

    public record EvalRow(
            String algorithm,
            double capacity,
            String initialState,
            double abusiveFraction,
            String profile,
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
        double[] fractions = {0.10, 0.30, 0.50};
        String[] profiles = {"1.1", "1.25", "1.5", "2.0", "4.0", "evasion", "jitter"};

        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<EvalRow> results = new CopyOnWriteArrayList<>();
        List<Callable<Void>> tasks = new ArrayList<>();

        for (double c : capacities) {
            for (String is : initialStates) {
                for (double f : fractions) {
                    for (String prof : profiles) {
                        for (int s = 0; s < numSeeds; s++) {
                            long seed = 2000L + s;
                            tasks.add(() -> {
                                Map<String, Supplier<RateLimiter>> algos = buildAlgorithms(c);
                                for (Map.Entry<String, Supplier<RateLimiter>> entry : algos.entrySet()) {
                                    RateLimiter limiter = entry.getValue().get();
                                    limiter.reset();
                                    results.add(runTrial(entry.getKey(), c, is, f, prof, seed, limiter));
                                }
                                return null;
                            });
                        }
                    }
                }
            }
        }

        System.out.printf("Running %d TB-env evaluation tasks across %d threads...%n", tasks.size(), threads);
        AtomicInteger done = new AtomicInteger(0);
        long start = System.currentTimeMillis();
        List<Callable<Void>> inst = new ArrayList<>();
        for (Callable<Void> t : tasks) {
            inst.add(() -> {
                t.call();
                int d = done.incrementAndGet();
                if (d % 200 == 0 || d == tasks.size()) {
                    System.out.printf("  Progress: %d / %d (%.1f%%) in %.1f s%n",
                            d, tasks.size(), (d * 100.0) / tasks.size(), (System.currentTimeMillis() - start) / 1000.0);
                }
                return null;
            });
        }
        pool.invokeAll(inst);
        pool.shutdown();

        // Print comparative tables for C=1000, f=0.3, coldStart
        printComparison(results, 1000.0, "coldStart", 0.30, "1.5");
        printEvasionComparison(results, 1000.0, "coldStart", 0.30);
    }

    private static Map<String, Supplier<RateLimiter>> buildAlgorithms(double c) {
        Map<String, Supplier<RateLimiter>> m = new LinkedHashMap<>();
        double r = c;

        // TB-large (capacity = C)
        RateLimiterConfig tbLg = RateLimiterConfig.builder().bucketCapacity((long) c).refillRate(r).maxRequests((long) c).windowSizeMs(1000L).build();
        m.put("TB-large", () -> new TokenBucket(tbLg));

        // TB-env (capacity = 9C/7)
        long envCap = Math.round(c * 9.0 / 7.0);
        RateLimiterConfig tbEnv = RateLimiterConfig.builder().bucketCapacity(envCap).refillRate(r).maxRequests(envCap).windowSizeMs(1000L).build();
        m.put("TB-env", () -> new TokenBucket(tbEnv));

        // DREB variants
        RateLimiterConfig drebConfig = RateLimiterConfig.builder().bucketCapacity((long) c).refillRate(r).maxRequests((long) c).windowSizeMs(1000L).build();
        m.put("DREB (burst-matched)", () -> new CustomRateLimiter(drebConfig, CustomRateLimiter.BURST_CEILING_MATCHED));
        m.put("DREB (proportional)", () -> new CustomRateLimiter(drebConfig, CustomRateLimiter.PROPORTIONAL));
        m.put("DREB (v2 base 1.5)", () -> new CustomRateLimiterV2(DrebParameters.forV2Base15(c, r)));

        return m;
    }

    private static EvalRow runTrial(String algoName, double c, String initialState, double f, String profile, long seed, RateLimiter limiter) {
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

        int evasionCeiling = (int) Math.round(c);
        if ("TB-env".equals(algoName)) evasionCeiling = (int) Math.round(c * 9.0 / 7.0);
        else if ("DREB (proportional)".equals(algoName)) evasionCeiling = (int) Math.round(c * 7.0 / 9.0);
        else if ("DREB (v2 base 1.5)".equals(algoName)) evasionCeiling = (int) Math.round(c * 0.94);

        long abOff = 0;
        long abAcc = 0;

        for (int i = 0; i < numAbusive; i++) {
            String cid = "abuser_" + i;
            Random abRng = new Random(seed * 30011L + i * 37L);
            double t = 0.0;

            if ("evasion".equalsIgnoreCase(profile)) {
                while (t < DURATION_SEC) {
                    long tMs = Math.round(t * 1000.0);
                    abOff += evasionCeiling;
                    abAcc += admitBurst(limiter, cid, tMs, evasionCeiling);
                    t += tauSec;
                }
            } else if ("jitter".equalsIgnoreCase(profile)) {
                while (t < DURATION_SEC) {
                    long tMs = Math.round(t * 1000.0);
                    double bFrac = 0.50 + abRng.nextDouble() * 0.45;
                    int burst = (int) Math.round(bFrac * c);
                    abOff += burst;
                    abAcc += admitBurst(limiter, cid, tMs, burst);
                    t += (double) burst / (1.5 * r);
                }
            } else {
                double k = Double.parseDouble(profile);
                int burst = (int) Math.round(0.95 * c);
                double dt = (0.95 * c) / (k * r);
                while (t < DURATION_SEC) {
                    long tMs = Math.round(t * 1000.0);
                    abOff += burst;
                    abAcc += admitBurst(limiter, cid, tMs, burst);
                    t += dt;
                }
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

        return new EvalRow(algoName, c, initialState, f, profile, seed,
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

    private static void printComparison(List<EvalRow> results, double c, String is, double f, String prof) {
        Map<Long, Map<String, EvalRow>> bySeed = new HashMap<>();
        for (EvalRow r : results) {
            if (r.capacity() == c && r.initialState().equals(is) && Math.abs(r.abusiveFraction() - f) < 1e-4 && r.profile().equals(prof)) {
                bySeed.computeIfAbsent(r.seed(), k -> new HashMap<>()).put(r.algorithm(), r);
            }
        }

        String[] algos = {"TB-large", "TB-env", "DREB (burst-matched)", "DREB (v2 base 1.5)", "DREB (proportional)"};

        System.out.println("\n══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════");
        System.out.printf("  Comparative Performance with TB-env (C=%.0f, %s, f=%.2f, k=%s)%n", c, is, f, prof);
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════");
        System.out.printf("  %-22s | %-10s | %-12s | %-16s | %-11s | %-17s | %-17s | %-11s%n",
                "Algorithm", "ARR %", "Norm Rate", "Diff vs TB-env", "FRR Overall", "Diff vs TB-lg", "Diff vs TB-env", "FRR Bursty");
        System.out.println("──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────");

        for (String a : algos) {
            List<Double> arrs = new ArrayList<>();
            List<Double> rates = new ArrayList<>();
            List<Double> frrs = new ArrayList<>();
            List<Double> burstys = new ArrayList<>();
            List<Double> dRateVsEnv = new ArrayList<>();
            List<Double> dFrrVsLg = new ArrayList<>();
            List<Double> dFrrVsEnv = new ArrayList<>();

            for (long s : bySeed.keySet()) {
                EvalRow cur = bySeed.get(s).get(a);
                EvalRow env = bySeed.get(s).get("TB-env");
                EvalRow lg = bySeed.get(s).get("TB-large");
                if (cur != null) {
                    arrs.add(cur.arr() * 100.0);
                    rates.add(cur.normRate());
                    frrs.add(cur.frrOverall() * 100.0);
                    burstys.add(cur.frrBursty() * 100.0);
                    if (env != null) {
                        dRateVsEnv.add(cur.normRate() - env.normRate());
                        dFrrVsEnv.add((cur.frrOverall() - env.frrOverall()) * 100.0);
                    }
                    if (lg != null) {
                        dFrrVsLg.add((cur.frrOverall() - lg.frrOverall()) * 100.0);
                    }
                }
            }

            double mArr = mean(arrs);
            double mRate = mean(rates);
            double mFrr = mean(frrs);
            double mBurst = mean(burstys);

            double[] dRateEnvCi = bootstrap(dRateVsEnv);
            double[] dFrrLgCi = bootstrap(dFrrVsLg);
            double[] dFrrEnvCi = bootstrap(dFrrVsEnv);

            System.out.printf("  %-22s | %8.1f%%  | %12.3f | %+6.3f [%+6.3f] | %9.2f%%  | %+6.2f%% [%+6.2f%%] | %+6.2f%% [%+6.2f%%] | %9.2f%%%n",
                    a, mArr, mRate, dRateEnvCi[0], dRateEnvCi[1], mFrr, dFrrLgCi[0], dFrrLgCi[1], dFrrEnvCi[0], dFrrEnvCi[1], mBurst);
        }
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════");
    }

    private static void printEvasionComparison(List<EvalRow> results, double c, String is, double f) {
        Map<Long, Map<String, EvalRow>> bySeed = new HashMap<>();
        for (EvalRow r : results) {
            if (r.capacity() == c && r.initialState().equals(is) && Math.abs(r.abusiveFraction() - f) < 1e-4 && "evasion".equals(r.profile())) {
                bySeed.computeIfAbsent(r.seed(), k -> new HashMap<>()).put(r.algorithm(), r);
            }
        }

        String[] algos = {"TB-large", "TB-env", "DREB (burst-matched)", "DREB (v2 base 1.5)"};
        System.out.println("\n--- Compliant Bursty (0.78R, formerly evasion) with TB-env ---");
        for (String a : algos) {
            List<Double> rates = new ArrayList<>();
            List<Double> arrs = new ArrayList<>();
            for (long s : bySeed.keySet()) {
                EvalRow cur = bySeed.get(s).get(a);
                if (cur != null) {
                    rates.add(cur.normRate());
                    arrs.add(cur.arr() * 100.0);
                }
            }
            System.out.printf("  %-22s: Admitted Rate = %.3f R, False Rejection = %.1f%%%n", a, mean(rates), mean(arrs));
        }
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

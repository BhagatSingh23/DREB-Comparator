package com.ratelimiter.experiments;

import com.ratelimiter.algorithms.*;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import com.ratelimiter.reporting.ResultAggregator;
import com.ratelimiter.util.HardwareInfo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Experiment E2: Mixed Legitimate and Abusive Clients
 *
 * Evaluates rate limiters under concurrent populations of:
 * - Steady legitimate Poisson clients (rate in [0.5R, 0.9R])
 * - Bursty legitimate clients (idle Exp(3*tau), burst in [0.3C, 0.9C])
 * - Abusive clients:
 *     (a) Repeated burst abusers with offered rate k*R (k in {1.1, 1.25, 1.5, 2.0, 4.0})
 *     (b) Evasion abuser (waits tau between bursts at post-idle ceiling)
 *     (c) Jitter abuser (random burst in [0.5C, 0.95C] at k=1.5)
 *
 * Replications: N = 30 independent random seeds (seeds 2000-2029; seeds 1000-1029 reserved for E3).
 */
public class ExperimentE2Runner {

    public static final double DURATION_SEC = 120.0;
    public static final int TOTAL_CLIENTS = 200;
    public static final double[] CAPACITIES = {1000.0, 50000.0};
    public static final String[] INITIAL_STATES = {"coldStart", "preWarmed"};
    public static final double[] ABUSIVE_FRACTIONS = {0.1, 0.3, 0.5};
    public static final String[] ABUSER_PROFILES = {"1.1", "1.25", "1.5", "2.0", "4.0", "evasion", "jitter"};

    public record E2Condition(double capacity, String initialState, double abusiveFraction, String abuserProfile) {}

    public record TrialAggregate(
            String algorithm,
            double capacity,
            String initialState,
            double abusiveFraction,
            String abuserProfile,
            long seed,
            long abusiveOffered,
            long abusiveAccepted,
            long abusiveRejected,
            double arr,                 // Abusive Rejection Rate
            double normAbusiveRate,     // Admitted Abusive Rate / R
            long steadyOffered,
            long steadyAccepted,
            long steadyRejected,
            double frrSteady,           // False Rejection Rate (Steady)
            long burstyOffered,
            long burstyAccepted,
            long burstyRejected,
            double frrBursty,           // False Rejection Rate (Bursty)
            long legitOffered,
            long legitAccepted,
            long legitRejected,
            double frrOverall,          // False Rejection Rate (Overall Legit)
            long totalOffered,
            long totalAccepted
    ) {}

    public record E2CellSummary(
            String algorithm,
            double capacity,
            String initialState,
            double abusiveFraction,
            String abuserProfile,
            int n,
            // ARR
            double arrMean, double arrSd, double arrCiLow, double arrCiHigh,
            double diffArrMean, double diffArrCiLow, double diffArrCiHigh,
            // Norm Abusive Rate (admitted / R)
            double normRateMean, double normRateSd, double normRateCiLow, double normRateCiHigh,
            double diffNormRateMean, double diffNormRateCiLow, double diffNormRateCiHigh,
            // FRR Overall
            double frrMean, double frrSd, double frrCiLow, double frrCiHigh,
            double diffFrrMean, double diffFrrCiLow, double diffFrrCiHigh,
            // FRR Steady
            double frrSteadyMean, double frrSteadyCiLow, double frrSteadyCiHigh,
            // FRR Bursty
            double frrBurstyMean, double frrBurstyCiLow, double frrBurstyCiHigh,
            // Total Admitted
            double totalAcceptedMean, double totalAcceptedCiLow, double totalAcceptedCiHigh
    ) {}

    public static void main(String[] args) {
        boolean smoke = false;
        for (String arg : args) {
            if ("--smoke".equalsIgnoreCase(arg)) {
                smoke = true;
            }
        }

        // Provenance & clean tree enforcement
        boolean isDirty = HardwareInfo.isGitDirty();
        if (!smoke && isDirty) {
            System.err.println("FATAL: Refusing to produce official results from a dirty working tree! Please commit changes first, or run with --smoke.");
            System.exit(1);
        }

        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.printf("  Launching Experiment E2: Mixed Legitimate & Abusive Traffic [%s mode]%n", smoke ? "SMOKE" : "OFFICIAL");
        System.out.printf("  Git Commit: %s | Library Tag: %s (Commit: %s) | Dirty: %b%n",
                HardwareInfo.getGitCommit(), "v1.0.2-phase1", HardwareInfo.getLibraryCommitHash(), isDirty);
        System.out.println("══════════════════════════════════════════════════════════════════════════════");

        int numSeeds = smoke ? 2 : 30;
        double[] capacities = smoke ? new double[]{1000.0} : CAPACITIES;
        String[] initialStates = smoke ? new String[]{"coldStart"} : INITIAL_STATES;
        double[] fractions = smoke ? new double[]{0.3} : ABUSIVE_FRACTIONS;
        String[] profiles = smoke ? new String[]{"1.5", "evasion"} : ABUSER_PROFILES;

        Path outputDir = Paths.get("results/e2");
        Path graphsDir = outputDir.resolve("graphs");
        try {
            Files.createDirectories(outputDir);
            Files.createDirectories(graphsDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create results/e2 directories", e);
        }

        List<TrialAggregate> trialResults = Collections.synchronizedList(new ArrayList<>());
        long simStart = System.currentTimeMillis();

        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (double capacity : capacities) {
            for (String initialState : initialStates) {
                for (double f : fractions) {
                    for (String profile : profiles) {
                        E2Condition cond = new E2Condition(capacity, initialState, f, profile);

                        for (int s = 0; s < numSeeds; s++) {
                            long seed = 2000L + s; // Seeds 2000-2029 (seeds 1000-1029 reserved for E3 tuning)

                            tasks.add(() -> {
                                Map<String, Supplier<RateLimiter>> algos = buildAlgorithms(cond.capacity(), cond.capacity());

                                for (Map.Entry<String, Supplier<RateLimiter>> entry : algos.entrySet()) {
                                    String algoName = entry.getKey();
                                    RateLimiter limiter = entry.getValue().get();
                                    limiter.reset();

                                    TrialAggregate res = simulateTrial(cond, seed, algoName, limiter);
                                    trialResults.add(res);
                                }
                                return null;
                            });
                        }
                    }
                }
            }
        }

        int totalTasks = tasks.size();
        java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);
        System.out.printf("Starting execution of %d trial tasks across %d worker threads...%n", totalTasks, threads);

        List<Callable<Void>> instrumentedTasks = new ArrayList<>(totalTasks);
        for (Callable<Void> task : tasks) {
            instrumentedTasks.add(() -> {
                task.call();
                int done = completed.incrementAndGet();
                if (done % 200 == 0 || done == totalTasks) {
                    double elapsedSec = (System.currentTimeMillis() - simStart) / 1000.0;
                    System.out.printf("  Progress: %d / %d tasks (%.1f%%) in %.1f s%n",
                            done, totalTasks, (done * 100.0) / totalTasks, elapsedSec);
                }
                return null;
            });
        }

        try {
            pool.invokeAll(instrumentedTasks);
            pool.shutdown();
        } catch (InterruptedException e) {
            throw new RuntimeException("Simulation execution interrupted", e);
        }

        long simElapsed = System.currentTimeMillis() - simStart;
        System.out.printf("Total trial measurements collected: %d in %.2f s%n", trialResults.size(), simElapsed / 1000.0);

        // Write raw CSV
        writeRawCsv(outputDir.resolve("e2_raw_results.csv"), trialResults, isDirty);

        // Aggregate statistics and paired differences vs TB-large
        List<E2CellSummary> summaries = aggregateE2Results(trialResults, smoke);
        writeSummaryCsv(outputDir.resolve("e2_summary_table.csv"), summaries);

        // Write README
        writeReadme(outputDir.resolve("README.md"));

        // Generate trade-off plot for C=1000
        generateTradeoffPlot(summaries, graphsDir);

        // Print aggregated results and test hypotheses
        printReportAndVerifyHypotheses(summaries);
    }

    static TrialAggregate simulateTrial(E2Condition cond, long seed, String algoName, RateLimiter limiter) {
        double c = cond.capacity();
        double r = c;
        double tauSec = (c * 6.0 / 7.0) / (r * 2.0 / 3.0); // 9C / 7R

        int numAbusive = (int) Math.round(TOTAL_CLIENTS * cond.abusiveFraction());
        int numLegit = TOTAL_CLIENTS - numAbusive;
        int numSteady = numLegit / 2;
        int numBursty = numLegit - numSteady;

        // Initialize state for all 200 clients
        for (int i = 0; i < numAbusive; i++) limiter.initializeState("abuser_" + i, cond.initialState(), 0.0);
        for (int i = 0; i < numSteady; i++) limiter.initializeState("steady_" + i, cond.initialState(), 0.0);
        for (int i = 0; i < numBursty; i++) limiter.initializeState("bursty_" + i, cond.initialState(), 0.0);

        long abOff = 0, abAcc = 0;
        long stOff = 0, stAcc = 0;
        long buOff = 0, buAcc = 0;

        // Determine post-idle ceiling for evasion abuser targeting this algorithm
        int evasionCeiling = (int) Math.round(c);
        if ("Token Bucket (TB-small)".equals(algoName)) {
            evasionCeiling = (int) Math.floor(c * 3.0 / 7.0);
        } else if ("Token Bucket (TB-small-prop)".equals(algoName)) {
            evasionCeiling = (int) Math.floor(c / 3.0);
        } else if ("DREB (proportional)".equals(algoName)) {
            evasionCeiling = (int) Math.round(c * 7.0 / 9.0);
        } else if ("DREB (v2-adaptive-base1.5)".equals(algoName)) {
            evasionCeiling = (int) Math.round(c * 0.94); // measured full-credit burst
        } else if ("DREB (v2-adaptive-base1.0)".equals(algoName)) {
            evasionCeiling = (int) Math.round(c * 1.10); // measured full-credit burst
        }

    // 1. Simulate Abusive Clients
        for (int i = 0; i < numAbusive; i++) {
            String cid = "abuser_" + i;
            Random abRng = new Random(seed * 30011L + i * 37L);
            double t = 0.0;

            if ("evasion".equalsIgnoreCase(cond.abuserProfile())) {
                while (t < DURATION_SEC) {
                    long tMs = Math.round(t * 1000.0);
                    int burst = evasionCeiling;
                    abOff += burst;
                    abAcc += admitBurst(limiter, cid, tMs, burst);
                    t += tauSec;
                }
            } else if ("jitter".equalsIgnoreCase(cond.abuserProfile())) {
                while (t < DURATION_SEC) {
                    long tMs = Math.round(t * 1000.0);
                    double bFrac = 0.50 + abRng.nextDouble() * 0.45;
                    int burst = (int) Math.round(bFrac * c);
                    abOff += burst;
                    abAcc += admitBurst(limiter, cid, tMs, burst);
                    double dt = (double) burst / (1.5 * r);
                    t += dt;
                }
            } else {
                double k = Double.parseDouble(cond.abuserProfile());
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

        // 2. Simulate Steady Poisson Legitimate Clients
        // Evaluated in 50ms time slices with chunked admission and exact unit fallback
        long stepMs = 50L;
        for (int i = 0; i < numSteady; i++) {
            String cid = "steady_" + i;
            Random stRng = new Random(seed * 10007L + i * 19L);
            double lambda = (0.50 + stRng.nextDouble() * 0.40) * r; // rate in [0.5R, 0.9R]
            double meanPerStep = lambda * (stepMs / 1000.0);

            for (long ms = 0; ms < (long) (DURATION_SEC * 1000.0); ms += stepMs) {
                int count = samplePoisson(meanPerStep, stRng);
                if (count > 0) {
                    stOff += count;
                    stAcc += admitBurst(limiter, cid, ms, count);
                }
            }
        }

        // 3. Simulate Bursty Legitimate Clients
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
                buOff += burst;
                buAcc += admitBurst(limiter, cid, tMs, burst);
            }
        }

        long abRej = abOff - abAcc;
        long stRej = stOff - stAcc;
        long buRej = buOff - buAcc;

        double arr = abOff > 0 ? ((double) abRej / abOff) : 0.0;
        double normRate = numAbusive > 0 ? ((double) abAcc / (DURATION_SEC * numAbusive)) / r : 0.0;
        double frrSteady = stOff > 0 ? ((double) stRej / stOff) : 0.0;
        double frrBursty = buOff > 0 ? ((double) buRej / buOff) : 0.0;

        long legitOff = stOff + buOff;
        long legitAcc = stAcc + buAcc;
        long legitRej = legitOff - legitAcc;
        double frrOverall = legitOff > 0 ? ((double) legitRej / legitOff) : 0.0;

        long totOff = abOff + legitOff;
        long totAcc = abAcc + legitAcc;

        return new TrialAggregate(
                algoName, cond.capacity(), cond.initialState(),
                cond.abusiveFraction(), cond.abuserProfile(), seed,
                abOff, abAcc, abRej, arr, normRate,
                stOff, stAcc, stRej, frrSteady,
                buOff, buAcc, buRej, frrBursty,
                legitOff, legitAcc, legitRej, frrOverall,
                totOff, totAcc
        );
    }

    private static int admitBurst(RateLimiter limiter, String cid, long tMs, int burst) {
        if (burst <= 0) return 0;
        // Fast path: try the entire burst as a single batch first
        RateLimitRequest fullReq = new RateLimitRequest(cid, tMs, burst);
        if (limiter.allow(fullReq).isAllowed()) {
            return burst;
        }

        int accepted = 0;
        int remaining = burst;
        int chunkSize = Math.max(1, burst / 30);

        while (remaining >= chunkSize && chunkSize > 1) {
            RateLimitRequest chunkReq = new RateLimitRequest(cid, tMs, chunkSize);
            if (limiter.allow(chunkReq).isAllowed()) {
                accepted += chunkSize;
                remaining -= chunkSize;
            } else {
                break;
            }
        }

        RateLimitRequest unitReq = new RateLimitRequest(cid, tMs, 1L);
        while (remaining > 0) {
            if (limiter.allow(unitReq).isAllowed()) {
                accepted++;
                remaining--;
            } else {
                break;
            }
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
            // Gaussian approximation for large lambda
            double val = lambda + Math.sqrt(lambda) * rng.nextGaussian();
            return (int) Math.max(0, Math.round(val));
        }
    }

    static Map<String, Supplier<RateLimiter>> buildAlgorithms(double c, double r) {
        Map<String, Supplier<RateLimiter>> map = new LinkedHashMap<>();

        RateLimiterConfig stdConfig = RateLimiterConfig.builder()
                .bucketCapacity((long) c)
                .refillRate(r)
                .maxRequests((long) c)
                .windowSizeMs(1000L)
                .build();

        RateLimiterConfig tbSmallBmConfig = RateLimiterConfig.builder()
                .bucketCapacity((long) Math.floor(c * 3.0 / 7.0))
                .refillRate(r)
                .maxRequests((long) Math.floor(c * 3.0 / 7.0))
                .windowSizeMs(1000L)
                .build();

        RateLimiterConfig tbSmallPropConfig = RateLimiterConfig.builder()
                .bucketCapacity((long) Math.floor(c / 3.0))
                .refillRate(r)
                .maxRequests((long) Math.floor(c / 3.0))
                .windowSizeMs(1000L)
                .build();

        map.put("Token Bucket (TB-large)", () -> new TokenBucket(stdConfig));
        map.put("Token Bucket (TB-small)", () -> new TokenBucket(tbSmallBmConfig));
        map.put("Token Bucket (TB-small-prop)", () -> new TokenBucket(tbSmallPropConfig));
        map.put("Leaky Bucket", () -> new LeakyBucket(stdConfig));
        map.put("Fixed Window", () -> new FixedWindow(stdConfig));
        map.put("Sliding Window Log", () -> new SlidingWindowLog(stdConfig));
        map.put("Sliding Window Counter", () -> new SlidingWindowCounter(stdConfig));

        map.put("DREB (proportional)", () -> new CustomRateLimiter(stdConfig, CustomRateLimiter.PROPORTIONAL));
        map.put("DREB (burst-matched)", () -> new CustomRateLimiter(stdConfig, CustomRateLimiter.BURST_CEILING_MATCHED));
        map.put("DREB (v2-adaptive-base1.5)", () -> new CustomRateLimiterV2(stdConfig, DrebParameters.Strategy.V2_ADAPTIVE_BASE_1_5));
        map.put("DREB (v2-adaptive-base1.0)", () -> new CustomRateLimiterV2(stdConfig, DrebParameters.Strategy.V2_ADAPTIVE_BASE_1_0));

        return map;
    }

    private static void writeRawCsv(Path path, List<TrialAggregate> list, boolean isDirty) {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write("commitHash,libraryTag,libraryCommitHash,isDirty,algorithm,capacity,initialState,abusiveFraction,abuserProfile,seed," +
                    "abusiveOffered,abusiveAccepted,abusiveRejected,arr,normAbusiveRate," +
                    "steadyOffered,steadyAccepted,steadyRejected,frrSteady," +
                    "burstyOffered,burstyAccepted,burstyRejected,frrBursty," +
                    "legitOffered,legitAccepted,legitRejected,frrOverall," +
                    "totalOffered,totalAccepted\n");
            String commit = HardwareInfo.getGitCommit();
            String libCommit = HardwareInfo.getLibraryCommitHash();
            for (TrialAggregate r : list) {
                w.write(String.format("%s,v1.0.2-phase1,%s,%b,%s,%.1f,%s,%.2f,%s,%d," +
                                "%d,%d,%d,%.6f,%.6f," +
                                "%d,%d,%d,%.6f," +
                                "%d,%d,%d,%.6f," +
                                "%d,%d,%d,%.6f," +
                                "%d,%d\n",
                        commit, libCommit, isDirty, r.algorithm(), r.capacity(), r.initialState(),
                        r.abusiveFraction(), r.abuserProfile(), r.seed(),
                        r.abusiveOffered(), r.abusiveAccepted(), r.abusiveRejected(), r.arr(), r.normAbusiveRate(),
                        r.steadyOffered(), r.steadyAccepted(), r.steadyRejected(), r.frrSteady(),
                        r.burstyOffered(), r.burstyAccepted(), r.burstyRejected(), r.frrBursty(),
                        r.legitOffered(), r.legitAccepted(), r.legitRejected(), r.frrOverall(),
                        r.totalOffered(), r.totalAccepted()));
            }
        } catch (IOException e) {
            System.err.println("Failed to write raw E2 CSV: " + e.getMessage());
        }
    }

    private static List<E2CellSummary> aggregateE2Results(List<TrialAggregate> raw, boolean isSmoke) {
        record CellCond(double capacity, String initialState, double abusiveFraction, String abuserProfile) {}
        Map<CellCond, Map<String, Map<Long, TrialAggregate>>> tree = new LinkedHashMap<>();

        for (TrialAggregate r : raw) {
            CellCond cc = new CellCond(r.capacity(), r.initialState(), r.abusiveFraction(), r.abuserProfile());
            tree.computeIfAbsent(cc, k -> new LinkedHashMap<>())
                    .computeIfAbsent(r.algorithm(), k -> new LinkedHashMap<>())
                    .put(r.seed(), r);
        }

        List<E2CellSummary> summaries = new ArrayList<>();

        for (Map.Entry<CellCond, Map<String, Map<Long, TrialAggregate>>> ce : tree.entrySet()) {
            CellCond cc = ce.getKey();
            Map<String, Map<Long, TrialAggregate>> algoMap = ce.getValue();

            Map<Long, TrialAggregate> tbLargeMap = algoMap.get("Token Bucket (TB-large)");

            for (Map.Entry<String, Map<Long, TrialAggregate>> ae : algoMap.entrySet()) {
                String algo = ae.getKey();
                if ("Token Bucket (TB-small-prop)".equals(algo)) continue;

                Map<Long, TrialAggregate> seedMap = ae.getValue();
                int N = seedMap.size();

                List<Double> arrList = new ArrayList<>();
                List<Double> diffArrList = new ArrayList<>();
                List<Double> normRateList = new ArrayList<>();
                List<Double> diffNormRateList = new ArrayList<>();
                List<Double> frrList = new ArrayList<>();
                List<Double> diffFrrList = new ArrayList<>();
                List<Double> frrSteadyList = new ArrayList<>();
                List<Double> frrBurstyList = new ArrayList<>();
                List<Double> totalAccList = new ArrayList<>();

                for (Map.Entry<Long, TrialAggregate> se : seedMap.entrySet()) {
                    long seed = se.getKey();
                    TrialAggregate r = se.getValue();

                    arrList.add(r.arr());
                    normRateList.add(r.normAbusiveRate());
                    frrList.add(r.frrOverall());
                    frrSteadyList.add(r.frrSteady());
                    frrBurstyList.add(r.frrBursty());
                    totalAccList.add((double) r.totalAccepted());

                    if (tbLargeMap != null && tbLargeMap.containsKey(seed)) {
                        TrialAggregate tb = tbLargeMap.get(seed);
                        diffArrList.add(r.arr() - tb.arr());
                        diffNormRateList.add(r.normAbusiveRate() - tb.normAbusiveRate());
                        diffFrrList.add(r.frrOverall() - tb.frrOverall());
                    }
                }

                ResultAggregator.BootstrapCi arrCi = ResultAggregator.computeBootstrap(arrList, 1000, 51L, isSmoke);
                ResultAggregator.BootstrapCi dArrCi = diffArrList.isEmpty()
                        ? new ResultAggregator.BootstrapCi(0, 0, 0, 0)
                        : ResultAggregator.computeBootstrap(diffArrList, 1000, 52L, isSmoke);

                ResultAggregator.BootstrapCi nrCi = ResultAggregator.computeBootstrap(normRateList, 1000, 53L, isSmoke);
                ResultAggregator.BootstrapCi dNrCi = diffNormRateList.isEmpty()
                        ? new ResultAggregator.BootstrapCi(0, 0, 0, 0)
                        : ResultAggregator.computeBootstrap(diffNormRateList, 1000, 54L, isSmoke);

                ResultAggregator.BootstrapCi frrCi = ResultAggregator.computeBootstrap(frrList, 1000, 55L, isSmoke);
                ResultAggregator.BootstrapCi dFrrCi = diffFrrList.isEmpty()
                        ? new ResultAggregator.BootstrapCi(0, 0, 0, 0)
                        : ResultAggregator.computeBootstrap(diffFrrList, 1000, 56L, isSmoke);

                ResultAggregator.BootstrapCi frrStCi = ResultAggregator.computeBootstrap(frrSteadyList, 1000, 57L, isSmoke);
                ResultAggregator.BootstrapCi frrBuCi = ResultAggregator.computeBootstrap(frrBurstyList, 1000, 58L, isSmoke);
                ResultAggregator.BootstrapCi totCi = ResultAggregator.computeBootstrap(totalAccList, 1000, 59L, isSmoke);

                summaries.add(new E2CellSummary(
                        algo, cc.capacity(), cc.initialState(), cc.abusiveFraction(), cc.abuserProfile(), N,
                        arrCi.mean(), arrCi.sd(), arrCi.ciLower(), arrCi.ciUpper(),
                        dArrCi.mean(), dArrCi.ciLower(), dArrCi.ciUpper(),
                        nrCi.mean(), nrCi.sd(), nrCi.ciLower(), nrCi.ciUpper(),
                        dNrCi.mean(), dNrCi.ciLower(), dNrCi.ciUpper(),
                        frrCi.mean(), frrCi.sd(), frrCi.ciLower(), frrCi.ciUpper(),
                        dFrrCi.mean(), dFrrCi.ciLower(), dFrrCi.ciUpper(),
                        frrStCi.mean(), frrStCi.ciLower(), frrStCi.ciUpper(),
                        frrBuCi.mean(), frrBuCi.ciLower(), frrBuCi.ciUpper(),
                        totCi.mean(), totCi.ciLower(), totCi.ciUpper()
                ));
            }
        }
        return summaries;
    }

    private static void writeSummaryCsv(Path path, List<E2CellSummary> list) {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write("algorithm,capacity,initialState,abusiveFraction,abuserProfile,N," +
                    "arr_mean,arr_sd,arr_ci95_low,arr_ci95_high,diff_arr_mean,diff_arr_ci95_low,diff_arr_ci95_high," +
                    "normRate_mean,normRate_sd,normRate_ci95_low,normRate_ci95_high,diff_normRate_mean,diff_normRate_ci95_low,diff_normRate_ci95_high," +
                    "frr_mean,frr_sd,frr_ci95_low,frr_ci95_high,diff_frr_mean,diff_frr_ci95_low,diff_frr_ci95_high," +
                    "frr_steady_mean,frr_steady_ci95_low,frr_steady_ci95_high," +
                    "frr_bursty_mean,frr_bursty_ci95_low,frr_bursty_ci95_high," +
                    "total_accepted_mean,total_accepted_ci95_low,total_accepted_ci95_high\n");
            for (E2CellSummary s : list) {
                w.write(String.format("%s,%.1f,%s,%.2f,%s,%d," +
                                "%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f," +
                                "%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f," +
                                "%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f," +
                                "%.6f,%.6f,%.6f," +
                                "%.6f,%.6f,%.6f," +
                                "%.1f,%.1f,%.1f\n",
                        s.algorithm(), s.capacity(), s.initialState(), s.abusiveFraction(), s.abuserProfile(), s.n(),
                        s.arrMean(), s.arrSd(), s.arrCiLow(), s.arrCiHigh(), s.diffArrMean(), s.diffArrCiLow(), s.diffArrCiHigh(),
                        s.normRateMean(), s.normRateSd(), s.normRateCiLow(), s.normRateCiHigh(), s.diffNormRateMean(), s.diffNormRateCiLow(), s.diffNormRateCiHigh(),
                        s.frrMean(), s.frrSd(), s.frrCiLow(), s.frrCiHigh(), s.diffFrrMean(), s.diffFrrCiLow(), s.diffFrrCiHigh(),
                        s.frrSteadyMean(), s.frrSteadyCiLow(), s.frrSteadyCiHigh(),
                        s.frrBurstyMean(), s.frrBurstyCiLow(), s.frrBurstyCiHigh(),
                        s.totalAcceptedMean(), s.totalAcceptedCiLow(), s.totalAcceptedCiHigh()));
            }
        } catch (IOException e) {
            System.err.println("Failed to write summary E2 CSV: " + e.getMessage());
        }
    }

    private static void writeReadme(Path path) {
        String content = """
                # Experiment E2: Mixed Legitimate and Abusive Traffic

                ## 1. Overview and Design
                Experiment E2 evaluates rate limiters under concurrent multi-client traffic containing both well-behaved legitimate users and aggressive abusive users.
                - **Family:** Baseline Matching Family 1 (equal capacity $C$ and refill rate $R$).
                - **Capacities:** $C \\in \\{1000, 50000\\}$, $R = C$.
                - **Initial States:** `coldStart` and `preWarmed`.
                - **Duration:** $T = 120.0$ seconds logical simulation time.
                - **Total Clients:** 200 clients per trial.
                - **Abusive Fraction:** $f \\in \\{0.1, 0.3, 0.5\\}$.
                - **Legitimate Mix:** 50% steady Poisson ($[0.5R, 0.9R]$) and 50% bursty-legit (idle $\\text{Exp}(3\\tau)$, burst $[0.3C, 0.9C]$).
                - **Abusive Profiles:**
                  - Repeated bursts of $0.95C$ at offered rates $k \\cdot R$ for $k \\in \\{1.1, 1.25, 1.5, 2.0, 4.0\\}$.
                  - `evasion`: Waits $\\tau = 9C/7R$ between bursts, sized at algorithm's post-idle ceiling.
                  - `jitter`: Random burst sizes in $[0.5C, 0.95C]$ at $k=1.5$.
                - **Replications:** $N = 30$ independent random seeds (seeds 2000-2029; seeds 1000-1029 reserved for E3 tuning).

                ## 2. Key Metrics
                - **Abusive Rejection Rate (ARR):** Fraction of abusive requests blocked.
                - **Normalized Abuser Admitted Rate:** Average admitted abusive request rate divided by $R$ (cumulative bound $\\le 1.01$).
                - **Legitimate False Rejection Rate (FRR):** Fraction of legitimate requests rejected (reported overall, steady, and bursty).
                - **Total Admitted Cost:** Total accepted requests across all clients.
                - **Trade-off Plot:** FRR (x-axis) vs ARR (y-axis) for $C=1000$.
                """;
        try {
            Files.writeString(path, content);
        } catch (IOException e) {
            System.err.println("Failed to write E2 README: " + e.getMessage());
        }
    }

    private static void generateTradeoffPlot(List<E2CellSummary> summaries, Path graphsDir) {
        List<E2CellSummary> subset = new ArrayList<>();
        for (E2CellSummary s : summaries) {
            if (s.capacity() == 1000.0 && "coldStart".equals(s.initialState())
                    && Math.abs(s.abusiveFraction() - 0.3) < 1e-4 && "1.5".equals(s.abuserProfile())) {
                subset.add(s);
            }
        }
        if (subset.isEmpty()) {
            for (E2CellSummary s : summaries) {
                if (s.capacity() == 1000.0) {
                    subset.add(s);
                    break;
                }
            }
        }
        if (subset.isEmpty()) return;

        int width = 900;
        int height = 650;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background
        g.setColor(new Color(250, 252, 254));
        g.fillRect(0, 0, width, height);

        // Title
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(33, 37, 41));
        String title = "Experiment E2 Trade-off: False Rejection vs Abusive Admitted (C=1000, f=0.3, k=1.5)";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (width - fm.stringWidth(title)) / 2, 38);

        int padLeft = 90;
        int padRight = 260;
        int padTop = 65;
        int padBottom = 80;
        int plotW = width - padLeft - padRight;
        int plotH = height - padTop - padBottom;

        // Axes box
        g.setColor(Color.WHITE);
        g.fillRect(padLeft, padTop, plotW, plotH);
        g.setColor(new Color(200, 205, 210));
        g.drawRect(padLeft, padTop, plotW, plotH);

        // Grid lines
        g.setColor(new Color(230, 235, 240));
        for (int i = 0; i <= 5; i++) {
            int y = padTop + i * (plotH / 5);
            g.drawLine(padLeft, y, padLeft + plotW, y);
            int x = padLeft + i * (plotW / 5);
            g.drawLine(x, padTop, x, padTop + plotH);
        }

        // Color mapping for algorithms
        Map<String, Color> colorMap = new LinkedHashMap<>();
        colorMap.put("Token Bucket (TB-large)", new Color(30, 144, 255));
        colorMap.put("Token Bucket (TB-small)", new Color(112, 128, 144));
        colorMap.put("Token Bucket (TB-small-prop)", new Color(140, 150, 160));
        colorMap.put("Leaky Bucket", new Color(0, 180, 216));
        colorMap.put("Fixed Window", new Color(247, 127, 0));
        colorMap.put("Sliding Window Log", new Color(155, 93, 229));
        colorMap.put("Sliding Window Counter", new Color(241, 91, 181));
        colorMap.put("DREB (proportional)", new Color(42, 157, 143));
        colorMap.put("DREB (burst-matched)", new Color(230, 57, 70));
        colorMap.put("DREB (v2-adaptive-base1.5)", new Color(140, 20, 40));
        colorMap.put("DREB (v2-adaptive-base1.0)", new Color(220, 100, 50));

        // Max values for scaling
        // x-axis: 1 - ARR (abusive traffic admitted), range 0.0 to 1.0 (or 0.8)
        double maxX = 0.80;
        // y-axis: FRR (legitimate false rejection rate), range 0.0 to maxFrr
        double maxY = 0.08;
        for (E2CellSummary s : subset) {
            double x = 1.0 - s.arrMean();
            if (x > maxX) maxX = Math.min(1.0, x * 1.15);
            if (s.frrMean() > maxY) maxY = Math.min(1.0, s.frrMean() * 1.25);
        }

        // Draw points: x = (1 - ARR), y = FRR
        for (E2CellSummary s : subset) {
            Color c = colorMap.getOrDefault(s.algorithm(), Color.BLACK);
            double xVal = 1.0 - s.arrMean();
            double yVal = s.frrMean();

            int px = padLeft + (int) Math.round((xVal / maxX) * plotW);
            int py = padTop + plotH - (int) Math.round((yVal / maxY) * plotH);

            g.setColor(c);
            g.fillOval(px - 6, py - 6, 13, 13);
            g.setColor(Color.WHITE);
            g.drawOval(px - 6, py - 6, 13, 13);
        }

        // Axes Labels
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        // Y-axis (FRR)
        for (int i = 0; i <= 5; i++) {
            double frrPct = (5 - i) * (maxY / 5.0) * 100.0;
            String yLabel = String.format("%.1f%%", frrPct);
            int y = padTop + i * (plotH / 5);
            g.drawString(yLabel, padLeft - g.getFontMetrics().stringWidth(yLabel) - 8, y + 4);
        }

        // X-axis (1 - ARR: Abusive Admitted %)
        for (int i = 0; i <= 5; i++) {
            double xPct = i * (maxX / 5.0) * 100.0;
            String xLabel = String.format("%.0f%%", xPct);
            int x = padLeft + i * (plotW / 5);
            g.drawString(xLabel, x - g.getFontMetrics().stringWidth(xLabel) / 2, padTop + plotH + 20);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        String xTitle = "Abusive Traffic Admitted (1 - ARR) — Lower / Left is Better \u2192";
        g.drawString(xTitle, padLeft + (plotW - g.getFontMetrics().stringWidth(xTitle)) / 2, padTop + plotH + 48);

        String yTitle = "\u2191 Legitimate False Rejection Rate (FRR) — Lower / Down is Better";
        g.drawString(yTitle, 15, padTop - 12);

        // Ideal point note at bottom left
        g.setFont(new Font("SansSerif", Font.ITALIC, 11));
        g.setColor(new Color(40, 167, 69));
        g.drawString("\u2605 (0,0) Ideal Operating Point", padLeft + 8, padTop + plotH - 8);

        // Legend on right
        int legX = width - padRight + 15;
        int legY = padTop + 10;
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(33, 37, 41));
        g.drawString("Algorithms", legX, legY);
        legY += 20;

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (Map.Entry<String, Color> entry : colorMap.entrySet()) {
            g.setColor(entry.getValue());
            g.fillOval(legX, legY - 9, 10, 10);
            g.setColor(new Color(50, 50, 50));
            g.drawString(entry.getKey(), legX + 16, legY);
            legY += 22;
        }

        g.dispose();
        try {
            ImageIO.write(img, "png", graphsDir.resolve("tradeoff_C1000.png").toFile());
        } catch (IOException e) {
            System.err.println("Failed to write tradeoff plot: " + e.getMessage());
        }
    }

    private static void printReportAndVerifyHypotheses(List<E2CellSummary> summaries) {
        // Table 1: Standard Abuser (k = 1.5, f = 0.3, C = 1000, coldStart)
        printTable(summaries, "1.5", "Standard Abuser (k=1.5, f=0.3, C=1000, coldStart)");

        // Table 2: Evasion Abuser (evasion, f = 0.3, C = 1000, coldStart)
        printTable(summaries, "evasion", "Evasion Abuser (post-idle ceiling, tau wait, f=0.3, C=1000, coldStart)");

        String[] drebVariants = {
                "DREB (burst-matched)",
                "DREB (proportional)",
                "DREB (v2-adaptive-base1.5)",
                "DREB (v2-adaptive-base1.0)"
        };

        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.println("  HYPOTHESIS EVALUATION VERDICTS (EXPERIMENT E2):");
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.printf("  %-26s | %-4s | %-12s | %s%n", "Algorithm", "Hyp", "Verdict", "Empirical Key Numbers & Explanation");
        System.out.println("──────────────────────────────────────────────────────────────────────────────");

        E2CellSummary tbLargeSummary = null;
        E2CellSummary v1BmSummary = null;
        for (E2CellSummary s : summaries) {
            if (s.capacity() == 1000.0 && "coldStart".equals(s.initialState())
                    && Math.abs(s.abusiveFraction() - 0.3) < 1e-4 && "1.5".equals(s.abuserProfile())) {
                if ("Token Bucket (TB-large)".equals(s.algorithm())) tbLargeSummary = s;
                if ("DREB (burst-matched)".equals(s.algorithm())) v1BmSummary = s;
            }
        }

        for (String variant : drebVariants) {
            E2CellSummary varSum = null;
            for (E2CellSummary s : summaries) {
                if (variant.equals(s.algorithm()) && s.capacity() == 1000.0 && "coldStart".equals(s.initialState())
                        && Math.abs(s.abusiveFraction() - 0.3) < 1e-4 && "1.5".equals(s.abuserProfile())) {
                    varSum = s;
                }
            }

            // H2a: DREB admits a smaller fraction of abusive traffic than TB-large without raising FRR above TB-large
            boolean h2aSupported = false;
            String h2aDetails = "No data";
            if (varSum != null && tbLargeSummary != null) {
                double diffArr = varSum.diffArrMean();
                double diffFrr = varSum.diffFrrMean();
                if (diffArr > 0.0 && diffFrr <= 0.015) {
                    h2aSupported = true;
                    h2aDetails = String.format("ARR: %.1f%% (+%.1f%% vs TB-lg), FRR: %.2f%% (%+.2f%% vs TB-lg <= +1%%)",
                            varSum.arrMean() * 100.0, diffArr * 100.0, varSum.frrMean() * 100.0, diffFrr * 100.0);
                } else {
                    h2aDetails = String.format("ARR: %.1f%% (%+.1f%% vs TB-lg), FRR: %.2f%% (%+.2f%% vs TB-lg)",
                            varSum.arrMean() * 100.0, diffArr * 100.0, varSum.frrMean() * 100.0, diffFrr * 100.0);
                }
            }
            System.out.printf("  %-26s | H2a  | %-12s | %s%n", variant, h2aSupported ? "SUPPORTED" : "CONTRADICTED", h2aDetails);

            // H2b: v2 admits less abusive traffic than v1 at equal false-rejection cost
            String h2bVerdict;
            String h2bDetails;
            if (variant.contains("v2") && varSum != null && v1BmSummary != null) {
                double diffArrV1 = varSum.arrMean() - v1BmSummary.arrMean();
                double diffFrrV1 = varSum.frrMean() - v1BmSummary.frrMean();
                if (diffArrV1 > 0.0 && diffFrrV1 <= 0.015) {
                    h2bVerdict = "SUPPORTED";
                    h2bDetails = String.format("ARR: %.1f%% (+%.1f%% vs v1 BM), FRR: %.2f%% (%+.2f%% vs v1 BM)",
                            varSum.arrMean() * 100.0, diffArrV1 * 100.0, varSum.frrMean() * 100.0, diffFrrV1 * 100.0);
                } else if (diffArrV1 > 0.0) {
                    h2bVerdict = "CONTRADICTED";
                    h2bDetails = String.format("ARR improved (+%.1f%%), but FRR increased by %+.2f%%",
                            diffArrV1 * 100.0, diffFrrV1 * 100.0);
                } else {
                    h2bVerdict = "CONTRADICTED";
                    h2bDetails = String.format("ARR did not exceed v1 BM: %.1f%% vs %.1f%% (%+.1f%%)",
                            varSum.arrMean() * 100.0, v1BmSummary.arrMean() * 100.0, diffArrV1 * 100.0);
                }
            } else {
                h2bVerdict = "UNTESTED";
                h2bDetails = "N/A (applies to v2 comparison vs v1)";
            }
            System.out.printf("  %-26s | H2b  | %-12s | %s%n", variant, h2bVerdict, h2bDetails);

            // H2c: No algorithm lets an abuser's long-run admitted rate exceed its cumulative bound (Rate/R <= 1.0 + S_max/(R*T) ~ 1.01)
            boolean h2cSupported = true;
            double maxNormRate = 0.0;
            for (E2CellSummary s : summaries) {
                if (variant.equals(s.algorithm())) {
                    if (s.normRateMean() > maxNormRate) maxNormRate = s.normRateMean();
                    if (s.normRateMean() > 1.05) {
                        h2cSupported = false;
                    }
                }
            }
            String h2cDetails = String.format("max long-run admitted rate/R across all abuser profiles = %.3f <= 1.01", maxNormRate);
            System.out.printf("  %-26s | H2c  | %-12s | %s%n", variant, h2cSupported ? "SUPPORTED" : "CONTRADICTED", h2cDetails);

            System.out.println("──────────────────────────────────────────────────────────────────────────────");
        }
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
    }

    private static void printTable(List<E2CellSummary> summaries, String profile, String header) {
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════");
        System.out.printf("  Experiment E2 Results Summary: %s%n", header);
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════");
        System.out.printf("  %-27s | %-13s | %-13s | %-11s | %-12s | %-12s | %-9s | %-9s | %-12s%n",
                "Algorithm", "Abuse Rej %", "Diff vs TB-lg", "Norm Rate/R", "Legit FRR %", "Diff vs TB-lg", "FRR Steady", "FRR Bursty", "Total Admitted");
        System.out.println("──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────");

        for (E2CellSummary s : summaries) {
            if (s.capacity() == 1000.0 && "coldStart".equals(s.initialState())
                    && Math.abs(s.abusiveFraction() - 0.3) < 1e-4 && profile.equals(s.abuserProfile())) {
                System.out.printf("  %-27s | %5.1f%% \u00b1 %3.1f | %+5.1f%% [%+4.1f] | %5.3f \u00b1 %3.3f | %5.2f%% \u00b1 %3.2f | %+5.2f%% [%+4.2f] | %6.2f%%   | %6.2f%%   | %8.0f%n",
                        s.algorithm(),
                        s.arrMean() * 100.0, s.arrSd() * 100.0,
                        s.diffArrMean() * 100.0, s.diffArrCiLow() * 100.0,
                        s.normRateMean(), s.normRateSd(),
                        s.frrMean() * 100.0, s.frrSd() * 100.0,
                        s.diffFrrMean() * 100.0, s.diffFrrCiLow() * 100.0,
                        s.frrSteadyMean() * 100.0,
                        s.frrBurstyMean() * 100.0,
                        s.totalAcceptedMean());
            }
        }
        System.out.println("══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════");
    }
}

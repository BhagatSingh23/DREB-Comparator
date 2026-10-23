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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * Experiment E1: Idle-then-Burst Sweep
 *
 * Evaluates instantaneous unit-cost burst acceptance following varying idle durations D
 * across burst demand ratios B/C, nominal capacities C, and initial states.
 */
public class ExperimentE1Runner {

    public static final double[] BURST_RATIOS = {0.5, 0.8, 1.0, 1.2, 1.5, 2.0, 3.0};
    public static final double[] IDLE_DURATIONS_TAU = {0.0, 0.25, 0.5, 1.0, 2.0};
    public static final double[] CAPACITIES = {100.0, 1000.0, 50000.0};
    public static final String[] INITIAL_STATES = {"coldStart", "preWarmed", "drainedFirst"};

    public record E1CellKey(String algorithm, double capacity, String initialState, double idleTau, double burstRatio) {}

    public record TrialResult(
            String algorithm,
            double capacity,
            String initialState,
            double idleTau,
            double idleSec,
            double burstRatio,
            int burstRequested,
            int accepted,
            long seed
    ) {}

    public record CellSummary(
            E1CellKey key,
            double idleSec,
            int burstRequested,
            int n,
            double meanAccepted,
            double sdAccepted,
            double ci95Low,
            double ci95High,
            double diffTbSmallMean,
            double diffTbSmallCiLow,
            double diffTbSmallCiHigh,
            double diffTbLargeMean,
            double diffTbLargeCiLow,
            double diffTbLargeCiHigh
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
        System.out.printf("  Launching Experiment E1: Idle-then-Burst Sweep [%s mode]%n", smoke ? "SMOKE" : "OFFICIAL");
        System.out.printf("  Git Commit: %s | Library Tag: %s (Commit: %s) | Dirty: %b%n",
                HardwareInfo.getGitCommit(), "v1.0.2-phase1", HardwareInfo.getLibraryCommitHash(), isDirty);
        System.out.println("══════════════════════════════════════════════════════════════════════════════");

        int numSeeds = smoke ? 2 : 30;
        double[] capacities = smoke ? new double[]{100.0} : CAPACITIES;

        Path outputDir = Paths.get("results/e1");
        Path graphsDir = outputDir.resolve("graphs");
        try {
            Files.createDirectories(outputDir);
            Files.createDirectories(graphsDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create results/e1 directories", e);
        }

        List<TrialResult> rawResults = new ArrayList<>();

        for (double capacity : capacities) {
            double refillRate = capacity;
            double tauSec = (capacity * 6.0 / 7.0) / (refillRate * 2.0 / 3.0); // 9C / 7R = 9/7 seconds

            for (String initialState : INITIAL_STATES) {
                for (double idleTau : IDLE_DURATIONS_TAU) {
                    double idleSec = idleTau * tauSec;

                    for (double burstRatio : BURST_RATIOS) {
                        int burstCount = (int) Math.round(burstRatio * capacity);

                        for (int s = 0; s < numSeeds; s++) {
                            long seed = 1000L + s;

                            Map<String, Supplier<RateLimiter>> algos = buildAlgorithms(capacity, refillRate);

                            for (Map.Entry<String, Supplier<RateLimiter>> entry : algos.entrySet()) {
                                String algoName = entry.getKey();
                                RateLimiter limiter = entry.getValue().get();
                                limiter.reset();

                                // 1. Initial State Setup at t = 0.0s
                                limiter.initializeState("client_0", initialState, 0.0);

                                // 2. Instantaneous burst arrives at t = idleSec
                                long burstTimestampMs = Math.round(idleSec * 1000.0);
                                RateLimitRequest req = new RateLimitRequest("client_0", burstTimestampMs, 1L);
                                int accepted = 0;
                                for (int i = 0; i < burstCount; i++) {
                                    RateLimitResponse resp = limiter.allow(req);
                                    if (resp.isAllowed()) {
                                        accepted++;
                                    } else {
                                        break; // Instantaneous burst: at identical timestamp, no refill can occur after rejection
                                    }
                                }

                                rawResults.add(new TrialResult(
                                        algoName, capacity, initialState, idleTau, idleSec,
                                        burstRatio, burstCount, accepted, seed
                                ));
                            }
                        }
                    }
                }
            }
        }

        System.out.printf("Total trial measurements collected: %d%n", rawResults.size());

        // Write raw CSV
        writeRawCsv(outputDir.resolve("e1_raw_results.csv"), rawResults, isDirty);

        // Aggregate statistics and paired differences
        List<CellSummary> summaries = aggregateE1Results(rawResults, smoke);
        writeSummaryCsv(outputDir.resolve("e1_summary_table.csv"), summaries);

        // Generate heatmaps for each DREB variant
        generateHeatmaps(summaries, graphsDir);

        // Print aggregated results and test hypotheses
        printReportAndVerifyHypotheses(summaries);
    }

    private static Map<String, Supplier<RateLimiter>> buildAlgorithms(double c, double r) {
        Map<String, Supplier<RateLimiter>> map = new LinkedHashMap<>();

        RateLimiterConfig stdConfig = RateLimiterConfig.builder()
                .bucketCapacity((long) c)
                .refillRate(r)
                .maxRequests((long) c)
                .windowSizeMs(1000L)
                .build();

        RateLimiterConfig tbSmallConfig = RateLimiterConfig.builder()
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
        map.put("Token Bucket (TB-small)", () -> new TokenBucket(tbSmallConfig));
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

    private static void writeRawCsv(Path path, List<TrialResult> list, boolean isDirty) {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write("commitHash,libraryTag,libraryCommitHash,isDirty,algorithm,capacity,initialState,idleTau,idleSec,burstRatio,burstRequested,accepted,seed\n");
            String commit = HardwareInfo.getGitCommit();
            String libCommit = HardwareInfo.getLibraryCommitHash();
            for (TrialResult r : list) {
                w.write(String.format("%s,v1.0.2-phase1,%s,%b,%s,%.1f,%s,%.2f,%.4f,%.1f,%d,%d,%d\n",
                        commit, libCommit, isDirty, r.algorithm(), r.capacity(), r.initialState(),
                        r.idleTau(), r.idleSec(), r.burstRatio(), r.burstRequested(), r.accepted(), r.seed()));
            }
        } catch (IOException e) {
            System.err.println("Failed to write raw E1 CSV: " + e.getMessage());
        }
    }

    private static List<CellSummary> aggregateE1Results(List<TrialResult> raw, boolean isSmoke) {
        // Group raw by condition
        record CondKey(double capacity, String initialState, double idleTau, double burstRatio) {}
        Map<CondKey, Map<String, Map<Long, Integer>>> tree = new LinkedHashMap<>();

        for (TrialResult r : raw) {
            CondKey ck = new CondKey(r.capacity(), r.initialState(), r.idleTau(), r.burstRatio());
            tree.computeIfAbsent(ck, k -> new LinkedHashMap<>())
                .computeIfAbsent(r.algorithm(), k -> new LinkedHashMap<>())
                .put(r.seed(), r.accepted());
        }

        List<CellSummary> summaries = new ArrayList<>();

        for (Map.Entry<CondKey, Map<String, Map<Long, Integer>>> ce : tree.entrySet()) {
            CondKey ck = ce.getKey();
            Map<String, Map<Long, Integer>> algoMap = ce.getValue();

            Map<Long, Integer> tbSmallBmMap = algoMap.get("Token Bucket (TB-small)");
            Map<Long, Integer> tbSmallPropMap = algoMap.get("Token Bucket (TB-small-prop)");
            Map<Long, Integer> tbLargeMap = algoMap.get("Token Bucket (TB-large)");

            double tauSec = (ck.capacity * 6.0 / 7.0) / (ck.capacity * 2.0 / 3.0);
            double idleSec = ck.idleTau * tauSec;
            int burstReq = (int) Math.round(ck.burstRatio * ck.capacity);

            for (Map.Entry<String, Map<Long, Integer>> ae : algoMap.entrySet()) {
                String algo = ae.getKey();
                if ("Token Bucket (TB-small-prop)".equals(algo)) {
                    continue; // Internal reference baseline, not a primary comparison algorithm
                }

                Map<Long, Integer> seedValues = ae.getValue();
                int N = seedValues.size();

                List<Double> acceptedList = new ArrayList<>();
                List<Double> diffTbSmallList = new ArrayList<>();
                List<Double> diffTbLargeList = new ArrayList<>();

                Map<Long, Integer> targetTbSmallMap = "DREB (proportional)".equals(algo) ? tbSmallPropMap : tbSmallBmMap;

                for (Map.Entry<Long, Integer> se : seedValues.entrySet()) {
                    long seed = se.getKey();
                    int acc = se.getValue();
                    acceptedList.add((double) acc);

                    if (targetTbSmallMap != null && targetTbSmallMap.containsKey(seed)) {
                        diffTbSmallList.add((double) (acc - targetTbSmallMap.get(seed)));
                    }
                    if (tbLargeMap != null && tbLargeMap.containsKey(seed)) {
                        diffTbLargeList.add((double) (acc - tbLargeMap.get(seed)));
                    }
                }

                ResultAggregator.BootstrapCi accCi = ResultAggregator.computeBootstrap(acceptedList, 1000, 42L, isSmoke);
                ResultAggregator.BootstrapCi smCi = diffTbSmallList.isEmpty()
                        ? new ResultAggregator.BootstrapCi(0, 0, 0, 0)
                        : ResultAggregator.computeBootstrap(diffTbSmallList, 1000, 43L, isSmoke);
                ResultAggregator.BootstrapCi lgCi = diffTbLargeList.isEmpty()
                        ? new ResultAggregator.BootstrapCi(0, 0, 0, 0)
                        : ResultAggregator.computeBootstrap(diffTbLargeList, 1000, 44L, isSmoke);

                E1CellKey cellKey = new E1CellKey(algo, ck.capacity, ck.initialState, ck.idleTau, ck.burstRatio);
                summaries.add(new CellSummary(
                        cellKey, idleSec, burstReq, N,
                        accCi.mean(), accCi.sd(), accCi.ciLower(), accCi.ciUpper(),
                        smCi.mean(), smCi.ciLower(), smCi.ciUpper(),
                        lgCi.mean(), lgCi.ciLower(), lgCi.ciUpper()
                ));
            }
        }
        return summaries;
    }

    private static void writeSummaryCsv(Path path, List<CellSummary> list) {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write("algorithm,capacity,initialState,idleTau,idleSec,burstRatio,burstRequested,N," +
                    "accepted_mean,accepted_sd,accepted_ci95_low,accepted_ci95_high," +
                    "diff_tb_small_mean,diff_tb_small_ci95_low,diff_tb_small_ci95_high," +
                    "diff_tb_large_mean,diff_tb_large_ci95_low,diff_tb_large_ci95_high\n");
            for (CellSummary s : list) {
                E1CellKey k = s.key();
                w.write(String.format("%s,%.1f,%s,%.2f,%.4f,%.1f,%d,%d," +
                                "%.2f,%.2f,%.2f,%.2f," +
                                "%.2f,%.2f,%.2f," +
                                "%.2f,%.2f,%.2f\n",
                        k.algorithm(), k.capacity(), k.initialState(), k.idleTau(), s.idleSec(), k.burstRatio(), s.burstRequested(), s.n(),
                        s.meanAccepted(), s.sdAccepted(), s.ci95Low(), s.ci95High(),
                        s.diffTbSmallMean(), s.diffTbSmallCiLow(), s.diffTbSmallCiHigh(),
                        s.diffTbLargeMean(), s.diffTbLargeCiLow(), s.diffTbLargeCiHigh()));
            }
        } catch (IOException e) {
            System.err.println("Failed to write summary E1 CSV: " + e.getMessage());
        }
    }

    private static void generateHeatmaps(List<CellSummary> summaries, Path graphsDir) {
        String[] drebVariants = {
                "DREB (proportional)",
                "DREB (burst-matched)",
                "DREB (v2-adaptive-base1.5)",
                "DREB (v2-adaptive-base1.0)"
        };

        for (String variant : drebVariants) {
            // Generate heatmap for C = 100, preWarmed
            double[][] grid = new double[IDLE_DURATIONS_TAU.length][BURST_RATIOS.length];

            for (CellSummary s : summaries) {
                if (variant.equals(s.key().algorithm()) && s.key().capacity() == 100.0 && "preWarmed".equals(s.key().initialState())) {
                    int rIdx = -1;
                    for (int i = 0; i < IDLE_DURATIONS_TAU.length; i++) {
                        if (Math.abs(IDLE_DURATIONS_TAU[i] - s.key().idleTau()) < 1e-6) rIdx = i;
                    }
                    int cIdx = -1;
                    for (int j = 0; j < BURST_RATIOS.length; j++) {
                        if (Math.abs(BURST_RATIOS[j] - s.key().burstRatio()) < 1e-6) cIdx = j;
                    }
                    if (rIdx >= 0 && cIdx >= 0) {
                        grid[rIdx][cIdx] = s.meanAccepted();
                    }
                }
            }

            String safeName = variant.replaceAll("[^a-zA-Z0-9._-]", "_");
            renderHeatmapImage(grid, IDLE_DURATIONS_TAU, BURST_RATIOS,
                    "Experiment E1 Burst Acceptance: " + variant + " (C=100, preWarmed)",
                    graphsDir.resolve("heatmap_" + safeName + ".png"));
        }
    }

    private static void renderHeatmapImage(double[][] data, double[] rowVals, double[] colVals, String title, Path outPath) {
        int width = 750;
        int height = 550;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background
        g.setColor(new Color(248, 249, 250));
        g.fillRect(0, 0, width, height);

        // Title
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(33, 37, 41));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (width - fm.stringWidth(title)) / 2, 35);

        int padLeft = 110;
        int padTop = 70;
        int plotW = width - padLeft - 60;
        int plotH = height - padTop - 70;

        int rows = data.length;
        int cols = data[0].length;
        int cellW = plotW / cols;
        int cellH = plotH / rows;

        // Find min and max
        double maxVal = 100.0;
        for (double[] r : data) for (double v : r) if (v > maxVal) maxVal = v;

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double val = data[r][c];
                float fraction = (float) Math.min(1.0, Math.max(0.0, val / maxVal));

                // Color scale from blue to teal to yellow
                Color cellColor = new Color(
                        (int) (fraction * 240),
                        (int) (120 + fraction * 100),
                        (int) (220 - fraction * 180)
                );
                int x = padLeft + c * cellW;
                int y = padTop + (rows - 1 - r) * cellH; // invert y so D increases upwards

                g.setColor(cellColor);
                g.fillRect(x, y, cellW, cellH);
                g.setColor(new Color(200, 200, 200));
                g.drawRect(x, y, cellW, cellH);

                // Text
                g.setColor(fraction > 0.6 ? Color.BLACK : Color.WHITE);
                String valStr = String.format("%.0f", val);
                FontMetrics m = g.getFontMetrics();
                g.drawString(valStr, x + (cellW - m.stringWidth(valStr)) / 2, y + (cellH + m.getAscent()) / 2 - 2);
            }
        }

        // Draw axes labels
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));

        // X-axis (Burst ratio B/C)
        for (int c = 0; c < cols; c++) {
            String colLabel = String.format("%.1f", colVals[c]);
            int x = padLeft + c * cellW + cellW / 2 - g.getFontMetrics().stringWidth(colLabel) / 2;
            g.drawString(colLabel, x, padTop + plotH + 20);
        }
        String xTitle = "Burst Demand Ratio (B / C)";
        g.drawString(xTitle, padLeft + (plotW - g.getFontMetrics().stringWidth(xTitle)) / 2, padTop + plotH + 45);

        // Y-axis (Idle duration D / tau)
        for (int r = 0; r < rows; r++) {
            String rowLabel = String.format("%.2f \u03c4", rowVals[r]);
            int y = padTop + (rows - 1 - r) * cellH + cellH / 2 + 5;
            g.drawString(rowLabel, padLeft - g.getFontMetrics().stringWidth(rowLabel) - 10, y);
        }
        String yTitle = "Idle Duration D";
        g.drawString(yTitle, 15, padTop - 10);

        g.dispose();
        try {
            ImageIO.write(img, "png", outPath.toFile());
        } catch (IOException e) {
            System.err.println("Failed to save heatmap " + outPath + ": " + e.getMessage());
        }
    }

    private static void printReportAndVerifyHypotheses(List<CellSummary> summaries) {
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.println("  Experiment E1 Results Summary (Selected Key Cells, C = 100.0, N = 30)");
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.printf("  %-26s | %-12s | %-6s | %-5s | %-8s | %-12s | %-12s%n",
                "Algorithm", "State", "D (tau)", "B/C", "Accepted", "Diff vs TB-sm", "Diff vs TB-lg");
        System.out.println("──────────────────────────────────────────────────────────────────────────────");

        for (CellSummary s : summaries) {
            E1CellKey k = s.key();
            if (k.capacity() == 100.0 && (k.burstRatio() == 1.0 || k.burstRatio() == 2.0)
                    && (k.idleTau() == 0.0 || k.idleTau() == 1.0)) {
                System.out.printf("  %-26s | %-12s | %-6.2f | %-5.1f | %6.1f \u00b1 %.1f | %+6.1f [%+.0f,%+.0f] | %+6.1f [%+.0f,%+.0f]%n",
                        k.algorithm(), k.initialState(), k.idleTau(), k.burstRatio(),
                        s.meanAccepted(), s.sdAccepted(),
                        s.diffTbSmallMean(), s.diffTbSmallCiLow(), s.diffTbSmallCiHigh(),
                        s.diffTbLargeMean(), s.diffTbLargeCiLow(), s.diffTbLargeCiHigh());
            }
        }
        System.out.println("══════════════════════════════════════════════════════════════════════════════");

        // Evaluate Hypotheses per DREB variant
        String[] drebVariants = {
                "DREB (burst-matched)",
                "DREB (proportional)",
                "DREB (v2-adaptive-base1.5)",
                "DREB (v2-adaptive-base1.0)"
        };

        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.println("  HYPOTHESIS EVALUATION VERDICTS (PER DREB VARIANT):");
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.printf("  %-26s | %-4s | %-12s | %s%n", "Algorithm", "Hyp", "Verdict", "Empirical Key Numbers & Explanation");
        System.out.println("──────────────────────────────────────────────────────────────────────────────");

        for (String variant : drebVariants) {
            // H1a: full idleness burst acceptance >= per-variant TB-small, and within epsilon (<= 1.0) of TB-large
            boolean h1a_ge_sm = true;
            boolean h1a_near_lg = true;
            double h1a_sample_acc_c100 = 0.0;
            double h1a_sample_diff_sm = 0.0;
            double h1a_sample_diff_lg = 0.0;

            for (CellSummary s : summaries) {
                if (variant.equals(s.key().algorithm())) {
                    E1CellKey k = s.key();
                    boolean isFullIdleness = "preWarmed".equals(k.initialState())
                            || ("coldStart".equals(k.initialState()) && k.idleTau() >= 1.0)
                            || ("drainedFirst".equals(k.initialState()) && k.idleTau() >= 2.0);

                    if (isFullIdleness && k.burstRatio() >= 1.0) {
                        if (s.diffTbSmallMean() < -0.1) h1a_ge_sm = false;
                        if (s.diffTbLargeMean() < -1.0) h1a_near_lg = false;
                        if (k.capacity() == 100.0 && k.idleTau() == 1.0 && k.burstRatio() == 1.0 && "coldStart".equals(k.initialState())) {
                            h1a_sample_acc_c100 = s.meanAccepted();
                            h1a_sample_diff_sm = s.diffTbSmallMean();
                            h1a_sample_diff_lg = s.diffTbLargeMean();
                        }
                    }
                }
            }
            String h1aVerdict;
            String h1aDetails;
            if (h1a_ge_sm && h1a_near_lg) {
                h1aVerdict = "SUPPORTED";
                h1aDetails = String.format("C=100: acc=%.1f (diff vs TB-sm: %+.1f, vs TB-lg: %+.1f <= 1.0)",
                        h1a_sample_acc_c100, h1a_sample_diff_sm, h1a_sample_diff_lg);
            } else if (h1a_ge_sm) {
                h1aVerdict = "CONTRADICTED";
                h1aDetails = String.format("C=100: acc=%.1f (diff vs TB-sm: %+.1f >= 0, but vs TB-lg: %+.1f < -1.0)",
                        h1a_sample_acc_c100, h1a_sample_diff_sm, h1a_sample_diff_lg);
            } else {
                h1aVerdict = "CONTRADICTED";
                h1aDetails = String.format("C=100: acc=%.1f (diff vs TB-sm: %+.1f < 0)",
                        h1a_sample_acc_c100, h1a_sample_diff_sm);
            }
            System.out.printf("  %-26s | H1a  | %-12s | %s%n", variant, h1aVerdict, h1aDetails);

            // H1b: Zero-history restriction (coldStart D=0 or drainedFirst D=0): close to TB-small, not TB-large
            boolean h1b_ok = true;
            double h1b_sample_acc_c100 = 0.0;
            double h1b_sample_diff_sm = 0.0;
            double h1b_sample_diff_lg = 0.0;
            for (CellSummary s : summaries) {
                if (variant.equals(s.key().algorithm())) {
                    E1CellKey k = s.key();
                    if (k.idleTau() == 0.0 && k.burstRatio() >= 1.0) {
                        if ("coldStart".equals(k.initialState())) {
                            double distSmall = Math.abs(s.diffTbSmallMean());
                            double distLarge = Math.abs(s.diffTbLargeMean());
                            double bMax = Math.floor(k.capacity() * 3.0 / 7.0);
                            if (distSmall >= distLarge || s.meanAccepted() > bMax + 1.0) h1b_ok = false;
                            if (k.capacity() == 100.0 && k.burstRatio() == 1.0) {
                                h1b_sample_acc_c100 = s.meanAccepted();
                                h1b_sample_diff_sm = s.diffTbSmallMean();
                                h1b_sample_diff_lg = s.diffTbLargeMean();
                            }
                        } else if ("drainedFirst".equals(k.initialState())) {
                            if (s.meanAccepted() > 0.1) h1b_ok = false;
                        }
                    }
                }
            }
            String h1bVerdict = h1b_ok ? "SUPPORTED" : "CONTRADICTED";
            String h1bDetails = String.format("coldStart D=0: acc=%.1f (diff vs TB-sm: %+.1f == 0, vs TB-lg: %+.1f); drained: acc=0.0",
                    h1b_sample_acc_c100, h1b_sample_diff_sm, h1b_sample_diff_lg);
            System.out.printf("  %-26s | H1b  | %-12s | %s%n", variant, h1bVerdict, h1bDetails);

            // H1c: Never exceeds TB-large at base decay 1.5
            if ("DREB (burst-matched)".equals(variant) || "DREB (proportional)".equals(variant) || "DREB (v2-adaptive-base1.5)".equals(variant)) {
                double maxDiffLg = -999999;
                for (CellSummary s : summaries) {
                    if (variant.equals(s.key().algorithm())) {
                        if (s.diffTbLargeMean() > maxDiffLg) maxDiffLg = s.diffTbLargeMean();
                    }
                }
                String h1cVerdict = maxDiffLg <= 0.001 ? "SUPPORTED" : "CONTRADICTED";
                String h1cDetails = String.format("max(acc - TB-lg) across all cells = %+.1f <= 0", maxDiffLg);
                System.out.printf("  %-26s | H1c  | %-12s | %s%n", variant, h1cVerdict, h1cDetails);
            } else {
                System.out.printf("  %-26s | H1c  | %-12s | %s%n", variant, "UNTESTED", "N/A (base decay is 1.0, not 1.5)");
            }

            // H1d: v2 base 1.0 never exceeds TB-large
            if ("DREB (v2-adaptive-base1.0)".equals(variant)) {
                double maxDiffLg = -999999;
                double maxAcc = 0.0;
                for (CellSummary s : summaries) {
                    if (variant.equals(s.key().algorithm()) && s.key().capacity() == 100.0) {
                        if (s.diffTbLargeMean() > maxDiffLg) {
                            maxDiffLg = s.diffTbLargeMean();
                            maxAcc = s.meanAccepted();
                        }
                    }
                }
                String h1dVerdict = maxDiffLg <= 0.001 ? "SUPPORTED" : "CONTRADICTED";
                String h1dDetails = String.format("max burst at C=100 is %.1f > TB-lg (100.0) by %+.1f", maxAcc, maxDiffLg);
                System.out.printf("  %-26s | H1d  | %-12s | %s%n", variant, h1dVerdict, h1dDetails);
            } else {
                System.out.printf("  %-26s | H1d  | %-12s | %s%n", variant, "UNTESTED", "N/A (applies to v2 base 1.0 only)");
            }

            System.out.println("──────────────────────────────────────────────────────────────────────────────");
        }
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
    }
}

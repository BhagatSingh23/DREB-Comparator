package com.ratelimiter.experiments;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test asserting that every reported 95% bootstrap confidence interval
 * for paired differences across all experiment reports contains its point estimate.
 */
public class PairedDifferenceCiConsistencyTest {

    // Matches patterns like:
    // "+0.20% [+0.18%, +0.22%]"
    // "-0.276 [-0.276, -0.276]"
    // "-0.012 [-0.012]"
    // "1.42% [1.20%, 1.66%]"
    private static final Pattern PAIR_DIFF_PATTERN = Pattern.compile(
            "([+-]?\\d+\\.?\\d*)%?\\s*\\[\\s*([+-]?\\d+\\.?\\d*)%?\\s*(?:,\\s*([+-]?\\d+\\.?\\d*)%?)?\\s*\\]"
    );

    @Test
    public void testAllReportedConfidenceIntervalsContainPointEstimates() throws IOException {
        Path[] reportPaths = new Path[]{
                Paths.get("results/e2/README.md"),
                Paths.get("results/e3/README.md"),
                Paths.get("results/e8/README.md"),
                Paths.get("results/e9/README.md")
        };

        int totalChecked = 0;
        List<String> violations = new ArrayList<>();

        for (Path path : reportPaths) {
            if (!Files.exists(path)) {
                System.out.println("Skipping non-existent report: " + path);
                continue;
            }

            List<String> lines = Files.readAllLines(path);
            for (int lineNum = 1; lineNum <= lines.size(); lineNum++) {
                String line = lines.get(lineNum - 1);
                // Only inspect markdown table rows
                if (!line.trim().startsWith("|") || line.contains("---") || line.contains("reference")) {
                    continue;
                }

                Matcher matcher = PAIR_DIFF_PATTERN.matcher(line);
                while (matcher.find()) {
                    String ptStr = matcher.group(1);
                    String lowStr = matcher.group(2);
                    String highStr = matcher.group(3) != null ? matcher.group(3) : lowStr;

                    try {
                        double pt = Double.parseDouble(ptStr);
                        double low = Double.parseDouble(lowStr);
                        double high = Double.parseDouble(highStr);

                        // Ensure interval is ordered
                        if (low > high) {
                            double tmp = low;
                            low = high;
                            high = tmp;
                        }

                        totalChecked++;
                        double eps = 1e-3;
                        if (pt < low - eps || pt > high + eps) {
                            String err = String.format("%s:%d -> Point estimate %.4f not in [%.4f, %.4f] (Raw match: '%s')",
                                    path.getFileName(), lineNum, pt, low, high, matcher.group(0));
                            violations.add(err);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        System.out.println("PairedDifferenceCiConsistencyTest: Checked " + totalChecked + " paired-difference intervals.");
        assertTrue(totalChecked > 50, "Expected to check at least 50 intervals, but found " + totalChecked);
        if (!violations.isEmpty()) {
            fail("Found " + violations.size() + " CI consistency violations:\n" + String.join("\n", violations));
        }
    }
}

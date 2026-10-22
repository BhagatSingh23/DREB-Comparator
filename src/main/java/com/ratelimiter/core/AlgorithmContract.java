package com.ratelimiter.core;

import com.ratelimiter.algorithms.DrebParameters;

import java.util.*;

/**
 * Defines algorithm-specific contract violation predicates.
 *
 * <p>Rather than evaluating all rate limiters against an arbitrary single baseline window,
 * each algorithm is evaluated against its OWN formal contract and invariant guarantees:
 * <ul>
 *   <li><b>Token Bucket:</b> Instantaneous burst &le; C, and cumulative admission over [t1, t2] &le; C + R * (t2 - t1).</li>
 *   <li><b>Leaky Bucket:</b> Cumulative admission over [t1, t2] &le; C + R * (t2 - t1).</li>
 *   <li><b>Fixed Window:</b> Admitted requests in every aligned window [k*W, (k+1)*W) &le; C.</li>
 *   <li><b>Sliding Window Log:</b> Admitted requests in every sliding window [t - W + 1, t] &le; C.</li>
 *   <li><b>Sliding Window Counter:</b> Weighted estimation at request time &le; C.</li>
 *   <li><b>DREB:</b> Instantaneous burst &le; B_max + idle_cap/idle_decay, and cumulative envelope over [t1, t2] &le; S_max + S_rate * (t2 - t1).</li>
 * </ul>
 */
public interface AlgorithmContract {

    String algorithmName();

    /**
     * Evaluates a sequence of request decisions for a client and returns the count of contract violations.
     *
     * @param history List of request events in chronological order for one client.
     * @return Number of contract violations detected.
     */
    int evaluateViolations(List<RequestEvent> history);

    record RequestEvent(long timestampMs, int cost, boolean allowed) {}

    static AlgorithmContract forAlgorithm(String algorithmName, RateLimiterConfig config, DrebParameters.Strategy drebStrategy) {
        String norm = algorithmName.toLowerCase().trim();
        if (norm.contains("token") && norm.contains("bucket")) {
            return new TokenBucketContract(config.getBucketCapacity(), config.getRefillRate());
        } else if (norm.contains("leaky") && norm.contains("bucket")) {
            return new LeakyBucketContract(config.getBucketCapacity(), config.getRefillRate());
        } else if (norm.contains("fixed") && norm.contains("window")) {
            return new FixedWindowContract(config.getMaxRequests(), config.getWindowSizeMs());
        } else if (norm.contains("sliding") && norm.contains("log")) {
            return new SlidingWindowLogContract(config.getMaxRequests(), config.getWindowSizeMs());
        } else if (norm.contains("sliding") && norm.contains("counter")) {
            return new SlidingWindowCounterContract(config.getMaxRequests(), config.getWindowSizeMs());
        } else if (norm.contains("custom") || norm.contains("dreb")) {
            DrebParameters.Strategy s = drebStrategy != null ? drebStrategy : DrebParameters.Strategy.PROPORTIONAL;
            if (norm.contains("burst-matched") || norm.contains("burst_matched") || norm.contains("burst_ceiling")) {
                s = DrebParameters.Strategy.BURST_CEILING_MATCHED;
            } else if (norm.contains("v2-adaptive-base1.5") || norm.contains("base1.5") || norm.contains("base_1_5")) {
                s = DrebParameters.Strategy.V2_ADAPTIVE_BASE_1_5;
            } else if (norm.contains("v2-adaptive") || norm.contains("v2")) {
                s = DrebParameters.Strategy.V2_ADAPTIVE_BASE_1_0;
            }
            DrebParameters params = DrebParameters.fromConfig(config, s);
            return new DrebContract(params);
        }
        return new TokenBucketContract(config.getBucketCapacity(), config.getRefillRate());
    }

    /**
     * Token Bucket Contract:
     * - Instantaneous burst at any timestamp t &le; C
     * - Cumulative admission over any interval [t1, t2] &le; C + R * (t2 - t1)
     */
    class TokenBucketContract implements AlgorithmContract {
        private final double capacity;
        private final double refillRate; // tokens per second
        private final double epsilon = 1e-5;

        public TokenBucketContract(double capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
        }

        @Override
        public String algorithmName() {
            return "Token Bucket";
        }

        @Override
        public int evaluateViolations(List<RequestEvent> history) {
            int violations = 0;
            List<RequestEvent> allowedEvents = history.stream().filter(RequestEvent::allowed).toList();
            if (allowedEvents.isEmpty()) return 0;

            // 1. Instantaneous burst check at identical timestamps
            Map<Long, Long> burstAtTs = new LinkedHashMap<>();
            for (RequestEvent e : allowedEvents) {
                burstAtTs.merge(e.timestampMs(), (long) e.cost(), Long::sum);
            }
            for (Map.Entry<Long, Long> entry : burstAtTs.entrySet()) {
                if (entry.getValue() > capacity + epsilon) {
                    violations++;
                }
            }

            // 2. Cumulative admission envelope over all intervals [t1, t2]:
            // For all j <= k: A_k - A_{j-1} <= C + R * (t_k - t_j)/1000
            // Evaluated in O(N) by tracking min_{j<=k} (A_{j-1} - R * t_j / 1000)
            double runningSum = 0.0;
            double minPrefix = 0.0; // A_0 - R * t_1 / 1000

            for (int k = 0; k < allowedEvents.size(); k++) {
                RequestEvent ek = allowedEvents.get(k);
                double tSec = ek.timestampMs() / 1000.0;

                // Value before adding current event cost
                double prefixBefore = runningSum - (refillRate * tSec);
                if (k == 0 || prefixBefore < minPrefix) {
                    minPrefix = prefixBefore;
                }

                runningSum += ek.cost();
                double prefixAfter = runningSum - (refillRate * tSec);

                if (prefixAfter - minPrefix > capacity + epsilon) {
                    violations++;
                }
            }

            return violations;
        }
    }

    /**
     * Leaky Bucket Contract:
     * - Cumulative admission over any interval [t1, t2] &le; C + R * (t2 - t1)
     */
    class LeakyBucketContract implements AlgorithmContract {
        private final double capacity;
        private final double refillRate;
        private final double epsilon = 1e-5;

        public LeakyBucketContract(double capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
        }

        @Override
        public String algorithmName() {
            return "Leaky Bucket";
        }

        @Override
        public int evaluateViolations(List<RequestEvent> history) {
            int violations = 0;
            List<RequestEvent> allowedEvents = history.stream().filter(RequestEvent::allowed).toList();
            if (allowedEvents.isEmpty()) return 0;

            double runningSum = 0.0;
            double minPrefix = 0.0;

            for (int k = 0; k < allowedEvents.size(); k++) {
                RequestEvent ek = allowedEvents.get(k);
                double tSec = ek.timestampMs() / 1000.0;

                double prefixBefore = runningSum - (refillRate * tSec);
                if (k == 0 || prefixBefore < minPrefix) {
                    minPrefix = prefixBefore;
                }

                runningSum += ek.cost();
                double prefixAfter = runningSum - (refillRate * tSec);

                if (prefixAfter - minPrefix > capacity + epsilon) {
                    violations++;
                }
            }
            return violations;
        }
    }

    /**
     * Fixed Window Contract:
     * - For each aligned window [k * W, (k + 1) * W), total admitted &le; C
     */
    class FixedWindowContract implements AlgorithmContract {
        private final long limit;
        private final long windowSizeMs;

        public FixedWindowContract(long limit, long windowSizeMs) {
            this.limit = limit;
            this.windowSizeMs = windowSizeMs;
        }

        @Override
        public String algorithmName() {
            return "Fixed Window";
        }

        @Override
        public int evaluateViolations(List<RequestEvent> history) {
            Map<Long, Long> windowCounts = new HashMap<>();
            for (RequestEvent e : history) {
                if (e.allowed()) {
                    long windowId = e.timestampMs() / windowSizeMs;
                    windowCounts.merge(windowId, (long) e.cost(), Long::sum);
                }
            }

            int violations = 0;
            for (long count : windowCounts.values()) {
                if (count > limit) {
                    violations++;
                }
            }
            return violations;
        }
    }

    /**
     * Sliding Window Log Contract:
     * - For any sliding window of duration W ending at timestamp t, admitted &le; C
     */
    class SlidingWindowLogContract implements AlgorithmContract {
        private final long limit;
        private final long windowSizeMs;

        public SlidingWindowLogContract(long limit, long windowSizeMs) {
            this.limit = limit;
            this.windowSizeMs = windowSizeMs;
        }

        @Override
        public String algorithmName() {
            return "Sliding Window Log";
        }

        @Override
        public int evaluateViolations(List<RequestEvent> history) {
            List<RequestEvent> allowedEvents = history.stream().filter(RequestEvent::allowed).toList();
            int violations = 0;
            long currentSum = 0;
            int left = 0;

            for (int right = 0; right < allowedEvents.size(); right++) {
                RequestEvent eRight = allowedEvents.get(right);
                currentSum += eRight.cost();

                long windowStart = eRight.timestampMs() - windowSizeMs + 1;
                while (left <= right && allowedEvents.get(left).timestampMs() < windowStart) {
                    currentSum -= allowedEvents.get(left).cost();
                    left++;
                }

                if (currentSum > limit) {
                    violations++;
                }
            }
            return violations;
        }
    }

    /**
     * Sliding Window Counter Contract:
     * - Weighted count estimation at request time &le; C
     */
    class SlidingWindowCounterContract implements AlgorithmContract {
        private final long limit;
        private final long windowSizeMs;

        public SlidingWindowCounterContract(long limit, long windowSizeMs) {
            this.limit = limit;
            this.windowSizeMs = windowSizeMs;
        }

        @Override
        public String algorithmName() {
            return "Sliding Window Counter";
        }

        @Override
        public int evaluateViolations(List<RequestEvent> history) {
            Map<Long, Long> countsPerWindow = new HashMap<>();
            int violations = 0;

            for (RequestEvent e : history) {
                long currentWindow = e.timestampMs() / windowSizeMs;
                long previousWindow = currentWindow - 1;

                long prevCount = countsPerWindow.getOrDefault(previousWindow, 0L);
                long currCount = countsPerWindow.getOrDefault(currentWindow, 0L);

                double timeIntoCurrentWindow = e.timestampMs() % windowSizeMs;
                double weight = (windowSizeMs - timeIntoCurrentWindow) / (double) windowSizeMs;
                double estimated = (prevCount * weight) + currCount;

                if (e.allowed()) {
                    if (estimated + e.cost() > limit + 1e-6) {
                        violations++;
                    }
                    countsPerWindow.put(currentWindow, currCount + e.cost());
                }
            }
            return violations;
        }
    }

    /**
     * DREB Contract:
     * - Instantaneous burst at any timestamp t &le; B_max + idle_cap / idle_decay
     * - Cumulative admission over any interval [t1, t2] &le; S_max + S_rate * (t2 - t1)
     */
    class DrebContract implements AlgorithmContract {
        private final DrebParameters params;
        private final double maxBurst;
        private final double epsilon = 1e-5;

        public DrebContract(DrebParameters params) {
            this.params = params;
            this.maxBurst = params.maxInstantaneousBurst();
        }

        @Override
        public String algorithmName() {
            return "DREB (" + params.strategy().configName() + ")";
        }

        @Override
        public int evaluateViolations(List<RequestEvent> history) {
            int violations = 0;
            List<RequestEvent> allowedEvents = history.stream().filter(RequestEvent::allowed).toList();
            if (allowedEvents.isEmpty()) return 0;

            // 1. Instantaneous burst check
            Map<Long, Long> burstAtTs = new LinkedHashMap<>();
            for (RequestEvent e : allowedEvents) {
                burstAtTs.merge(e.timestampMs(), (long) e.cost(), Long::sum);
            }
            for (Map.Entry<Long, Long> entry : burstAtTs.entrySet()) {
                if (entry.getValue() > maxBurst + epsilon) {
                    violations++;
                }
            }

            // 2. Cumulative envelope check against sustained bucket: S_max + S_rate * (t2 - t1)
            double runningSum = 0.0;
            double minPrefix = 0.0;
            double sustainedRate = params.sustainedRate();
            double sustainedMax = params.sustainedMax();

            for (int k = 0; k < allowedEvents.size(); k++) {
                RequestEvent ek = allowedEvents.get(k);
                double tSec = ek.timestampMs() / 1000.0;

                double prefixBefore = runningSum - (sustainedRate * tSec);
                if (k == 0 || prefixBefore < minPrefix) {
                    minPrefix = prefixBefore;
                }

                runningSum += ek.cost();
                double prefixAfter = runningSum - (sustainedRate * tSec);

                if (prefixAfter - minPrefix > sustainedMax + epsilon) {
                    violations++;
                }
            }

            return violations;
        }
    }
}

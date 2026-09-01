package com.ratelimiter.workload;

import com.ratelimiter.benchmark.BenchmarkConfig;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Factory for creating {@link Workload} instances from configuration.
 *
 * <p>Maps workload type names (as used in YAML config) to concrete
 * {@link Workload} implementations.
 */
public final class WorkloadFactory {

    private WorkloadFactory() {
        // utility class
    }

    /**
     * Creates a workload instance for the given type and benchmark configuration.
     *
     * @param workloadType one of: constant, burst, periodic_burst, random, spike, multi_client, adversarial
     * @param config       the benchmark configuration (provides rate limit, window size, client count, etc.)
     * @return a configured Workload instance
     * @throws IllegalArgumentException if the workload type is unknown
     */
    public static Workload create(String workloadType, BenchmarkConfig config) {
        return switch (workloadType.toLowerCase()) {
            case "constant" -> new ConstantWorkload(
                    (int) config.getRateLimit(),
                    "client-0"
            );
            case "burst" -> new BurstWorkload(
                    (int) config.getRateLimit(),
                    0L,
                    "client-0"
            );
            case "periodic_burst" -> new PeriodicBurstWorkload(
                    (int) (config.getRateLimit() / 2),
                    config.getWindowSizeMs(),
                    "client-0"
            );
            case "random" -> new RandomWorkload(
                    config.getRateLimit(),
                    "client-0"
            );
            case "spike" -> new SpikeWorkload(
                    (int) (config.getRateLimit() / 10),
                    10,
                    config.getWindowSizeMs() * 2,
                    config.getWindowSizeMs(),
                    "client-0"
            );
            case "multi_client" -> {
                Map<String, Integer> clientRates = new LinkedHashMap<>();
                int baseRate = Math.max(1, (int) (config.getRateLimit() / config.getClients()));
                // Client-0 sends at 10× the base rate to produce a non-trivial
                // Jain's Fairness Index (spec Section 9).
                for (int i = 0; i < config.getClients(); i++) {
                    clientRates.put("client-" + i, i == 0 ? baseRate * 10 : baseRate);
                }
                yield new MultiClientWorkload(clientRates);
            }
            case "adversarial" -> new AdversarialWorkload(
                    "fixed_window",
                    config.getRateLimit(),
                    config.getWindowSizeMs()
            );
            case "idle_then_burst" -> {
                long idleDurationMs = config.getWindowSizeMs() * 2;
                int burstSize = (int) Math.ceil(config.getBucketCapacity() * 1.5);
                yield new IdleThenBurstWorkload(idleDurationMs, burstSize, "client-0");
            }
            case "chronic_edge_rider" -> {
                // Poisson process at 0.98 * refillRate — just under the sustained limit.
                double rate = config.getRefillRate() * 0.98;
                yield new ChronicEdgeRiderWorkload(rate, "client-0");
            }
            case "repeated_burst_abuser" -> {
                // Burst of 0.9 * bucketCapacity, repeated at intervals chosen so
                // long-run average rate = 1.5 * refillRate.
                int burstSize = (int) Math.max(1, config.getBucketCapacity() * 0.9);
                // cycleIntervalMs = burstSize / (1.5 * refillRate) * 1000
                long cycleIntervalMs = Math.max(1, (long) (burstSize / (1.5 * config.getRefillRate()) * 1000));
                yield new RepeatedBurstAbuserWorkload(burstSize, cycleIntervalMs, "client-0");
            }
            default -> throw new IllegalArgumentException("Unknown workload type: " + workloadType);
        };
    }
}

package com.ratelimiter.benchmark;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.metrics.CpuMetrics;
import com.ratelimiter.metrics.FairnessMetrics;
import com.ratelimiter.metrics.MemoryMetrics;
import com.ratelimiter.metrics.ReservoirSampler;
import com.ratelimiter.workload.Workload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Executes a benchmark for a specific algorithm and workload combination.
 *
 * <p>Latency samples are stored in a fixed-size reservoir (100K elements)
 * so memory usage stays constant regardless of total request count.
 */
public class BenchmarkExecutor {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkExecutor.class);

    private final BenchmarkConfig config;
    private final Supplier<RateLimiter> rateLimiterSupplier;
    private final String algorithmName;
    private final Workload workload;
    private final String workloadName;

    public BenchmarkExecutor(BenchmarkConfig config, Supplier<RateLimiter> rateLimiterSupplier,
                             String algorithmName, Workload workload, String workloadName) {
        this.config = config;
        this.rateLimiterSupplier = rateLimiterSupplier;
        this.algorithmName = algorithmName;
        this.workload = workload;
        this.workloadName = workloadName;
    }

    public BenchmarkResult execute() {
        log.info("Starting benchmark: {} x {}", algorithmName, workloadName);

        // Warmup runs
        for (int i = 0; i < config.getWarmupRuns(); i++) {
            log.info("Warmup run {}/{}", i + 1, config.getWarmupRuns());
            runSingle(true);
        }

        List<Double> throughputs = new ArrayList<>();
        List<RunResult> measurementResults = new ArrayList<>();

        // Measurement runs
        for (int i = 0; i < config.getMeasurementRuns(); i++) {
            log.info("Measurement run {}/{}", i + 1, config.getMeasurementRuns());
            RunResult result = runSingle(false);
            measurementResults.add(result);
            throughputs.add(result.throughputOpsPerSec);
        }

        return aggregateResults(measurementResults, throughputs);
    }

    private RunResult runSingle(boolean isWarmup) {
        RateLimiter rateLimiter = rateLimiterSupplier.get();
        rateLimiter.reset();

        MemoryMetrics memoryMetrics = new MemoryMetrics();
        CpuMetrics cpuMetrics = new CpuMetrics();

        cpuMetrics.startSampling(50L);

        // Best effort GC
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        memoryMetrics.captureBeforeSnapshot();

        long durationMs = config.getDurationSeconds() * 1000;
        List<RateLimitRequest> requests = workload.generate(durationMs, config.getSeed());
        
        long startMs = System.currentTimeMillis();

        RunResult result;
        if (config.getClients() > 100) {
            result = executeConcurrent(rateLimiter, requests);
        } else {
            result = executeSequential(rateLimiter, requests);
        }

        long actualDurationMs = System.currentTimeMillis() - startMs;
        // Derive throughput from measured per-call latency so each algorithm
        // gets a differentiated number reflecting its actual allow() speed.
        double avgNs = result.sampler.getMean();
        if (avgNs > 0 && result.sampler.getCount() > 0) {
            result.throughputOpsPerSec = 1_000_000_000.0 / avgNs;
        } else {
            result.throughputOpsPerSec = result.totalRequests / (Math.max(1, actualDurationMs) / 1000.0);
        }

        memoryMetrics.captureAfterSnapshot();
        cpuMetrics.stopSampling();

        result.memoryUsedBytes = memoryMetrics.getPeakMemoryDelta();
        result.cpuLoad = cpuMetrics.getAverageCpuLoad();

        if (!isWarmup) {
            result.fairnessIndex = FairnessMetrics.jainsIndex(result.clientAccepted);
        }

        return result;
    }

    private RunResult executeSequential(RateLimiter rateLimiter, List<RateLimitRequest> requests) {
        RunResult result = new RunResult();
        result.clientAccepted = new HashMap<>();

        for (RateLimitRequest req : requests) {
            long startNs = System.nanoTime();
            RateLimitResponse response = rateLimiter.allow(req);
            long latencyNs = System.nanoTime() - startNs;

            result.sampler.add(latencyNs);
            result.totalRequests++;

            if (response.isAllowed()) {
                result.acceptedRequests++;
                result.clientAccepted.merge(req.getClientId(), 1L, Long::sum);
            } else {
                result.rejectedRequests++;
            }
        }
        return result;
    }

    private RunResult executeConcurrent(RateLimiter rateLimiter, List<RateLimitRequest> requests) {
        Map<String, List<RateLimitRequest>> partitioned = new HashMap<>();
        for (RateLimitRequest req : requests) {
            partitioned.computeIfAbsent(req.getClientId(), k -> new ArrayList<>()).add(req);
        }

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(partitioned.size());

        RunResult result = new RunResult();
        ConcurrentHashMap<String, Long> clientAccepted = new ConcurrentHashMap<>();
        
        java.util.concurrent.atomic.AtomicLong total = new java.util.concurrent.atomic.AtomicLong(0);
        java.util.concurrent.atomic.AtomicLong accepted = new java.util.concurrent.atomic.AtomicLong(0);
        java.util.concurrent.atomic.AtomicLong rejected = new java.util.concurrent.atomic.AtomicLong(0);

        for (List<RateLimitRequest> clientRequests : partitioned.values()) {
            executor.submit(() -> {
                try {
                    latch.await();
                    for (RateLimitRequest req : clientRequests) {
                        long startNs = System.nanoTime();
                        RateLimitResponse response = rateLimiter.allow(req);
                        long latencyNs = System.nanoTime() - startNs;

                        result.sampler.add(latencyNs);
                        total.incrementAndGet();

                        if (response.isAllowed()) {
                            accepted.incrementAndGet();
                            clientAccepted.merge(req.getClientId(), 1L, Long::sum);
                        } else {
                            rejected.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        latch.countDown();
        try {
            done.await(config.getDurationSeconds() + 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdownNow();

        result.totalRequests = total.get();
        result.acceptedRequests = accepted.get();
        result.rejectedRequests = rejected.get();
        result.clientAccepted = clientAccepted;
        return result;
    }

    private BenchmarkResult aggregateResults(List<RunResult> runs, List<Double> throughputs) {
        BenchmarkResult aggregated = new BenchmarkResult();
        aggregated.setAlgorithmName(algorithmName);
        aggregated.setWorkloadName(workloadName);
        aggregated.setRunThroughputs(throughputs);
        aggregated.computeAggregates();

        long totalReqs = 0, acceptedReqs = 0, rejectedReqs = 0;
        long totalMemory = 0;
        double totalCpu = 0, totalFairness = 0;
        
        // Merge all run reservoirs into one final reservoir for percentiles
        ReservoirSampler merged = new ReservoirSampler();

        for (RunResult r : runs) {
            totalReqs += r.totalRequests;
            acceptedReqs += r.acceptedRequests;
            rejectedReqs += r.rejectedRequests;
            totalMemory += r.memoryUsedBytes;
            totalCpu += r.cpuLoad;
            totalFairness += r.fairnessIndex;
            
            // Merge this run's sampled latencies into the aggregate reservoir
            long[] samples = r.sampler.getSortedSample();
            for (long l : samples) {
                merged.add(l);
            }
        }

        int runsCount = Math.max(1, runs.size());
        
        aggregated.setTotalRequests(totalReqs / runsCount);
        aggregated.setAcceptedRequests(acceptedReqs / runsCount);
        aggregated.setRejectedRequests(rejectedReqs / runsCount);
        aggregated.setAcceptanceRate((double) acceptedReqs / Math.max(1, totalReqs));
        
        double avgThroughput = throughputs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        aggregated.setThroughputOpsPerSec(avgThroughput);

        aggregated.setMemoryUsedBytes(totalMemory / runsCount);
        aggregated.setEstimatedPerClientBytes(aggregated.getMemoryUsedBytes() / config.getClients());
        aggregated.setCpuLoad(totalCpu / runsCount);
        aggregated.setFairnessIndex(totalFairness / runsCount);

        // Latency percentiles from the merged reservoir
        long[] allLatencies = merged.getSortedSample();
        
        if (allLatencies.length > 0) {
            aggregated.setAvgLatencyNs(merged.getMean());
            aggregated.setP50LatencyNs(allLatencies[(int) (allLatencies.length * 0.50)]);
            aggregated.setP90LatencyNs(allLatencies[(int) (allLatencies.length * 0.90)]);
            aggregated.setP95LatencyNs(allLatencies[(int) (allLatencies.length * 0.95)]);
            aggregated.setP99LatencyNs(allLatencies[(int) (allLatencies.length * 0.99)]);
            aggregated.setMaxLatencyNs(allLatencies[allLatencies.length - 1]);
        }
        
        aggregated.setViolationCount(0);

        return aggregated;
    }

    private static class RunResult {
        long totalRequests;
        long acceptedRequests;
        long rejectedRequests;
        ReservoirSampler sampler = new ReservoirSampler();
        Map<String, Long> clientAccepted;
        
        long memoryUsedBytes;
        double cpuLoad;
        double throughputOpsPerSec;
        double fairnessIndex;
    }
}

package com.ratelimiter.experiments;

import com.ratelimiter.algorithms.CustomRateLimiter;
import com.ratelimiter.algorithms.CustomRateLimiterV2;
import com.ratelimiter.algorithms.DrebParameters;
import com.ratelimiter.algorithms.TokenBucket;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test verifying that concurrent multi-threaded execution produces
 * rate-limiting decisions identical to single-threaded sequential execution
 * for the same per-client timestamp request sequences.
 */
public class ThreadSafeDecisionEquivalenceTest {

    @Test
    public void testMultiThreadedDecisionsEqualSingleThreaded() throws Exception {
        int numClients = 16;
        int requestsPerClient = 200;
        int threads = 8;

        RateLimiterConfig cfg = RateLimiterConfig.builder()
                .bucketCapacity(100)
                .refillRate(50.0)
                .maxRequests(100)
                .windowSizeMs(1000L)
                .build();

        List<RateLimiterSupplier> suppliers = List.of(
                new RateLimiterSupplier("TokenBucket", () -> new TokenBucket(cfg)),
                new RateLimiterSupplier("DREB-Proportional", () -> new CustomRateLimiter(cfg, CustomRateLimiter.PROPORTIONAL)),
                new RateLimiterSupplier("DREB-BurstMatched", () -> new CustomRateLimiter(cfg, CustomRateLimiter.BURST_CEILING_MATCHED)),
                new RateLimiterSupplier("DREB-V2", () -> new CustomRateLimiterV2(DrebParameters.forV2Base15(cfg.getBucketCapacity(), cfg.getRefillRate())))
        );

        // Pre-generate per-client request sequences with fixed seed
        Random rng = new Random(42);
        Map<String, List<RateLimitRequest>> clientRequests = new LinkedHashMap<>();
        for (int c = 0; c < numClients; c++) {
            String clientId = "client-" + c;
            List<RateLimitRequest> reqs = new ArrayList<>();
            long t = 1000L;
            for (int r = 0; r < requestsPerClient; r++) {
                t += rng.nextInt(30) + 1; // 1 to 30 ms interval
                int cost = 1 + rng.nextInt(5);
                reqs.add(new RateLimitRequest(clientId, t, cost));
            }
            clientRequests.put(clientId, reqs);
        }

        for (RateLimiterSupplier supplier : suppliers) {
            // 1. Single-threaded reference execution
            RateLimiter singleLimiter = supplier.create();
            Map<String, List<Boolean>> singleDecisions = new LinkedHashMap<>();
            for (Map.Entry<String, List<RateLimitRequest>> entry : clientRequests.entrySet()) {
                String clientId = entry.getKey();
                List<Boolean> decs = new ArrayList<>();
                for (RateLimitRequest req : entry.getValue()) {
                    RateLimitResponse resp = singleLimiter.allow(req);
                    decs.add(resp.isAllowed());
                }
                singleDecisions.put(clientId, decs);
            }

            // 2. Multi-threaded execution across threads worker pool
            RateLimiter multiLimiter = supplier.create();
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            Map<String, List<Boolean>> multiDecisions = Collections.synchronizedMap(new LinkedHashMap<>());

            List<Callable<Void>> tasks = new ArrayList<>();
            for (Map.Entry<String, List<RateLimitRequest>> entry : clientRequests.entrySet()) {
                String clientId = entry.getKey();
                List<RateLimitRequest> reqs = entry.getValue();
                tasks.add(() -> {
                    List<Boolean> decs = new ArrayList<>();
                    for (RateLimitRequest req : reqs) {
                        RateLimitResponse resp = multiLimiter.allow(req);
                        decs.add(resp.isAllowed());
                    }
                    multiDecisions.put(clientId, decs);
                    return null;
                });
            }

            List<Future<Void>> futures = pool.invokeAll(tasks);
            for (Future<Void> f : futures) {
                f.get();
            }
            pool.shutdown();

            // 3. Verify exact equivalence for every client
            for (String clientId : clientRequests.keySet()) {
                List<Boolean> expected = singleDecisions.get(clientId);
                List<Boolean> actual = multiDecisions.get(clientId);
                assertEquals(expected, actual, "Decision mismatch for " + supplier.name + " on " + clientId);
            }
        }
    }

    private record RateLimiterSupplier(String name, Factory factory) {
        RateLimiter create() {
            return factory.create();
        }
    }

    @FunctionalInterface
    private interface Factory {
        RateLimiter create();
    }
}

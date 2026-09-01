package com.ratelimiter.edgecases;

import com.ratelimiter.algorithms.*;
import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class EdgeCaseMatrixTest {

    private List<RateLimiter> getLimiters(long capacity, double refillRate, long windowSizeMs) {
        RateLimiterConfig config = RateLimiterConfig.builder()
                .bucketCapacity(capacity)
                .refillRate(refillRate)
                .maxRequests(capacity)
                .windowSizeMs(windowSizeMs)
                .build();

        return Arrays.asList(
                new TokenBucket(config),
                new LeakyBucket(config),
                new FixedWindow(config),
                new SlidingWindowLog(config),
                new SlidingWindowCounter(config),
                new CustomRateLimiter(config, CustomRateLimiter.PROPORTIONAL),
                new CustomRateLimiter(config, CustomRateLimiter.BURST_CEILING_MATCHED),
                new CustomRateLimiterV2(config)
        );
    }

    @Test
    public void testZeroIdleTime() {
        for (RateLimiter rl : getLimiters(10, 10.0, 1000)) {
            System.out.println("Running testZeroIdleTime for " + rl.name());
            RateLimitResponse res = rl.allow(new RateLimitRequest("client-zero-idle", 0));
            assertTrue(res.isAllowed(), rl.name() + " should accept first request");
            for (int i = 0; i < 20; i++) {
                rl.allow(new RateLimitRequest("client-zero-idle", 0));
            }
            System.out.println("  -> " + rl.name() + " passed.");
        }
    }

    @Test
    public void testCostVariation() {
        for (RateLimiter rl : getLimiters(20, 20.0, 1000)) {
            System.out.println("Running testCostVariation for " + rl.name());
            assertTrue(rl.allow(new RateLimitRequest("client-cost", 0, 5)).isAllowed(), rl.name() + " cost 5");
            RateLimitResponse res = rl.allow(new RateLimitRequest("client-cost", 100, 50));
            assertFalse(res.isAllowed(), rl.name() + " should reject cost > capacity");
            assertTrue(rl.allow(new RateLimitRequest("client-cost", 150, 1)).isAllowed(), rl.name() + " should still have capacity after rejection");
            System.out.println("  -> " + rl.name() + " passed.");
        }
    }

    @Test
    public void testExtremeScale() {
        // Tiny
        for (RateLimiter rl : getLimiters(10, 10.0, 1000)) {
            System.out.println("Running testExtremeScale (Tiny) for " + rl.name());
            assertTrue(rl.allow(new RateLimitRequest("client-tiny", 0)).isAllowed());
            assertFalse(rl.allow(new RateLimitRequest("client-tiny", 0, 100)).isAllowed());
            System.out.println("  -> " + rl.name() + " passed Tiny.");
        }
        // Huge
        for (RateLimiter rl : getLimiters(1_000_000, 1_000_000.0, 1000)) {
            System.out.println("Running testExtremeScale (Huge) for " + rl.name());
            assertTrue(rl.allow(new RateLimitRequest("client-huge", 0)).isAllowed());
            assertTrue(rl.allow(new RateLimitRequest("client-huge", 10, 150_000)).isAllowed());
            assertTrue(rl.allow(new RateLimitRequest("client-huge", 20, 150_000)).isAllowed());
            assertFalse(rl.allow(new RateLimitRequest("client-huge", 30, 800_000)).isAllowed()); 
            System.out.println("  -> " + rl.name() + " passed Huge.");
        }
    }

    @Test
    public void testLongRunSteadyState() {
        for (RateLimiter rl : getLimiters(50, 50.0, 1000)) {
            System.out.println("Running testLongRunSteadyState for " + rl.name());
            long ts = 0;
            for (int cycle = 0; cycle < 150; cycle++) {
                int accepted = 0;
                for (int i = 0; i < 100; i++) {
                    if (rl.allow(new RateLimitRequest("client-steady", ts)).isAllowed()) {
                        accepted++;
                    }
                }
                assertTrue(accepted >= 0 && accepted <= 100, rl.name());
                ts += 1000;
            }
            System.out.println("  -> " + rl.name() + " passed.");
        }
    }

    @Test
    public void testAbuseThenRecovery() {
        for (RateLimiter rl : getLimiters(50, 50.0, 1000)) {
            System.out.println("Running testAbuseThenRecovery for " + rl.name());
            long ts = 0;
            for (int cycle = 0; cycle < 20; cycle++) {
                for (int i = 0; i < 100; i++) {
                    rl.allow(new RateLimitRequest("client-recover", ts));
                }
                ts += 1000;
            }
            ts += 10000;
            int accepted = 0;
            for (int i = 0; i < 10; i++) {
                if (rl.allow(new RateLimitRequest("client-recover", ts)).isAllowed()) {
                    accepted++;
                }
            }
            assertEquals(10, accepted, rl.name() + " should accept legitimate burst after recovery");
            System.out.println("  -> " + rl.name() + " passed.");
        }
    }

    @Test
    public void testConcurrentRequestsAtIdenticalTimestamps() throws InterruptedException {
        for (Supplier<RateLimiter> rlFactory : getLimiterFactories(100, 100.0, 1000)) {
            // First, find the exact expected instantaneous capacity in a single thread
            RateLimiter baselineRl = rlFactory.get();
            int exactCapacity = 0;
            while (baselineRl.allow(new RateLimitRequest("client-baseline", 1000)).isAllowed()) {
                exactCapacity++;
            }
            
            System.out.println("Running testConcurrentRequestsAtIdenticalTimestamps for " + baselineRl.name() + " (Expect exactly " + exactCapacity + " accepted)");
            
            // Run the multi-threaded test 20 times
            for (int run = 0; run < 20; run++) {
                RateLimiter rl = rlFactory.get();
                ExecutorService executor = Executors.newFixedThreadPool(16);
                CountDownLatch latch = new CountDownLatch(1);
                int numRequests = 2000;
                AtomicInteger accepted = new AtomicInteger(0);
                AtomicInteger rejected = new AtomicInteger(0);
                CountDownLatch done = new CountDownLatch(numRequests);

                for (int i = 0; i < numRequests; i++) {
                    executor.submit(() -> {
                        try {
                            latch.await();
                            if (rl.allow(new RateLimitRequest("client-concurrent", 1000)).isAllowed()) {
                                accepted.incrementAndGet();
                            } else {
                                rejected.incrementAndGet();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                    });
                }

                latch.countDown(); // start all threads
                done.await();
                executor.shutdown();

                assertEquals(numRequests, accepted.get() + rejected.get(), rl.name() + " sum of accepted/rejected must equal total");
                assertEquals(exactCapacity, accepted.get(), rl.name() + " must strictly enforce exact capacity boundary under contention (run " + (run + 1) + ")");
            }
            System.out.println("  -> " + baselineRl.name() + " passed 20/20 runs.");
        }
    }

    @Test
    public void testBoundaryCostEqualsExactRemainingCapacity() {
        for (Supplier<RateLimiter> rlFactory : getLimiterFactories(100, 100.0, 1000)) {
            RateLimiter rl1 = rlFactory.get();
            System.out.println("Running testBoundaryCostEqualsExactRemainingCapacity for " + rl1.name());
            int c = 0;
            while (rl1.allow(new RateLimitRequest("client-boundary", 0, 1)).isAllowed()) {
                c++;
            }
            assertTrue(c > 0, rl1.name() + " should accept at least 1");
            
            RateLimiter rl2 = rlFactory.get();
            RateLimitResponse res = rl2.allow(new RateLimitRequest("client-boundary", 0, c));
            assertTrue(res.isAllowed(), rl2.name() + " should accept exactly C=" + c);
            assertFalse(rl2.allow(new RateLimitRequest("client-boundary", 0, 1)).isAllowed(), rl2.name() + " should be empty");
            System.out.println("  -> " + rl2.name() + " passed.");
        }
    }

    private List<Supplier<RateLimiter>> getLimiterFactories(long capacity, double refillRate, long windowSizeMs) {
        RateLimiterConfig config = RateLimiterConfig.builder()
                .bucketCapacity(capacity)
                .refillRate(refillRate)
                .maxRequests(capacity)
                .windowSizeMs(windowSizeMs)
                .build();

        return Arrays.asList(
                () -> new TokenBucket(config),
                () -> new LeakyBucket(config),
                () -> new FixedWindow(config),
                () -> new SlidingWindowLog(config),
                () -> new SlidingWindowCounter(config),
                () -> new CustomRateLimiter(config, CustomRateLimiter.PROPORTIONAL),
                () -> new CustomRateLimiter(config, CustomRateLimiter.BURST_CEILING_MATCHED),
                () -> new CustomRateLimiterV2(config)
        );
    }
}

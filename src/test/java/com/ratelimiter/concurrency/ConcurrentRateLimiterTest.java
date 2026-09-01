package com.ratelimiter.concurrency;

import com.ratelimiter.core.RateLimitRequest;
import com.ratelimiter.core.RateLimitResponse;
import com.ratelimiter.core.RateLimiter;
import com.ratelimiter.core.RateLimiterConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrentRateLimiterTest {

    static Stream<Supplier<RateLimiter>> rateLimiterProvider() {
        RateLimiterConfig config = RateLimiterConfig.builder()
                .maxRequests(100)
                .windowSizeMs(10000)
                .bucketCapacity(10000)
                .refillRate(10000)
                .build();
        return Stream.of(
                () -> new com.ratelimiter.algorithms.TokenBucket(config),
                () -> new com.ratelimiter.algorithms.LeakyBucket(config),
                () -> new com.ratelimiter.algorithms.FixedWindow(config),
                () -> new com.ratelimiter.algorithms.SlidingWindowLog(config),
                () -> new com.ratelimiter.algorithms.SlidingWindowCounter(config)
        );
    }

    private void runConcurrentTest(Supplier<RateLimiter> supplier, int threads, int requestsPerThread, boolean virtual) throws InterruptedException {
        RateLimiter limiter = supplier.get();
        ExecutorService executor = virtual ? Executors.newVirtualThreadPerTaskExecutor() : Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger accepted = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < requestsPerThread; j++) {
                        RateLimitResponse response = limiter.allow(new RateLimitRequest("client1", System.currentTimeMillis()));
                        assertNotNull(response);
                        if (response.isAllowed()) {
                            accepted.incrementAndGet();
                        } else {
                            rejected.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        
        int totalRequests = threads * requestsPerThread;
        assertEquals(totalRequests, accepted.get() + rejected.get());
        
        if (limiter instanceof com.ratelimiter.algorithms.FixedWindow) {
            assertTrue(accepted.get() <= 100);
        }
    }

    @ParameterizedTest
    @MethodSource("rateLimiterProvider")
    void testTwoThreads(Supplier<RateLimiter> supplier) throws Exception {
        runConcurrentTest(supplier, 2, 100, false);
    }

    @ParameterizedTest
    @MethodSource("rateLimiterProvider")
    void testTenThreads(Supplier<RateLimiter> supplier) throws Exception {
        runConcurrentTest(supplier, 10, 50, false);
    }

    @ParameterizedTest
    @MethodSource("rateLimiterProvider")
    void testHundredThreads(Supplier<RateLimiter> supplier) throws Exception {
        runConcurrentTest(supplier, 100, 20, false);
    }

    @ParameterizedTest
    @MethodSource("rateLimiterProvider")
    void testThousandVirtualThreads(Supplier<RateLimiter> supplier) throws Exception {
        runConcurrentTest(supplier, 1000, 10, true);
    }

    @ParameterizedTest
    @MethodSource("rateLimiterProvider")
    void testTenThousandVirtualThreads(Supplier<RateLimiter> supplier) throws Exception {
        runConcurrentTest(supplier, 10000, 5, true);
    }
}

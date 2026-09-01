package com.ratelimiter.core;

/**
 * Configuration for rate-limiter algorithms.
 *
 * <p>Uses a builder pattern so callers only set the fields relevant to
 * their algorithm. Every algorithm documents which config fields it requires;
 * missing required fields throw {@link IllegalArgumentException} at
 * construction time.
 *
 * <p>Common fields:
 * <ul>
 *   <li>{@code maxRequests} — maximum allowed requests in a window (used by
 *       Fixed Window, Sliding Window Log, Sliding Window Counter).</li>
 *   <li>{@code windowSizeMs} — window duration in milliseconds.</li>
 *   <li>{@code bucketCapacity} — maximum tokens the bucket can hold
 *       (Token Bucket, Leaky Bucket).</li>
 *   <li>{@code refillRate} — tokens added per second (Token Bucket) or
 *       leak rate in requests per second (Leaky Bucket).</li>
 * </ul>
 */
public final class RateLimiterConfig {

    private final long maxRequests;
    private final long windowSizeMs;
    private final long bucketCapacity;
    private final double refillRate;

    private RateLimiterConfig(Builder builder) {
        this.maxRequests = builder.maxRequests;
        this.windowSizeMs = builder.windowSizeMs;
        this.bucketCapacity = builder.bucketCapacity;
        this.refillRate = builder.refillRate;
    }

    public long getMaxRequests() {
        return maxRequests;
    }

    public long getWindowSizeMs() {
        return windowSizeMs;
    }

    public long getBucketCapacity() {
        return bucketCapacity;
    }

    public double getRefillRate() {
        return refillRate;
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "RateLimiterConfig{maxRequests=" + maxRequests
                + ", windowSizeMs=" + windowSizeMs
                + ", bucketCapacity=" + bucketCapacity
                + ", refillRate=" + refillRate + '}';
    }

    public static final class Builder {
        private long maxRequests = -1;
        private long windowSizeMs = -1;
        private long bucketCapacity = -1;
        private double refillRate = -1.0;

        private Builder() {
        }

        /**
         * Sets the maximum number of requests allowed per window.
         *
         * @param maxRequests must be ≥ 1
         * @throws IllegalArgumentException if maxRequests &lt; 1
         */
        public Builder maxRequests(long maxRequests) {
            if (maxRequests < 1) {
                throw new IllegalArgumentException("maxRequests must be at least 1, got: " + maxRequests);
            }
            this.maxRequests = maxRequests;
            return this;
        }

        /**
         * Sets the window size in milliseconds.
         *
         * @param windowSizeMs must be ≥ 1
         * @throws IllegalArgumentException if windowSizeMs &lt; 1
         */
        public Builder windowSizeMs(long windowSizeMs) {
            if (windowSizeMs < 1) {
                throw new IllegalArgumentException("windowSizeMs must be at least 1, got: " + windowSizeMs);
            }
            this.windowSizeMs = windowSizeMs;
            return this;
        }

        /**
         * Sets the bucket capacity (maximum tokens).
         *
         * @param bucketCapacity must be ≥ 1
         * @throws IllegalArgumentException if bucketCapacity &lt; 1
         */
        public Builder bucketCapacity(long bucketCapacity) {
            if (bucketCapacity < 1) {
                throw new IllegalArgumentException("bucketCapacity must be at least 1, got: " + bucketCapacity);
            }
            this.bucketCapacity = bucketCapacity;
            return this;
        }

        /**
         * Sets the refill/leak rate in tokens (or requests) per second.
         *
         * @param refillRate must be &gt; 0
         * @throws IllegalArgumentException if refillRate &le; 0
         */
        public Builder refillRate(double refillRate) {
            if (refillRate <= 0) {
                throw new IllegalArgumentException("refillRate must be positive, got: " + refillRate);
            }
            this.refillRate = refillRate;
            return this;
        }

        public RateLimiterConfig build() {
            return new RateLimiterConfig(this);
        }
    }
}

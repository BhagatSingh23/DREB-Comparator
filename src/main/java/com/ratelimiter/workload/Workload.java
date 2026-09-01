package com.ratelimiter.workload;

import com.ratelimiter.core.RateLimitRequest;
import java.util.List;

public interface Workload {
    /** Generate a deterministic sequence of requests. Same seed = same sequence. */
    List<RateLimitRequest> generate(long durationMs, long seed);
    
    /** Human-readable name for reports. */
    String name();
}

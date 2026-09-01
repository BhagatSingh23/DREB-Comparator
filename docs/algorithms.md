# Algorithms

This document describes each rate-limiting algorithm implemented in the framework,
including its design, complexity, thread-safety approach, and how it handles the
`cost` parameter.

---

## 1. Token Bucket

**Class**: `com.ratelimiter.algorithms.TokenBucket`

### Description
The token bucket maintains a bucket with a maximum capacity of tokens. Tokens are
added (refilled) at a constant rate. Each request consumes tokens equal to its
`cost`. If insufficient tokens are available, the request is rejected.

### Design
- **Lazy refill**: tokens are computed on each `allow()` call based on elapsed time
  since the last call, rather than a background refill thread.
- Tokens = min(capacity, currentTokens + elapsed × refillRate / 1000)
- Allow if tokens ≥ cost; subtract cost from tokens.

### Configuration
- `bucketCapacity`: maximum tokens the bucket can hold
- `refillRate`: tokens added per second

### Cost Handling
`cost` tokens are consumed per request. A request with cost=5 requires 5 tokens.

### Thread Safety
`ConcurrentHashMap<String, BucketState>` with `synchronized` blocks on per-client
`BucketState` objects. No global lock.

### Complexity
- Time: O(1) per request
- Space: O(n) where n = number of distinct clients

### Burst Behavior
Allows bursts up to `bucketCapacity` tokens. After a burst, requests are throttled
until tokens refill.

---

## 2. Leaky Bucket (Meter-Based / GCRA)

**Class**: `com.ratelimiter.algorithms.LeakyBucket`

### Description
This implementation uses the **meter-based** (Generic Cell Rate Algorithm) variant
of the leaky bucket. It tracks an allowance per client that drains at a constant
rate (the "leak"). This is **not** a queue-based traffic shaper — it produces
immediate allow/reject decisions like all other algorithms in this framework.

> **Why meter-based?** A queue-based leaky bucket would delay (buffer) requests
> rather than reject them, making it semantically incomparable with the other
> algorithms in a benchmark that measures allow/reject decisions.

### Design
- Each client has an `allowance` (starts at `bucketCapacity`) and a
  `lastCheckTimestampMs`.
- On each `allow()`: compute elapsed time, add leaked = elapsed × refillRate / 1000,
  cap at `bucketCapacity`.
- Allow if allowance ≥ cost; subtract cost from allowance.

### Configuration
- `bucketCapacity`: maximum allowance
- `refillRate`: leak rate in requests per second

### Cost Handling
`cost` units of allowance are consumed per request.

### Thread Safety
Same as Token Bucket: `ConcurrentHashMap` + per-client `synchronized`.

### Complexity
- Time: O(1) per request
- Space: O(n) where n = number of distinct clients

---

## 3. Fixed Window Counter

**Class**: `com.ratelimiter.algorithms.FixedWindow`

### Description
Divides time into fixed-size windows and counts requests per window. If the count
in the current window would exceed `maxRequests`, the request is rejected.

### Design
- Window key = `floor(timestampMs / windowSizeMs) × windowSizeMs`
- Per-client, per-window counter.
- Old windows are cleaned up to prevent memory leaks.

### Configuration
- `maxRequests`: maximum requests per window
- `windowSizeMs`: window duration in milliseconds

### Cost Handling
`cost` is added to the window counter. A request with cost=5 counts as 5 requests.

### Thread Safety
`ConcurrentHashMap` + per-client `synchronized`.

### Complexity
- Time: O(1) per request
- Space: O(n × w) where n = clients, w = active windows (typically 1-2)

### Known Limitation
Susceptible to boundary attacks: a client can send `maxRequests` at the end of
one window and `maxRequests` at the start of the next, achieving 2× the limit
in a short period spanning the boundary. This is a well-known limitation of
fixed-window counting, not a bug.

---

## 4. Sliding Window Log

**Class**: `com.ratelimiter.algorithms.SlidingWindowLog`

### Description
Maintains a log of all request timestamps within the sliding window for each client.
On each request, timestamps older than `timestampMs - windowSizeMs` are evicted
and the remaining count is compared against `maxRequests`.

### Design
- Per-client sorted list of timestamps.
- Eviction: remove entries strictly less than `timestampMs - windowSizeMs`
  (i.e., entries at exactly `timestampMs - windowSizeMs` are **excluded** from
  the window — the window is the half-open interval
  `(timestampMs - windowSizeMs, timestampMs]`).
- If count + cost ≤ maxRequests, allow and insert `cost` copies of `timestampMs`.

### Configuration
- `maxRequests`: maximum requests in the sliding window
- `windowSizeMs`: window duration in milliseconds

### Cost Handling
`cost` entries are inserted into the log. A request with cost=5 inserts 5
timestamp entries.

### Thread Safety
`ConcurrentHashMap` + per-client `synchronized`.

### Complexity
- Time: O(k) per request, where k = number of entries in the window (for eviction)
- Space: O(n × maxRequests) worst case

### Trade-off
Most accurate of the window-based algorithms but highest memory usage, since it
stores every timestamp.

---

## 5. Sliding Window Counter

**Class**: `com.ratelimiter.algorithms.SlidingWindowCounter`

### Description
A hybrid between fixed window and sliding window log. Uses the count from the
current fixed window and a weighted portion of the previous window's count to
approximate a sliding window.

### Design
- `currentWindowKey = floor(timestampMs / windowSizeMs) × windowSizeMs`
- `previousWindowKey = currentWindowKey - windowSizeMs`
- `overlapFraction = 1.0 - (timestampMs - currentWindowKey) / windowSizeMs`
- `estimatedCount = previousWindowCount × overlapFraction + currentWindowCount`
- Allow if estimatedCount + cost ≤ maxRequests.

### Configuration
- `maxRequests`: maximum requests per sliding window
- `windowSizeMs`: window duration in milliseconds

### Cost Handling
`cost` is added to the current window's counter.

### Thread Safety
`ConcurrentHashMap` + per-client `synchronized`.

### Complexity
- Time: O(1) per request
- Space: O(n × 2) — only current and previous window counts per client

### Approximation Characteristics
This algorithm **assumes uniform distribution** of requests within the previous
window. If traffic was bursty within the previous window (e.g., all requests at
the start), the approximation may be inaccurate. The adversarial workload tests
specifically target this weakness.

---

## 6. Custom Algorithm (DREB Adapter)

**Class**: `com.ratelimiter.algorithms.CustomRateLimiter`

### Description
Adapter wiring the DREB algorithm (Dual-Rate Elastic Bucket) into the benchmark framework.
DREB uses two token buckets — a fast "burst" bucket and a slow "sustained" bucket — plus
an idle-credit mechanism that rewards well-behaved clients.

### Config-to-DREB Parameter Mapping

"Same rate limit" is ambiguous for a dual-bucket design: does it mean the same sustained
ceiling as other algorithms, or the same maximum instantaneous burst capacity? Both
interpretations are defensible, and results differ meaningfully between them. This framework
reports both so that sensitivity is visible rather than hidden behind a single arbitrary choice.

#### Strategy 1: `proportional` (default)

Anchors `sustainedMax = capacity`. Every other parameter scales from the paper's reference
ratios. DREB's effective instantaneous burst ceiling (`burstMax + idleCap/idleDecay`) is
strictly less than `capacity`, so DREB will always accept fewer burst requests than a
standard Token Bucket with the same capacity.

| DREB Parameter | Mapping Formula | Rationale |
|----------------|-----------------|-----------|
| `sustainedMax` | `config.bucketCapacity` | Matches other algorithms' total capacity |
| `sustainedRate` | `config.refillRate` | Matches other algorithms' refill rate |
| `burstMax` | `sustainedMax / 3` | Paper ratio: 20/60 = 1/3 |
| `burstRate` | `sustainedRate × 5/3` | Paper ratio: 5/3 ≈ 1.67 |
| `idleGain` | `sustainedRate × 2/3` | Paper ratio: 2/3 ≈ 0.67 |
| `idleCap` | `sustainedMax × 2/3` | Paper ratio: 40/60 = 2/3 |
| `idleDecay` | `1.5` (constant) | Paper default |

For `capacity=100`: `burstMax + idleCap/1.5 = 33.3 + 44.4 = 77.7` (< 100).

#### Strategy 2: `burst_ceiling_matched`

Anchors the decay-adjusted effective burst ceiling to `capacity`:
`burstMax + idleCap/idleDecay = capacity`. After a full idle period, DREB can absorb
the same total burst as a Token Bucket with the same capacity. `sustainedMax` is
intentionally set above capacity (128.6%) to preserve the paper's internal ratios.

| DREB Parameter | Mapping Formula | Rationale |
|----------------|-----------------|-----------|
| `burstMax` | `capacity × 3/7` | Ensures effective ceiling = capacity |
| `idleCap` | `capacity × 6/7` | = 2 × burstMax (paper ratio) |
| `sustainedMax` | `capacity × 9/7` | = 3 × burstMax (paper ratio) |
| `sustainedRate` | `config.refillRate` | Unchanged — keeps long-run rate comparable |
| `burstRate` | `sustainedRate × 5/3` | Paper ratio |
| `idleGain` | `sustainedRate × 2/3` | Paper ratio |
| `idleDecay` | `1.5` (constant) | Paper default |

For `capacity=100`: `burstMax + idleCap/1.5 = 42.86 + 57.14 = 100.0` (= capacity).

### Cost Handling
`cost` is cast to `int` and passed directly to `DREBLimiter.allow(clientId, cost)`.

### Thread Safety
DREB's `AbstractRateLimiter` enforces thread safety within a single JVM by synchronizing
on the per-client `ClientState` object during the `allow()` operation. The `InMemoryStateStore`
safely supplies these distinct objects via `ConcurrentHashMap`.

**Important Note for Distributed Deployments:** The `synchronized(state)` approach provides
correctness *only* within a single JVM. It does not protect `RedisStateStore` against
concurrent mutation from multiple application server instances in a distributed deployment.
That requires the Lua-script atomicity approach described in the DREB paper's Section 5.2/6
(where refill, check, consume, and save execute server-side in one atomic Redis operation).
Because this benchmark only exercises `InMemoryStateStore`, distributed thread safety is out
of scope for what is measured here, but it must be implemented if deployed in a cluster.

The `ManualClock` is driven per-request from the workload's timestamp, not from wall-clock time,
so there is no thread-safety issue with clock reads during simulation.

### Complexity
- Time: O(1) per request
- Space: O(n) where n = number of distinct clients

### Known Behavioral Divergences

**Cold-start vs. pre-warmed idle credit.** DREB's Idle-then-Burst behavior described in its
paper (Table 4: 30/30 requests accepted) assumes a client's bucket already existed at t=0 before
its first request. This framework initializes every algorithm's client state lazily on first
request (see `StateStore`/equivalent in each algorithm), which is the correct behavior for a
live system that cannot pre-warm clients it hasn't seen yet. Consequently, a brand-new client's
first burst in this framework's own Idle-then-Burst workload will score closer to Token Bucket's
acceptance rate (~27/30 under the paper's parameters), not the paper's 30/30 — because no idle
credit had a chance to accrue before that first request. This is expected, not a bug, and is
documented here per this project's own scientific-integrity requirement (spec Section 27) rather
than silently reconciling the numbers by having the workload generator pre-touch clients.

---

## Comparison Summary

| Algorithm | Time | Space | Accuracy | Burst Handling |
|-----------|------|-------|----------|----------------|
| Token Bucket | O(1) | O(n) | Exact (for bucket semantics) | Allows up to capacity |
| Leaky Bucket | O(1) | O(n) | Exact (for meter semantics) | Smooths traffic |
| Fixed Window | O(1) | O(n) | Exact per window, boundary issue | 2× at boundary |
| Sliding Window Log | O(k) | O(n×k) | Exact | Accurate |
| Sliding Window Counter | O(1) | O(n) | Approximate | Approximate |
| DREB (Custom) | O(1) | O(n) | Exact (dual-bucket) | Burst + idle credit |


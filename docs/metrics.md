# Metrics

This document provides mathematical definitions for every metric collected
by the benchmark framework.

---

## Throughput

**Definition**: the estimated number of sequential `allow()` operations a single
thread could sustain per second, derived from the measured average latency.

```
throughput = 1,000,000,000 / averageLatencyNs    [ops/sec]
```

> **Important — this is a derived metric, not an independent measurement.**
> The throughput number in the benchmark report is computed as
> `1,000,000,000 / averageLatencyNs` from the same `System.nanoTime()` latency
> measurements reported in the Latency columns. It represents a single-threaded,
> back-to-back estimate of sustained calls per second — not an independently
> measured saturated or concurrent throughput figure. The Throughput and Latency
> columns are therefore arithmetically related: one is the reciprocal of the other.
> Do not treat them as two independent measurements.
>
> For an independently measured throughput number under JMH-controlled conditions
> (multi-iteration, fork-isolated, with JIT warmup), run the JMH microbenchmark:
> `java -jar target/jmh-benchmarks.jar -prof gc`. Those results are reported
> separately and are not merged into this table.

Measured over the full duration of a measured run (excluding warmup).

---

## Latency

**Definition**: the wall-clock time to execute a single `allow()` call.

Measured via `System.nanoTime()` before and after each call:
```
latency_i = nanoTimeAfter - nanoTimeBefore    [nanoseconds]
```

### Percentiles

Given n sorted latency measurements L₁ ≤ L₂ ≤ ... ≤ Lₙ:

```
p-th percentile = L[⌈p/100 × n⌉]
```

Reported percentiles:
- **p50** (median): 50th percentile
- **p90**: 90th percentile
- **p95**: 95th percentile
- **p99**: 99th percentile
- **max**: maximum observed latency

### Average

```
avg = (1/n) × Σᵢ Lᵢ
```

### Standard Deviation

```
σ = √((1/n) × Σᵢ (Lᵢ - avg)²)
```

---

## Memory Usage

### Heap Usage
Measured via `Runtime.getRuntime()`:
```
usedHeap = totalMemory() - freeMemory()    [bytes]
```

Snapshots taken before and after the benchmark run:
```
memoryDelta = usedHeapAfter - usedHeapBefore    [bytes]
```

### Per-Client State Size (Estimate)
```
perClientBytes = memoryDelta / clientCount    [bytes]
```

> **Limitation**: this is a rough estimate, not precise instrumentation. It
> assumes that most heap growth during the benchmark is due to per-client state
> allocation. Other allocations (temporary objects, JVM internals) are included
> in the total. See `docs/benchmark-methodology.md` for discussion.

---

## CPU Usage

**Definition**: process CPU load as reported by `OperatingSystemMXBean`.

```
cpuLoad = getProcessCpuLoad()    [0.0 – 1.0]
```

Sampled at configurable intervals during the benchmark run.

Reported values:
- **Average CPU load**: arithmetic mean of all samples
- **Peak CPU load**: maximum observed sample

> **Limitation**: `getProcessCpuLoad()` returns the CPU usage of the JVM
> process as a fraction of available CPU time. Sampling frequency affects
> accuracy. Sub-millisecond spikes between samples are not captured.

---

## Fairness (Jain's Fairness Index)

**Definition**: measures how fairly rate-limit capacity is distributed among
competing clients.

Given n clients where client i had xᵢ accepted requests:

```
J(x₁, x₂, ..., xₙ) = (Σᵢ xᵢ)² / (n × Σᵢ xᵢ²)
```

Properties:
- **J = 1.0**: perfectly fair — all clients received equal shares
- **J = 1/n**: maximally unfair — one client monopolized all capacity
- **J ∈ [1/n, 1]** for all valid inputs

Edge cases:
- Empty input (no clients): returns 1.0
- Single client: returns 1.0
- All clients have 0 accepted requests: returns 1.0

---

## Correctness

### Acceptance Rate
```
acceptanceRate = acceptedRequests / totalRequests    [0.0 – 1.0]
```

### Violation Count
A **violation** is defined as any time window where the number of accepted
requests exceeds the algorithm's documented guarantee.

For window-based algorithms (Fixed Window, Sliding Window Log, Sliding Window
Counter):
```
violation if acceptedInWindow > maxRequests
```

For bucket-based algorithms (Token Bucket, Leaky Bucket):
```
violation if tokens go negative (should never happen with correct implementation)
```

### Total / Accepted / Rejected
```
totalRequests = acceptedRequests + rejectedRequests
```

This invariant is verified in concurrency tests.

---

## Burst Acceptance

Measures how an algorithm handles sudden traffic spikes:
- **Burst acceptance rate**: fraction of burst requests that were allowed
- Plotted as line chart: request index vs. allowed/rejected decision

---

## Aggregate Statistics Across Runs

For any metric M measured across k runs (M₁, M₂, ..., Mₖ):

### Mean
```
mean = (1/k) × Σⱼ Mⱼ
```

### Median
```
median = sorted(M)[⌈k/2⌉]
```

### Standard Deviation
```
σ = √((1/k) × Σⱼ (Mⱼ - mean)²)
```

### Min / Max
```
min = min(M₁, ..., Mₖ)
max = max(M₁, ..., Mₖ)
```

These statistics are reported for: throughput, average latency, memory usage,
and CPU load. Percentile latencies (p50/p95/p99) are computed from the merged
set of all individual measurements across runs, not as the mean of per-run
percentiles.

# Architecture

This document describes the architecture of the Rate Limiter Benchmark Framework.

## Overview

The framework is designed to evaluate and compare rate-limiting algorithms under identical,
reproducible conditions. It produces quantitative metrics, charts, and reports suitable for
engineering research and publication.

## Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        BenchmarkRunner                          │
│  (Orchestrates the entire benchmark lifecycle)                  │
│                                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │  Config   │  │ Algorithm│  │ Workload │  │  Metrics     │   │
│  │  Loader   │  │ Registry │  │ Factory  │  │  Engine      │   │
│  │(YAML)     │  │          │  │          │  │              │   │
│  └─────┬─────┘  └─────┬────┘  └─────┬────┘  └──────┬───────┘   │
│        │              │             │              │            │
│        ▼              ▼             ▼              ▼            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                   BenchmarkExecutor                       │   │
│  │  (Runs one algorithm × one workload: warmup + measured)   │   │
│  └────────────────────────┬─────────────────────────────────┘   │
│                           │                                     │
│                           ▼                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                     Reporting                             │   │
│  │  CsvReporter │ JsonReporter │ ChartGenerator │ ReportGen  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Package Structure

### `com.ratelimiter.core`
The common interface layer. All algorithms implement `RateLimiter` and process
`RateLimitRequest` → `RateLimitResponse`. Configuration is provided via
`RateLimiterConfig` (builder pattern).

### `com.ratelimiter.algorithms`
Five standard rate-limiting algorithms plus a placeholder for a custom algorithm.
Each implementation is self-contained, thread-safe, and supports variable request
cost.

### `com.ratelimiter.workload`
Deterministic workload generators that produce sequences of `RateLimitRequest`
objects. Each workload is seeded for reproducibility and models a different traffic
pattern (constant, burst, spike, adversarial, etc.).

### `com.ratelimiter.benchmark`
The benchmark orchestration layer:
- **BenchmarkConfig** — loaded from YAML profiles
- **BenchmarkExecutor** — runs one algorithm against one workload with proper
  warmup/measurement protocol
- **BenchmarkRunner** — the main entry point; iterates over all algorithm × workload
  combinations and produces results
- **BenchmarkResult** — aggregated metrics for one experiment

### `com.ratelimiter.metrics`
Metric collectors:
- **ThroughputMetrics** — ops/sec
- **LatencyMetrics** — percentile computation (p50/p90/p95/p99/max)
- **MemoryMetrics** — heap usage before/after
- **CpuMetrics** — process CPU load sampling
- **FairnessMetrics** — Jain's Fairness Index
- **AccuracyMetrics** — violation counting (accepted > limit)

### `com.ratelimiter.reporting`
Output generation:
- **CsvReporter** — raw and combined CSV files
- **JsonReporter** — metadata.json, configuration.json, results.json
- **ChartGenerator** — XChart PNG generation
- **ReportGenerator** — Markdown benchmark report
- **ResultAggregator** — merges JMH and workload results

### `com.ratelimiter.util`
- **HardwareInfo** — collects machine metadata (CPU, RAM, OS, JDK) for
  reproducibility

### `com.ratelimiter.microbench` (JMH source set)
JMH microbenchmarks for low-level `allow()` throughput/latency measurement.
Separate from the workload-simulator pipeline.

## Data Flow

```
YAML Config ──→ BenchmarkRunner ──→ BenchmarkExecutor
                     │                    │
                     │                    ├──→ Workload.generate()
                     │                    ├──→ RateLimiter.allow() [hot loop]
                     │                    ├──→ Metrics collection
                     │                    └──→ BenchmarkResult
                     │
                     ├──→ CsvReporter  ──→ results/<ts>/raw/*.csv
                     ├──→ JsonReporter ──→ results/<ts>/metadata.json
                     ├──→ ChartGenerator ──→ results/<ts>/graphs/*.png
                     └──→ ReportGenerator ──→ results/<ts>/report/benchmark-report.md
```

## Test Architecture

Tests are separated by concern and Maven phase:

| Category | Package | Maven Phase | Runs With |
|----------|---------|-------------|-----------|
| Correctness | `correctness/` | `test` | `mvn test` |
| Edge cases | `edgecases/` | `test` | `mvn test` |
| Concurrency | `concurrency/` | `integration-test` | `mvn verify -Pintegration` |
| Fairness | `fairness/` | `integration-test` | `mvn verify -Pintegration` |
| Property-based | `correctness/properties/` | `test` | `mvn test` |
| JMH | `microbench/` (separate source set) | manual | `mvn package -Pjmh` + `java -jar` |

## Algorithm Registration

Algorithms are registered in `BenchmarkRunner`'s `createAlgorithmRegistry()` method.
When `CustomRateLimiter` is implemented, it is automatically picked up — no changes
needed beyond replacing the placeholder implementation in `CustomRateLimiter.java`.
The framework detects `UnsupportedOperationException` and gracefully skips
unimplemented algorithms.

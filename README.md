# Rate Limiter Benchmark Framework

A production-grade benchmarking framework for comparing rate-limiting algorithms under
identical, reproducible conditions — built as the companion codebase for the paper
**"The Dual-Rate Elastic Bucket (DREB) Algorithm."** [Link the paper here once it's finalized.]

Five standard algorithms are benchmarked against three variants of DREB, a custom dual-rate,
idle-reward algorithm, under 10 workload patterns including two designed specifically to probe
DREB's stated advantages (`idle_then_burst`, `repeated_burst_abuser`) and one classic sustained-rate
scenario (`chronic_edge_rider`).

## Algorithms

| # | Algorithm | Type | Complexity |
|---|-----------|------|------------|
| 1 | **Token Bucket** | Bucket-based | O(1) time, O(n) space |
| 2 | **Leaky Bucket** | Bucket-based (traffic-shaping) | O(1) time, O(n) space |
| 3 | **Fixed Window Counter** | Window-based | O(1) time, O(n) space |
| 4 | **Sliding Window Log** | Window-based | O(k) time, O(n×k) space |
| 5 | **Sliding Window Counter** | Window-based (approx) | O(1) time, O(n) space |
| 6 | **DREB (proportional)** | Dual-bucket, idle-reward | O(1) time, O(n) space |
| 7 | **DREB (burst-matched)** | Dual-bucket, idle-reward, alternate capacity mapping | O(1) time, O(n) space |
| 8 | **DREB v2 (adaptive)** | Dual-bucket, abuse-adaptive idle credit and sustained cost | O(1) time, O(n) space |

See [`docs/algorithms.md`](docs/algorithms.md) for the full description of each, including DREB's two
capacity-mapping strategies and what v2 changes from v1.

## Quick Start

### Prerequisites
- **JDK 21** (required for virtual threads)
- **Maven 3.8+**

> Commands below use `JAVA_HOME=$(/usr/libexec/java_home -v 21)`, which is macOS-specific. On
> Linux/Windows, set `JAVA_HOME` to your JDK 21 install path instead.

### Run Correctness + Edge-Case Tests
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test
```

### Run SMOKE Benchmark (quick validation, ~5s)
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn exec:java \
  -Dexec.mainClass="com.ratelimiter.benchmark.BenchmarkRunner" \
  -Dexec.args="--profile smoke"
```

### Run Standard Benchmark (full comparison, ~30s)
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn exec:java \
  -Dexec.mainClass="com.ratelimiter.benchmark.BenchmarkRunner" \
  -Dexec.args="--profile standard"
```

### Run Stress Benchmark (high-load, ~30s at 2000 clients)
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn exec:java \
  -Dexec.mainClass="com.ratelimiter.benchmark.BenchmarkRunner" \
  -Dexec.args="--profile stress"
```

### Run JMH Microbenchmarks
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean package -Pjmh
java -jar target/jmh-benchmarks.jar -wi 3 -i 5 -f 1
```

## Adding Another Custom Algorithm

DREB's three variants already occupy the `Custom` slot as `CustomRateLimiter` and
`CustomRateLimiterV2`. To benchmark a different algorithm instead or in addition:

1. Implement the `RateLimiter` interface (`src/main/java/com/ratelimiter/core/RateLimiter.java`)
   in a new class under `src/main/java/com/ratelimiter/algorithms/`.
2. Register it in `BenchmarkRunner`'s algorithm list.
3. Run `mvn clean test` to verify correctness against the existing edge-case matrix.
4. Run a benchmark profile — it's included in the report, CSVs, and charts automatically.

## Project Structure

```
rate-limiter-benchmark/
├── src/main/java/
│   ├── com/ratelimiter/
│   │   ├── core/           # Common interface (RateLimiter, Request, Response, Config)
│   │   ├── algorithms/     # 5 standard algorithms + CustomRateLimiter/V2 (DREB adapters)
│   │   ├── workload/       # 10 workload generators
│   │   ├── benchmark/      # Benchmark orchestration (config, executor, runner)
│   │   ├── metrics/        # Metric collectors (throughput, latency, memory, CPU, fairness)
│   │   ├── reporting/      # Output generation (CSV, JSON, XChart PNG, Markdown report)
│   │   └── util/           # Hardware info collection
│   └── com/dreb/ratelimit/ # DREB v1 + v2 reference implementation (see the paper)
├── src/test/java/          # Correctness, edge-case, and concurrency test suites
├── src/jmh/java/           # JMH microbenchmarks (separate source set)
├── benchmarks/configurations/  # YAML profiles (smoke, standard, stress)
├── docs/                   # Architecture, algorithms, methodology, metrics documentation
└── scripts/                # Shell scripts for running tests and benchmarks
```

`results/` (generated benchmark output) and `scratch/`/`incoming/` (development staging material)
are gitignored — see `.gitignore`. The final, verified results referenced in the paper are
[link to wherever you're hosting them — a release asset, a separate results branch, etc.].

## Results

Each benchmark run creates a timestamped directory under `results/` (gitignored by default):

```
results/20260902_030333/
├── metadata.json                          # Machine info, config, git commit
├── configuration.json                     # Full benchmark configuration
├── raw/                                   # Per-algorithm CSV results
│   ├── Token_Bucket_results.csv
│   ├── Custom__proportional__results.csv
│   ├── Custom__burst-matched__results.csv
│   ├── Custom__v2-adaptive__results.csv
│   └── all_results.csv
├── graphs/                                # XChart-generated PNG charts
│   ├── throughput_comparison.png
│   ├── latency_percentiles.png
│   ├── memory_usage.png
│   ├── fairness_index.png
│   └── burst_behavior.png
└── report/
    └── benchmark-report.md                # Full comparison report
```

## Benchmark Profiles

| Profile | Clients | Duration | Rate Limit | Workloads | Purpose |
|---------|---------|----------|------------|-----------|---------|
| `smoke` | 2 | 5s | 100 | 5 | Quick validation |
| `standard` | 10 | 30s | 1000 | 10 | Full comparison |
| `stress` | 2000 | 30s | 50000 | 10 | High-load stress test |

## Documentation

- [Architecture](docs/architecture.md) — Component design and data flow
- [Algorithms](docs/algorithms.md) — Algorithm descriptions, complexity, trade-offs, and DREB's
  parameter-mapping strategies
- [Benchmark Methodology](docs/benchmark-methodology.md) — Single-machine framing, warmup, statistics
- [Metrics](docs/metrics.md) — Mathematical definitions for every metric, including which are
  derived vs. independently measured
- [Reproducibility](docs/reproducibility.md) — How to reproduce benchmark runs
- [Research Notes](docs/research-notes.md) — Methodology issues found and resolved during
  development (config-mapping bugs, timestamp-overlap bugs, a real concurrency race, etc.)

## Scientific Integrity

This framework does not manipulate results to favor any algorithm. Over the course of development,
several real bugs were found and fixed through this discipline rather than papered over — including
a case where DREB was accidentally being tested with a smaller effective capacity than the other
algorithms, and a genuine lost-update race condition in concurrent state mutation. See
[Research Notes](docs/research-notes.md) for the full account. The framework:
- Uses identical conditions for all algorithms
- Reports measured deltas factually, including cases where DREB underperforms
- Starts all bar chart y-axes at zero
- Documents measurement limitations (e.g. throughput is derived from latency, not independently
  measured under saturation)
- Records all results including failures
- Frames conclusions as relative comparisons on a specific machine

## License

See [LICENSE](LICENSE) for details.
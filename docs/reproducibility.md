# Reproducibility

This document describes how to reproduce benchmark results from this framework.

---

## Deterministic Workloads

All randomized workloads accept a `seed` parameter. The same seed produces the
same logical event sequence (same timestamps, same client IDs, same request
order). This is achieved via `java.util.Random` initialized with the given seed.

The seed used in each benchmark run is stored in `metadata.json` and
`configuration.json`.

---

## Metadata Capture

Every benchmark run creates a `metadata.json` containing:

| Field | Source |
|-------|--------|
| `timestamp` | ISO 8601 at run start |
| `gitCommit` | `git rev-parse --short HEAD` |
| `javaVersion` | `System.getProperty("java.version")` |
| `jvmVendor` | `System.getProperty("java.vm.vendor")` |
| `os` | `os.name` + `os.version` + `os.arch` |
| `cpuModel` | `sysctl -n machdep.cpu.brand_string` (macOS) |
| `cpuCores` | `Runtime.getRuntime().availableProcessors()` |
| `totalMemoryMb` | `OperatingSystemMXBean.getTotalMemorySize()` |
| `rateLimit` | from config |
| `windowSize` | from config |
| `clientCount` | from config |
| `workload` | from config |
| `randomSeed` | from config |
| `warmupRuns` | from config |
| `measurementRuns` | from config |

---

## Reproducing a Run

To reproduce a specific benchmark run:

1. **Check out the same commit**:
   ```bash
   git checkout <gitCommit from metadata.json>
   ```

2. **Use the same JDK**:
   Verify `java -version` matches `javaVersion` and `jvmVendor` from metadata.

3. **Use the same configuration**:
   The complete configuration is stored in `configuration.json`. Copy it to
   a YAML file or pass parameters directly.

4. **Use the same seed**:
   Ensure the `seed` in your configuration matches `randomSeed` from metadata.

5. **Run on the same machine**:
   Results are machine-specific. Running on different hardware will produce
   different absolute numbers (but relative rankings should be similar for
   CPU-bound benchmarks).

6. **Minimize background load**:
   Close unnecessary applications. Run-to-run variation increases with
   background activity.

### Example
```bash
# Reproduce using the same profile
mvn exec:java \
  -Dexec.mainClass="com.ratelimiter.benchmark.BenchmarkRunner" \
  -Dexec.args="--profile standard"

# Or with a specific configuration file
mvn exec:java \
  -Dexec.mainClass="com.ratelimiter.benchmark.BenchmarkRunner" \
  -Dexec.args="--profile path/to/configuration.yaml"
```

---

## Expected Reproducibility

| Metric | Expected Reproducibility |
|--------|-------------------------|
| Correctness (violations) | Exact — deterministic |
| Request ordering | Exact — seeded random |
| Throughput | ±5-10% — depends on CPU state, GC |
| Latency percentiles | ±10-20% — sensitive to scheduling |
| Memory usage | ±5% — depends on GC timing |
| Fairness index | Exact — deterministic workload |

---

## Version Control

The following should be committed to version control:
- All source code
- Configuration files (YAML profiles)
- Documentation

The following should **not** be committed (in `.gitignore`):
- `results/` — large generated data; archive selectively
- `target/` — build artifacts

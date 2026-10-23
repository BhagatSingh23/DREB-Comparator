# System & Execution Environment

This document records the exact system hardware, JVM runtime, operating system, and git library metadata under which performance benchmarks were executed.

**Label Note:** *All benchmark figures and tables reflect single machine, relative comparison conditions.*

## System Hardware & CPU Architecture

- **Operating System:** Mac OS X 26.5.2 (macOS Sequoia) aarch64
- **CPU Model:** Apple M3
- **Processor Topology:** 8 Physical Cores — 4 Performance Cores (P-cores @ 4.05 GHz) + 4 Efficiency Cores (E-cores @ 2.75 GHz)
- **Cache Hierarchy:**
  - P-cores: 128 KB L1 instruction cache + 64 KB L1 data cache per core, 16 MB shared L2 cache
  - E-cores: 128 KB L1 instruction cache + 64 KB L1 data cache per core, 4 MB shared L2 cache
- **Physical Memory (RAM):** 8192 MB Unified Memory (LPDDR5)
- **Hardware Timebase Resolution:** 24 MHz ARM generic timer (`CNTVCT_EL0`), yielding fixed ~41.67 ns clock tick period
- **Benchmark Timestamp:** 2026-10-23T23:06:13.552276Z

## Java Runtime & Compiler Specification

- **Java Version:** OpenJDK 26.0.2 (build 26.0.2+0)
- **JVM Vendor:** Homebrew OpenJDK 64-Bit Server VM
- **JVM Runtime Path:** `/opt/homebrew/Cellar/openjdk/26.0.2/libexec/openjdk.jdk/Contents/Home`
- **Bytecode Compilation Level:** `--release 21` (Java 21 LTS bytecode compatibility)
- **Compiler Options:** `-parameters -g`
- **JVM Runtime Flags:** `-Xmx2048m -XX:+UseG1GC`
- **Host Power State:** AC power, high performance profile

## Codebase & Library Metadata

- **DREB Comparator Git Commit:** `5772c1a`
- **dreb-ratelimiter Pinned Library Tag:** `v1.0.2-phase1`

## Timer Resolution & Batched Latency Note

On Apple Silicon, `System.nanoTime()` reads the 24 MHz ARM architecture counter, producing raw readings that step in discrete 41.67 ns multiples. Single-call microbenchmarks thus suffer from quantization noise. To eliminate this discretization artifact, latency is evaluated using batched sampling ($N = 1000$ consecutive operations per sample window), providing sub-nanosecond per-operation precision and continuous percentile distributions.

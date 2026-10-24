# System & Execution Environment

This document records the exact system hardware, JVM runtime, operating system, and git library metadata under which performance benchmarks were executed.

**Label Note:** *All benchmark figures and tables reflect single machine, relative comparison conditions.*

---

## 1. Raw `java -version` Outputs

### Primary Runtime & Benchmark Execution JDK (JDK 21 LTS)
```
java version "21.0.9" 2025-10-21 LTS
Java(TM) SE Runtime Environment (build 21.0.9+7-LTS-338)
Java HotSpot(TM) 64-Bit Server VM (build 21.0.9+7-LTS-338, mixed mode, sharing)
```

### System Compiler & Secondary JDK (OpenJDK 26.0.2)
```
openjdk version "26.0.2" 2026-07-21
OpenJDK Runtime Environment Homebrew (build 26.0.2)
OpenJDK 64-Bit Server VM Homebrew (build 26.0.2, mixed mode, sharing)
```

> **JVM Runtime Specification Note:**
> The primary compilation target for bytecode across the entire repository is `--release 21` (Java 21 LTS compatibility).
> Headline JMH microbenchmarks and diagnostic throughput scaling benchmarks are executed on the official Oracle JDK 21 LTS runtime (`/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`).
> System Maven build runs under OpenJDK 26.0.2 (`/opt/homebrew/Cellar/openjdk/26.0.2/libexec/openjdk.jdk/Contents/Home`).

---

## 2. Raw `sysctl` Hardware Architecture Output

Command: `sysctl hw.perflevel0 hw.perflevel1 hw.cachelinesize hw.l1icachesize hw.l1dcachesize hw.l2cachesize`

```
hw.perflevel0.physicalcpu: 4
hw.perflevel0.physicalcpu_max: 4
hw.perflevel0.logicalcpu: 4
hw.perflevel0.logicalcpu_max: 4
hw.perflevel0.l1icachesize: 196608
hw.perflevel0.l1dcachesize: 131072
hw.perflevel0.l2cachesize: 16777216
hw.perflevel0.cpusperl2: 4
hw.perflevel0.name: Performance
hw.perflevel1.physicalcpu: 4
hw.perflevel1.physicalcpu_max: 4
hw.perflevel1.logicalcpu: 4
hw.perflevel1.logicalcpu_max: 4
hw.perflevel1.l1icachesize: 131072
hw.perflevel1.l1dcachesize: 65536
hw.perflevel1.l2cachesize: 4194304
hw.perflevel1.cpusperl2: 4
hw.perflevel1.name: Efficiency
hw.cachelinesize: 128
hw.l1icachesize: 131072
hw.l1dcachesize: 65536
hw.l2cachesize: 4194304
```

---

## 3. Raw `pmset -g therm` Thermal State Output

Command: `pmset -g therm`

```
Note: No thermal warning level has been recorded
Note: No performance warning level has been recorded
Note: No CPU power status has been recorded
```

---

## 4. Hardware Timebase Resolution Note

On Apple Silicon (ARM64), `System.nanoTime()` reads the virtual counter register `CNTVCT_EL0` running at a fixed 24 MHz frequency ($1 / 24\,\text{MHz} \approx 41.67\,\text{ns}$). Single-call microbenchmarks without batching record discrete integer multiples of 41.67 ns ($41.7\,\text{ns}, 83.3\,\text{ns}, 125.0\,\text{ns}$).
To eliminate timer quantization noise and obtain continuous percentile distributions, latency is evaluated using batched sampling ($N = 1000$ calls per timing window).

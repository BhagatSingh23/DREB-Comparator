#!/bin/bash
set -e

mkdir -p results/perf/raw_runs

JAVA_BIN="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin/java"
JAR_PATH="target/jmh-benchmarks.jar"

echo "=========================================================================="
echo " Starting JMH 3-Run Stabilized Suite (Apple M3, JDK 21 LTS)"
echo " Protocol: 5 forks, warmup 5 x 1s, measurement 10 x 1s per run"
echo "=========================================================================="

for r in 1 2 3; do
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting Run $r of 3..."
    caffeinate -dims "$JAVA_BIN" -jar "$JAR_PATH" UnifiedRateLimiterBenchmark \
        -p scenario=same-key \
        -f 5 -wi 5 -i 10 -w 1s -r 1s \
        -jvm "$JAVA_BIN" \
        -rf json -rff "results/perf/raw_runs/jmh_same_key_run${r}.json" \
        -o "results/perf/raw_runs/jmh_same_key_run${r}.log"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Run $r finished successfully."
done

echo "All 3 benchmark runs finished."

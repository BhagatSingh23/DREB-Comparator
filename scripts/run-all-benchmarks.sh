#!/usr/bin/env bash
# run-all-benchmarks.sh — Runs correctness tests, then benchmarks at the specified profile level.
#
# Usage:
#   ./scripts/run-all-benchmarks.sh [profile]
#
# Arguments:
#   profile  — smoke | standard | stress (default: standard)
#
# This script:
#   1. Runs correctness + edge-case tests (mvn test)
#   2. Runs integration tests (concurrency, fairness)
#   3. Runs workload benchmarks at the specified profile
#   4. Runs JMH microbenchmarks (abbreviated for smoke)

set -euo pipefail

PROFILE="${1:-standard}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "============================================"
echo " Rate Limiter Benchmark Suite"
echo " Profile: $PROFILE"
echo " Project: $PROJECT_DIR"
echo "============================================"

cd "$PROJECT_DIR"

# Step 1: Compile
echo ""
echo "[1/5] Compiling project..."
mvn compile -q

# Step 2: Correctness tests
echo ""
echo "[2/5] Running correctness + edge-case tests..."
mvn test -q

# Step 3: Integration tests (concurrency, fairness)
echo ""
echo "[3/5] Running integration tests (concurrency, fairness)..."
mvn verify -Pintegration -q

# Step 4: Workload benchmarks
echo ""
echo "[4/5] Running workload benchmarks (profile: $PROFILE)..."
mvn exec:java \
  -Dexec.mainClass="com.ratelimiter.benchmark.BenchmarkRunner" \
  -Dexec.args="--profile $PROFILE"

# Step 5: JMH microbenchmarks (optional, skipped in smoke)
if [ "$PROFILE" != "smoke" ]; then
    echo ""
    echo "[5/5] Running JMH microbenchmarks..."
    mvn package -Pjmh -q -DskipTests
    java -jar target/jmh-benchmarks.jar \
      -rf json \
      -rff "$(ls -d results/*/raw/ 2>/dev/null | tail -1)jmh-results.json" \
      -wi 3 -i 5 -f 1
else
    echo ""
    echo "[5/5] Skipping JMH microbenchmarks in smoke profile."
fi

echo ""
echo "============================================"
echo " Benchmark suite complete!"
echo " Results: results/ (latest timestamped directory)"
echo "============================================"

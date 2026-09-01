#!/usr/bin/env bash
# run-performance-tests.sh — Runs integration tests (concurrency, fairness) and JMH microbenchmarks.
#
# Usage:
#   ./scripts/run-performance-tests.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_DIR"

echo "[1/2] Running integration tests (concurrency, fairness)..."
mvn verify -Pintegration

echo ""
echo "[2/2] Running JMH microbenchmarks..."
mvn package -Pjmh -q -DskipTests
java -jar target/jmh-benchmarks.jar \
  -rf json \
  -rff results/jmh-results.json \
  -wi 3 -i 5 -f 1

echo ""
echo "Done. JMH results: results/jmh-results.json"

#!/usr/bin/env bash
# generate-report.sh — Generates charts and Markdown report from existing benchmark results.
#
# Usage:
#   ./scripts/generate-report.sh [results-dir]
#
# Arguments:
#   results-dir  — Path to a specific results/<timestamp>/ directory.
#                  If omitted, uses the most recent results directory.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_DIR"

if [ -n "${1:-}" ]; then
    RESULTS_DIR="$1"
else
    RESULTS_DIR="$(ls -dt results/*/ 2>/dev/null | head -1)"
    if [ -z "$RESULTS_DIR" ]; then
        echo "ERROR: No results directory found. Run a benchmark first."
        exit 1
    fi
fi

echo "Generating report from: $RESULTS_DIR"

mvn exec:java \
  -Dexec.mainClass="com.ratelimiter.reporting.ReportGenerator" \
  -Dexec.args="$RESULTS_DIR"

echo ""
echo "Done. Report: ${RESULTS_DIR}report/benchmark-report.md"

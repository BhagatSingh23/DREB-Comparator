#!/usr/bin/env bash
# run-correctness-tests.sh — Runs unit tests (correctness + edge cases) only.
#
# Usage:
#   ./scripts/run-correctness-tests.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Running correctness + edge-case tests..."
cd "$PROJECT_DIR"
mvn test
echo "Done."

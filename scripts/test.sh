#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="${RUNNER_TEMP:-/tmp}/github-answer-workspace-agent-test"
rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}"

find "${PROJECT_DIR}/src/main/java" "${PROJECT_DIR}/src/test/java" -name '*.java' -print0 \
  | xargs -0 javac --release 21 -encoding UTF-8 -d "${BUILD_DIR}"

java -cp "${BUILD_DIR}" hu.gov.nav.answerdraft.SelfTest

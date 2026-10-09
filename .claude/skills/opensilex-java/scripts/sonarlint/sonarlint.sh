#!/usr/bin/env bash
# Headless SonarLint for the OpenSILEX Java code base. Thin wrapper: see sonarlint.py (--help) and README.md.
#   sonarlint.sh                       files changed against develop, changed lines only
#   sonarlint.sh path/File.java dir/   explicit files / directories
set -eu
exec python3 "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/sonarlint.py" "$@"

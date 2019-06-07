#!/usr/bin/env bash
# Combine scalafmt and clang-format into one script
# Currently clangfmt does not run due to making
# the embedded libunwind not compile after formatting
# Also, the Scala Native plugin does not support compiling
# or bundling C files

# Enable strict mode and fail the script on non-zero exit code,
# unresolved variable or pipe failure.
set -euo pipefail

java -version
if [ -n "${CLANG_FORMAT_PATH:-}" ]; then scripts/clangfmt --test; fi
scripts/scalafmt --test
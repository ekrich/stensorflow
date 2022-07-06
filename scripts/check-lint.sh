#!/bin/bash

set -e

echo 'Running clangfmt...'
scripts/clangfmt --test
echo 'Running scalafmt...'
scripts/scalafmt --test

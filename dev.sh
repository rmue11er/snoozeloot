#!/usr/bin/env bash
set -euo pipefail

# Build plugin, then start local Paper server (copies jar to run/plugins).
mvn -q -Dmaven.repo.local=.m2 clean package
exec ./run/start.sh


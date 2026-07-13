#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT_DIR/run"
MC_VERSION="${MC_VERSION:-1.20.6}"
PAPER_JAR="$RUN_DIR/paper-${MC_VERSION}.jar"
PORT="${PORT:-25565}"

find_pids() {
  local pids=""

  if command -v lsof >/dev/null 2>&1; then
    pids="$(lsof -nP -iTCP:"$PORT" -sTCP:LISTEN -t 2>/dev/null || true)"
  fi

  if [[ -z "$pids" ]] && [[ -f "$PAPER_JAR" ]]; then
    pids="$(pgrep -f "$PAPER_JAR" 2>/dev/null || true)"
  fi

  echo "$pids" | tr ' ' '\n' | sed '/^$/d' | sort -u
}

stop_pid() {
  local pid="$1"
  if ! kill -0 "$pid" 2>/dev/null; then
    return 0
  fi

  echo "Stopping Paper server (PID $pid)..."
  kill -TERM "$pid" 2>/dev/null || true

  for _ in {1..10}; do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "Server stopped."
      return 0
    fi
    sleep 1
  done

  echo "Server did not stop gracefully, forcing..."
  kill -KILL "$pid" 2>/dev/null || true
  sleep 1

  if kill -0 "$pid" 2>/dev/null; then
    echo "Failed to stop PID $pid."
    return 1
  fi

  echo "Server stopped."
}

main() {
  local pids
  pids="$(find_pids)"

  if [[ -z "$pids" ]]; then
    echo "No local Paper server found (port $PORT)."
    exit 0
  fi

  local failed=0
  local pid
  for pid in $pids; do
    stop_pid "$pid" || failed=1
  done

  exit "$failed"
}

main "$@"

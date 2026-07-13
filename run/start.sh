#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT_DIR/run"
PLUGINS_DIR="$RUN_DIR/plugins"

MC_VERSION="${MC_VERSION:-1.20.6}"
JAVA_BIN="${JAVA_BIN:-java}"
MEMORY="${MEMORY:-2G}"

PAPER_JAR="$RUN_DIR/paper-${MC_VERSION}.jar"

download_paper() {
  python3 - <<'PY'
import json, os, urllib.request

mc_version = os.environ.get("MC_VERSION", "1.20.6")
run_dir = os.path.join(os.getcwd(), "run")
paper_jar = os.path.join(run_dir, f"paper-{mc_version}.jar")

UA = "SnoozeLoot-local-dev/0.1 (+https://papermc.io)"

def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req) as r:
        return r.read()

def get_json(url: str):
    return json.loads(fetch(url).decode("utf-8"))

project = "paper"
base = f"https://fill.papermc.io/v3/projects/{project}"
ver = get_json(base + f"/versions/{mc_version}")
builds = ver.get("builds", [])
if not builds:
    raise SystemExit(f"No builds found for {project} {mc_version}.")

# `builds` is a list of build IDs (ints), newest first.
build_id = builds[0]
build = get_json(base + f"/versions/{mc_version}/builds/{build_id}")
jar_url = build["downloads"]["server:default"]["url"]

os.makedirs(run_dir, exist_ok=True)
print(f"Downloading {jar_url}")
req = urllib.request.Request(jar_url, headers={"User-Agent": UA})
with urllib.request.urlopen(req) as r, open(paper_jar, "wb") as f:
    f.write(r.read())
print(f"Saved to {paper_jar}")
PY
}

ensure_paper() {
  if [[ ! -f "$PAPER_JAR" ]]; then
    echo "Paper jar missing. Downloading latest Paper for ${MC_VERSION}..."
    (cd "$ROOT_DIR" && download_paper)
  fi
}

ensure_eula() {
  if [[ ! -f "$RUN_DIR/eula.txt" ]]; then
    echo "eula=true" > "$RUN_DIR/eula.txt"
  fi
}

sync_plugin() {
  local jar=""
  shopt -s nullglob
  local candidates=("$ROOT_DIR"/target/*.jar)
  shopt -u nullglob

  if [[ ${#candidates[@]} -eq 0 ]]; then
    echo "No plugin jar in target/. Build first:"
    echo "  mvn -q -Dmaven.repo.local=.m2 clean package"
    exit 1
  fi

  for candidate in "${candidates[@]}"; do
    local base
    base="$(basename "$candidate")"
    if [[ "$base" == original-* ]]; then
      continue
    fi
    jar="$candidate"
    break
  done

  if [[ -z "$jar" ]]; then
    echo "No shaded plugin jar found in target/. Build first:"
    echo "  mvn -q -Dmaven.repo.local=.m2 clean package"
    exit 1
  fi

  mkdir -p "$PLUGINS_DIR"
  cp -f "$jar" "$PLUGINS_DIR/SnoozeLoot.jar"
  echo "Synced plugin jar: $(basename "$jar")"
}

main() {
  cd "$RUN_DIR"
  ensure_paper
  ensure_eula
  sync_plugin

  echo "Starting Paper ${MC_VERSION} with ${MEMORY} RAM..."
  exec "$JAVA_BIN" -Xms"$MEMORY" -Xmx"$MEMORY" -jar "$PAPER_JAR" --nogui
}

main "$@"


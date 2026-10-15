#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -f .env ]] || { echo 'Run ./scripts/setup.sh first.'; exit 1; }
set -a
source .env
set +a
docker compose up -d --wait db
runtime=java
[[ ! -x .tools/jdk/bin/java ]] || runtime="$PWD/.tools/jdk/bin/java"
"$runtime" -jar backend/target/codenova-1.0.0.jar > /tmp/codenova-api.log 2>&1 &
api_pid=$!
trap 'kill "$api_pid" 2>/dev/null || true' EXIT
ready=false
for attempt in {1..45}; do
 kill -0 "$api_pid" 2>/dev/null || { echo 'API failed. See /tmp/codenova-api.log'; exit 1; }
 if curl --fail --silent http://localhost:8080/api/content >/dev/null; then ready=true; break; fi
 sleep 1
done
[[ "$ready" == true ]] || { echo 'API not ready. See /tmp/codenova-api.log'; exit 1; }
cd frontend
npm start -- --port 4200

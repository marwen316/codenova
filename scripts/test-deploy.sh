#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -f .env ]] || { echo 'Run ./scripts/setup.sh first.'; exit 1; }
./scripts/mvn.sh -B package -DskipTests
(cd frontend && npm run build -- --configuration production)
# Nginx workers and the non-root JVM must be able to read mounted build outputs.
find frontend/dist/web/browser -type d -exec chmod 755 {} +
find frontend/dist/web/browser -type f -exec chmod 644 {} +
chmod 644 backend/target/codenova-1.0.0.jar
docker compose -p codenova-test -f compose.test.yaml --env-file .env up -d --force-recreate
ready=false
for attempt in {1..60}; do
 if curl --fail --silent "http://127.0.0.1:${TEST_PORT:-8088}/api/content" >/dev/null; then ready=true; break; fi
 sleep 1
done
[[ "$ready" == true ]] || { echo 'Test API failed. Inspect docker compose -p codenova-test -f compose.test.yaml logs.'; exit 1; }
set -a
source .env
set +a
export E2E_BASE_URL="http://127.0.0.1:${TEST_PORT:-8088}"
(cd frontend && npm run test:e2e)
echo 'Acceptance stack is running locally; no public URL has been configured.'

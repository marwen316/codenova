#!/usr/bin/env bash
# Read-only diagnostics. Never print credentials, cookies or the process environment.
set -euo pipefail
cd "$(dirname "$0")/.."
project_name="${1:-codenova-recette}"
compose_args=(-p "$project_name" -f compose.yaml)
[[ ! -f compose.recette.yaml ]] || compose_args+=(-f compose.recette.yaml)
compose_args+=(--profile app)
printf 'Checkout revision: '
git rev-parse --short HEAD
docker compose "${compose_args[@]}" ps
for endpoint in content csrf; do
 status=$(curl --silent --max-time 10 --output /dev/null --write-out '%{http_code}' "http://127.0.0.1:8088/api/$endpoint") || status=unreachable
 printf '/api/%s HTTP: %s\n' "$endpoint" "$status"
done
docker compose "${compose_args[@]}" exec -T backend sh -c '
 if [ -n "${ADMIN_USERNAME:-}" ]; then echo "ADMIN_USERNAME: present"; else echo "ADMIN_USERNAME: missing"; fi
 if [ -n "${ADMIN_PASSWORD:-}" ]; then echo "ADMIN_PASSWORD: present"; else echo "ADMIN_PASSWORD: missing"; fi
 case "${COOKIE_SECURE:-false}" in
  true) echo "Secure session cookie: enabled (requires HTTPS)";;
  false) echo "Secure session cookie: disabled (allows local HTTP testing)";;
  *) echo "Secure session cookie: nonstandard setting";;
 esac
' || printf 'Backend container cannot be inspected: check its running state.\n'

if docker compose "${compose_args[@]}" exec -T web curl --noproxy '*' --fail --silent --max-time 5 --output /dev/null http://backend:8080/api/content 2>/dev/null; then
 printf 'Direct web -> backend: reachable\n'
else
 printf 'Direct web -> backend: unavailable\n'
fi

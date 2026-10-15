#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .tools
if ! command -v javac >/dev/null && [[ ! -x .tools/jdk/bin/javac ]]; then
 docker pull eclipse-temurin:21-jdk
 container_id=$(docker create eclipse-temurin:21-jdk)
 trap 'docker rm "$container_id" >/dev/null' EXIT
 docker cp "$container_id:/opt/java/openjdk" .tools/jdk
 docker rm "$container_id" >/dev/null
 trap - EXIT
fi
if ! command -v mvn >/dev/null && [[ ! -x .tools/apache-maven-3.9.11/bin/mvn ]]; then
 curl -fsSL https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz -o .tools/maven.tar.gz
 curl -fsSL https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz.sha512 -o .tools/maven.sha512
 python3 - <<'PY'
import hashlib
from pathlib import Path
assert hashlib.sha512(Path('.tools/maven.tar.gz').read_bytes()).hexdigest()==Path('.tools/maven.sha512').read_text().strip(), 'Maven checksum mismatch'
PY
 tar -xzf .tools/maven.tar.gz -C .tools
fi
python3 - <<'PY'
from pathlib import Path
import secrets
p=Path('.env')
if not p.exists():
 p.write_text('DATABASE_PASSWORD='+secrets.token_urlsafe(32)+'\nADMIN_USERNAME=admin\nADMIN_PASSWORD='+secrets.token_urlsafe(24)+'\nCOOKIE_SECURE=false\n')
 p.chmod(0o600)
PY
(cd frontend && npm ci --cache /tmp/codenova-npm --no-audit --no-fund && npm run build -- --configuration production)
docker compose up -d --wait db
./scripts/mvn.sh -B package -DskipTests

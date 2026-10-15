#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -x .tools/jdk/bin/javac ]]; then export JAVA_HOME="$PWD/.tools/jdk"; fi
if [[ -f /etc/ssl/certs/java/cacerts ]]; then export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"; fi
python3 - <<'PY'
import os,urllib.parse,xml.etree.ElementTree as E
from pathlib import Path
root=E.Element('settings');url=urllib.parse.urlparse(os.environ.get('HTTPS_PROXY',''))
if url.hostname:
 p=E.SubElement(E.SubElement(root,'proxies'),'proxy')
 for k,v in {'id':'cloud','active':'true','protocol':'http','host':url.hostname,'port':str(url.port or 80)}.items():E.SubElement(p,k).text=v
Path('.tools').mkdir(exist_ok=True)
E.ElementTree(root).write('.tools/settings.xml')
PY
if command -v mvn >/dev/null; then runner=mvn; else runner="$PWD/.tools/apache-maven-3.9.11/bin/mvn"; fi
exec "$runner" -s "$PWD/.tools/settings.xml" -f backend/pom.xml -Dmaven.repo.local="$PWD/.tools/m2" "$@"

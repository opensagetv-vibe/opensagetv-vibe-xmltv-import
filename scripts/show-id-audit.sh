#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
plugin_jar="$root/output/packages/XMLTVImportPlugin.jar"
sage_jar="${SAGE_JAR:-/work/sagetv/output/server/Sage.jar}"

if [[ ! -f "$plugin_jar" ]]; then
  echo "Missing $plugin_jar; build the plugin first." >&2
  exit 1
fi
if [[ ! -f "$sage_jar" ]]; then
  echo "Missing Sage.jar at $sage_jar; set SAGE_JAR to the built Core artifact." >&2
  exit 1
fi

exec java -classpath "$plugin_jar:$sage_jar" xmltv.ShowIdAudit "$@"

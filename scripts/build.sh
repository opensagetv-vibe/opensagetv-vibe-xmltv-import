#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sage_jar="${SAGE_JAR:-/work/sagetv/output/server/Sage.jar}"
src="$root/SAGETV_SERVER_ROOT_Contents/xmltv_src"
out="$root/output"
test -f "$sage_jar" || { echo "ERROR: Sage.jar not found: $sage_jar" >&2; exit 1; }
rm -rf "$out"
mkdir -p "$out/classes" "$out/test-classes" "$out/packages" "$out/test-results" "$out/config-examples"
javac -encoding UTF-8 -Xlint:deprecation -Xlint:unchecked -source 8 -target 8 \
  -classpath "$sage_jar" -d "$out/classes" \
  "$src/Channel.java" "$src/Show.java" "$src/Init.java" \
  "$src/XMLInputStreamFilter.java" "$src/XMLTVImportPlugin.java" \
  2>&1 | tee "$out/test-results/javac.log"
javac -encoding UTF-8 -source 8 -target 8 -classpath "$out/classes:$sage_jar" \
  -d "$out/test-classes" "$root/tests/xmltv/XMLInputStreamFilterTest.java" \
  "$root/tests/xmltv/ImporterHarness.java"
java -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.XMLInputStreamFilterTest \
  | tee "$out/test-results/tests.log"
identity_work="$(mktemp -d)"
trap 'rm -rf "$identity_work"' EXIT
cp "$root/tests/fixtures/series-season-episode.xml" "$identity_work/identity.xml"
printf 'provider.name=Identity Test\nprovider.id=999\nxmltv.files=%s\nlog.configuration=false\nlog.channel=false\nlog.show=false\n' \
  "$identity_work/identity.xml" > "$identity_work/identity.xmltv.properties"
(cd "$identity_work"; java -Dxmltv.test.currentTimeMillis=1787659500000 \
  -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.ImporterHarness identity.xml) \
  | tee "$out/test-results/identity.log"
grep -q 'shows=2 uniqueShowIds=2 conflictingShowIds=0 airings=2' "$out/test-results/identity.log"
jar --create --file "$out/packages/XMLTVImportPlugin.jar" -C "$out/classes" xmltv
cp "$root"/SAGETV_SERVER_ROOT_Contents/*.properties "$out/config-examples/"
jar --list --file "$out/packages/XMLTVImportPlugin.jar" | grep -q 'xmltv/XMLTVImportPlugin.class'
echo "[PASS] XMLTVImportPlugin build"

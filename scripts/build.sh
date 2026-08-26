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
  "$src/ShowIdGenerator.java" "$src/XMLInputStreamFilter.java" \
  "$src/XMLTVImportPlugin.java" \
  2>&1 | tee "$out/test-results/javac.log"
javac -encoding UTF-8 -source 8 -target 8 -classpath "$out/classes:$sage_jar" \
  -d "$out/test-classes" "$root/tests/xmltv/XMLInputStreamFilterTest.java" \
  "$root/tests/xmltv/ImporterHarness.java" "$root/tests/xmltv/ProviderDiscoveryTest.java" \
  "$root/tests/xmltv/ChannelIconDownloadTest.java" \
  "$root/tests/xmltv/ShowIdGeneratorTest.java"
java -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.XMLInputStreamFilterTest \
  | tee "$out/test-results/tests.log"
provider_work="$(mktemp -d)"
(cd "$provider_work"; java -classpath "$out/test-classes:$out/classes:$sage_jar" \
  xmltv.ProviderDiscoveryTest) | tee -a "$out/test-results/tests.log"
rm -rf "$provider_work"
icon_work="$(mktemp -d)"
(cd "$icon_work"; java -Djava.awt.headless=true \
  -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.ChannelIconDownloadTest) \
  | tee -a "$out/test-results/tests.log"
rm -rf "$icon_work"
java -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.ShowIdGeneratorTest \
  | tee -a "$out/test-results/tests.log"
identity_work="$(mktemp -d)"
trap 'rm -rf "$identity_work"' EXIT
cp "$root/tests/fixtures/series-season-episode.xml" "$identity_work/identity.xml"
printf 'provider.name=Identity Test\nprovider.id=999\nxmltv.files=%s\nlog.configuration=false\nlog.channel=false\nlog.show=false\n' \
  "$identity_work/identity.xml" > "$identity_work/identity.xmltv.properties"
(cd "$identity_work"; java -Dxmltv.test.currentTimeMillis=1787659500000 \
  -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.ImporterHarness identity.xml) \
  | tee "$out/test-results/identity.log"
grep -q 'shows=2 uniqueShowIds=2 conflictingShowIds=0 airings=2' "$out/test-results/identity.log"
grep -q 'showIdDescriptions=0 showIdBonus=0 ids=\[EP1KbtI40002, EP2YRXUU0001\]' "$out/test-results/identity.log"

v2_work="$(mktemp -d)"
cp "$root/tests/fixtures/series-season-episode.xml" "$v2_work/identity.xml"
printf 'provider.name=Identity Test\nprovider.id=999\nxmltv.files=%s\nxmltv.show_id.strategy=v2\nxmltv.show_id.v2.map_file=%s\nxmltv.show_id.display=description\nlog.configuration=false\nlog.channel=false\nlog.show=false\n' \
  "$v2_work/identity.xml" "$v2_work/show-id-map.properties" > "$v2_work/identity.xmltv.properties"
(cd "$v2_work"; java -Dxmltv.test.currentTimeMillis=1787659500000 \
  -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.ImporterHarness identity.xml) \
  | tee "$out/test-results/identity-v2-first.log"
(cd "$v2_work"; java -Dxmltv.test.currentTimeMillis=1787659500000 \
  -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.ImporterHarness identity.xml) \
  | tee "$out/test-results/identity-v2-second.log"
grep -q 'shows=2 uniqueShowIds=2 conflictingShowIds=0 airings=2 showIdDescriptions=2 showIdBonus=0' "$out/test-results/identity-v2-first.log"
first_ids="$(sed -n 's/.* ids=\(\[[^]]*\]\) calls=.*/\1/p' "$out/test-results/identity-v2-first.log")"
second_ids="$(sed -n 's/.* ids=\(\[[^]]*\]\) calls=.*/\1/p' "$out/test-results/identity-v2-second.log")"
test -n "$first_ids" && test "$first_ids" = "$second_ids"
test -s "$v2_work/show-id-map.properties"
test -z "$(find "$v2_work" -name '*.tmp' -print -quit)"
rm -rf "$v2_work"

bonus_work="$(mktemp -d)"
cp "$root/tests/fixtures/series-season-episode.xml" "$bonus_work/identity.xml"
printf 'provider.name=Identity Test\nprovider.id=999\nxmltv.files=%s\nxmltv.show_id.strategy=v2\nxmltv.show_id.v2.map_file=%s\nxmltv.show_id.display=bonus\nlog.configuration=false\nlog.channel=false\nlog.show=false\n' \
  "$bonus_work/identity.xml" "$bonus_work/show-id-map.properties" > "$bonus_work/identity.xmltv.properties"
(cd "$bonus_work"; java -Dxmltv.test.currentTimeMillis=1787659500000 \
  -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.ImporterHarness identity.xml) \
  | tee "$out/test-results/identity-v2-bonus.log"
grep -q 'shows=2 uniqueShowIds=2 conflictingShowIds=0 airings=2 showIdDescriptions=0 showIdBonus=2' "$out/test-results/identity-v2-bonus.log"
rm -rf "$bonus_work"
jar --create --file "$out/packages/XMLTVImportPlugin.jar" -C "$out/classes" xmltv
cp "$root"/SAGETV_SERVER_ROOT_Contents/*.properties "$out/config-examples/"
jar --list --file "$out/packages/XMLTVImportPlugin.jar" | grep -q 'xmltv/XMLTVImportPlugin.class'
jar --list --file "$out/packages/XMLTVImportPlugin.jar" | grep -q 'xmltv/ShowIdGenerator.class'
echo "[PASS] XMLTVImportPlugin build"

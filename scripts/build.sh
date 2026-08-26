#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sage_jar="${SAGE_JAR:-/work/sagetv/output/server/Sage.jar}"
src="$root/SAGETV_SERVER_ROOT_Contents/xmltv_src"
out="$root/output"
test -f "$sage_jar" || { echo "ERROR: Sage.jar not found: $sage_jar" >&2; exit 1; }
rm -rf "$out"
mkdir -p "$out/classes" "$out/test-classes" "$out/packages" "$out/test-results" "$out/config-examples"
javac -encoding UTF-8 -Xlint:deprecation -Xlint:unchecked --release 8 \
  -classpath "$sage_jar" -d "$out/classes" \
  "$src/Channel.java" "$src/Show.java" "$src/Init.java" \
  "$src/ImportResult.java" "$src/XmltvConfiguration.java" \
  "$src/FeedDownloader.java" "$src/SecureXmlReader.java" \
  "$src/XmltvParser.java" "$src/XmltvDateParser.java" \
  "$src/ExternalCommandRunner.java" "$src/ChannelMapper.java" \
  "$src/ProgrammeMapper.java" "$src/SageGuideWriter.java" \
  "$src/ShowIdGenerator.java" "$src/ShowIdAudit.java" \
  "$src/XMLInputStreamFilter.java" \
  "$src/XMLTVImportPlugin.java" \
  2>&1 | tee "$out/test-results/javac.log"
javac -encoding UTF-8 --release 8 -classpath "$out/classes:$sage_jar" \
  -d "$out/test-classes" "$root/tests/xmltv/XMLInputStreamFilterTest.java" \
  "$root/tests/xmltv/ImporterHarness.java" "$root/tests/xmltv/ProviderDiscoveryTest.java" \
  "$root/tests/xmltv/ChannelIconDownloadTest.java" \
  "$root/tests/xmltv/ShowIdGeneratorTest.java" \
  "$root/tests/xmltv/ModernInfrastructureTest.java" \
  "$root/tests/xmltv/ImportFailureHarness.java" \
  "$root/tests/xmltv/MetadataMappingHarness.java" \
  "$root/tests/xmltv/ProviderReloadTest.java"
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
java -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.ModernInfrastructureTest \
  | tee -a "$out/test-results/tests.log"
reload_work="$(mktemp -d)"
(cd "$reload_work"; java -classpath "$out/test-classes:$out/classes:$sage_jar" \
  xmltv.ProviderReloadTest) | tee -a "$out/test-results/tests.log"
rm -rf "$reload_work"

audit_work="$(mktemp -d)"
java -classpath "$out/classes:$sage_jar" xmltv.ShowIdAudit --provider-id 999 \
  --map "$audit_work/show-id-map.properties" \
  "$root/tests/fixtures/series-season-episode.xml" \
  > "$out/test-results/show-id-audit.tsv" 2> "$out/test-results/show-id-audit.log"
test "$(wc -l < "$out/test-results/show-id-audit.tsv")" -eq 3
grep -q 'programmes=2.*v2_collisions=0.*map_written=false' \
  "$out/test-results/show-id-audit.log"
test ! -e "$audit_work/show-id-map.properties"
java -classpath "$out/classes:$sage_jar" xmltv.ShowIdAudit --provider-id 999 \
  --map "$audit_work/show-id-map.properties" --write-map \
  "$root/tests/fixtures/series-season-episode.xml" > /dev/null 2> /dev/null
test -s "$audit_work/show-id-map.properties"
set +e
java -classpath "$out/classes:$sage_jar" xmltv.ShowIdAudit --provider-id 999 \
  --map "$audit_work/duplicate-map.properties" \
  "$root/tests/fixtures/duplicate-provider-id.xml" \
  > /dev/null 2> "$out/test-results/show-id-duplicate.log"
duplicate_status=$?
set -e
test "$duplicate_status" -eq 2
grep -q 'v2_collisions=1' "$out/test-results/show-id-duplicate.log"
rm -rf "$audit_work"
echo '[PASS] Show-ID migration audit is dry-run by default' \
  | tee -a "$out/test-results/tests.log"

failure_work="$(mktemp -d)"
printf 'provider.name=Missing Feed\nprovider.id=999\nxmltv.files=%s\nlog.configuration=false\nlog.channel=false\nlog.show=false\n' \
  "$failure_work/does-not-exist.xml" > "$failure_work/failure.xmltv.properties"
(cd "$failure_work"; java -classpath "$out/test-classes:$out/classes:$sage_jar" \
  xmltv.ImportFailureHarness) | tee "$out/test-results/missing-feed.log"
grep -q 'result=false' "$out/test-results/missing-feed.log"

cp "$root/tests/fixtures/hostile-doctype.xml.invalid" "$failure_work/hostile.xml"
printf 'provider.name=Hostile Feed\nprovider.id=999\nxmltv.files=%s\nlog.configuration=false\nlog.channel=false\nlog.show=false\n' \
  "$failure_work/hostile.xml" > "$failure_work/failure.xmltv.properties"
(cd "$failure_work"; java -classpath "$out/test-classes:$out/classes:$sage_jar" \
  xmltv.ImportFailureHarness) | tee "$out/test-results/hostile-feed.log"
grep -Eq 'result=false calls=\{toString=[0-9]+\}' "$out/test-results/hostile-feed.log"

cp "$root/tests/fixtures/truncated.xml.invalid" "$failure_work/truncated.xml"
printf 'provider.name=Truncated Feed\nprovider.id=999\nxmltv.files=%s\nlog.configuration=false\nlog.channel=false\nlog.show=false\n' \
  "$failure_work/truncated.xml" > "$failure_work/failure.xmltv.properties"
(cd "$failure_work"; java -classpath "$out/test-classes:$out/classes:$sage_jar" \
  xmltv.ImportFailureHarness) | tee "$out/test-results/truncated-feed.log"
grep -Eq 'result=false calls=\{toString=[0-9]+\}' "$out/test-results/truncated-feed.log"

cp "$root/tests/fixtures/series-season-episode.xml" "$failure_work/valid.xml"
printf 'provider.name=Rejected Show\nprovider.id=999\nxmltv.files=%s\nlog.configuration=false\nlog.channel=false\nlog.show=false\n' \
  "$failure_work/valid.xml" > "$failure_work/failure.xmltv.properties"
(cd "$failure_work"; java -Dxmltv.test.currentTimeMillis=1787659500000 \
  -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.ImportFailureHarness reject-show) \
  | tee "$out/test-results/rejected-show.log"
grep -q 'result=false' "$out/test-results/rejected-show.log"
rm -rf "$failure_work"
echo '[PASS] failed imports are reported and do not replace lineups' \
  | tee -a "$out/test-results/tests.log"

metadata_work="$(mktemp -d)"
cp "$root/tests/fixtures/metadata-coverage.xml" "$metadata_work/metadata.xml"
printf 'provider.name=Metadata Coverage\nprovider.id=999\nxmltv.files=%s\nxmltv.language.preferred=en\nlog.configuration=false\nlog.channel=false\nlog.show=false\n' \
  "$metadata_work/metadata.xml" > "$metadata_work/metadata.xmltv.properties"
(cd "$metadata_work"; java -Dxmltv.test.currentTimeMillis=1787659500000 \
  -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.MetadataMappingHarness) \
  | tee "$out/test-results/metadata-coverage.log"
grep -q 'result=true movie=true series=true subtitle=true' \
  "$out/test-results/metadata-coverage.log"
rm -rf "$metadata_work"

multi_work="$(mktemp -d)"
printf 'provider.name=Multiple Sources\nprovider.id=999\nxmltv.files=%s,%s\nlog.configuration=false\nlog.channel=false\nlog.show=false\n' \
  "$root/tests/fixtures/multi-source-a.xml" "$root/tests/fixtures/multi-source-b.xml" \
  > "$multi_work/multi.xmltv.properties"
(cd "$multi_work"; java -Dxmltv.test.currentTimeMillis=1787659500000 \
  -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.ImporterHarness multi.xml) \
  | tee "$out/test-results/multiple-sources.log"
grep -q 'result=true channels=1 shows=2 uniqueShowIds=2 conflictingShowIds=0 airings=2' \
  "$out/test-results/multiple-sources.log"
grep -q 'lineupStations=1' "$out/test-results/multiple-sources.log"
test -s "$multi_work/xmltv_Multiple_Sources.xml"
test -s "$multi_work/xmltv_Multiple_Sources_2.xml"
rm -rf "$multi_work"
echo '[PASS] metadata coverage and multiple-source import fixtures' \
  | tee -a "$out/test-results/tests.log"

large_work="$(mktemp -d)"
{
  printf '%s\n' '<?xml version="1.0" encoding="UTF-8"?>' '<tv>'
  for number in $(seq 1000 1499); do
    printf '  <channel id="large-%s"><display-name>%s CALL%s</display-name></channel>\n' \
      "$number" "$number" "$number"
  done
  for number in $(seq 1000 1499); do
    printf '  <programme start="20260825120000 +0000" stop="20260825123000 +0000" channel="large-%s"><title>Large %s</title><episode-num system="tms">EP%08d0001</episode-num></programme>\n' \
      "$number" "$number" "$number"
  done
  printf '%s\n' '</tv>'
} > "$large_work/large.xml"
printf 'provider.name=Large Lineup\nprovider.id=999\nxmltv.files=%s\nlog.configuration=false\nlog.channel=false\nlog.show=false\n' \
  "$large_work/large.xml" > "$large_work/large.xmltv.properties"
(cd "$large_work"; java -Dxmltv.test.currentTimeMillis=1787659500000 \
  -classpath "$out/test-classes:$out/classes:$sage_jar" xmltv.ImporterHarness large.xml) \
  | tee "$out/test-results/large-lineup.log"
grep -q 'result=true channels=500 shows=500 uniqueShowIds=500 conflictingShowIds=0 airings=500' \
  "$out/test-results/large-lineup.log"
rm -rf "$large_work"
echo '[PASS] generated 500-channel stress lineup' | tee -a "$out/test-results/tests.log"
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

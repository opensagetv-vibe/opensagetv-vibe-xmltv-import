#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
corpus="${1:?directory containing private XMLTV files}"
classes="$root/output/classes"; tests="$root/output/test-classes"
javac -encoding UTF-8 -source 8 -target 8 -classpath "$classes:/work/sagetv/output/server/Sage.jar" -d "$tests" "$root/tests/xmltv/ImporterHarness.java"
find "$corpus" -maxdepth 1 -type f \( -name '*.xml' -o -name '*.xmltv' -o -name '*.xml.old' \) -print0 | sort -z | while IFS= read -r -d '' file; do
  work="$(mktemp -d)"; input="$work/$(basename "$file")"; cp "$file" "$input"
  test_now="$(python3 - "$input" <<'PY'
import datetime,sys,xml.etree.ElementTree as ET
values=[]
for _,element in ET.iterparse(sys.argv[1],events=('end',)):
    if element.tag.rsplit('}',1)[-1]=='programme' and element.get('start'):
        value=element.get('start').strip()
        values.append(datetime.datetime.strptime(value[:14]+' '+(value[15:20] if len(value)>=20 else '+0000'),'%Y%m%d%H%M%S %z'))
    element.clear()
if values: print(int((min(values)+datetime.timedelta(hours=1)).timestamp()*1000))
PY
)"
  test -n "$test_now" || { echo "ERROR: no programme start in $file" >&2; exit 2; }
  printf 'provider.name=Corpus\nprovider.id=999\nxmltv.files=%s\nlog.configuration=false\nlog.channel=false\nlog.show=false\n' "$input" > "$work/corpus.xmltv.properties"
  if ! (cd "$work"; timeout 300 java -Xmx2g -Dxmltv.test.currentTimeMillis="$test_now" -classpath "$tests:$classes:/work/sagetv/output/server/Sage.jar" xmltv.ImporterHarness "$input"); then
    echo "ERROR: importer mapping failure for $(basename "$file")" >&2
    find "$work" -maxdepth 1 -name '*.log' -type f -exec tail -200 {} \; >&2
    exit 3
  fi
  rm -rf "$work"
done

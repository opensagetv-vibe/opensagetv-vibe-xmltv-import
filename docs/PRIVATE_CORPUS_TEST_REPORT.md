# XMLTV Private Corpus Compatibility Test

Tested 2026-08-25 against every XML/XMLTV guide found in the authorized Unraid
`appdata/xmltvdata` directory. Source files were mounted read-only, were not
committed, and temporary copies were deleted after testing.

## Result

- Files: 13/13 structurally valid and importer-complete
- Channel records passed to Sage: 3,928
- Valid programme records: 323,682
- Shows passed to Sage: 323,682
- Airings passed to Sage: 323,682
- Uncaught exceptions, JVM aborts, or server-data writes: 0

Historical files were replayed using a test-only clock override so their full
mapping paths could be exercised. Normal production time filtering is unchanged.

## Fixes produced by the corpus

1. Truncated numeric XML entities previously caused an unbounded read/buffer
   loop. The filter now returns malformed input to the XML parser without JVM
   memory exhaustion.
2. Short/non-numeric show IDs caused `StringIndexOutOfBoundsException` after a
   show was inserted but before its airing was inserted. SeriesInfo derivation
   is now skipped when no numeric series prefix exists; the valid show and
   airing are retained.

## Source-data finding

`plutotv_guide.xml` contained 9,074 programme elements, of which 6,504 had a
stop timestamp less than or equal to the start timestamp. The importer correctly
rejected those invalid-duration records and imported all remaining 2,570.

Recommendation: fix the Pluto guide generator to emit `stop > start`; do not
weaken the importer duration check because SageTV cannot safely schedule a
zero/negative-duration airing.

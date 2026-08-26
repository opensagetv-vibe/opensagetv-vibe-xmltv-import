# XMLTV Private Corpus Compatibility Test

Historically tested 2026-08-25 against every XML/XMLTV guide found in the
authorized Unraid `appdata/xmltvdata` directory. Source files were mounted
read-only, were not committed, and temporary copies were deleted after testing.

These files are not retained in the repository and were not available for the
version 3.5 profile/reliability replay. The current committed suite uses synthetic
fixtures and a generated stress lineup; the numbers below remain historical
compatibility evidence rather than current 3.5 results.

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
3. A series ID was incorrectly used as the programme/show ID, merging distinct
   episodes. Series IDs are now inputs to deterministic fallback identity and
   remain separate, matching the Schedule Direct data model.
4. The zap2xml corpus uses separate `series`, `season`, and `episode` number
   systems. They are now parsed and a committed two-episode fixture verifies
   two distinct shows and two airings.

The post-fix replay still reports 1,340 IDs whose title/subtitle/description or
season/episode metadata differs within a file. 1,312 are concentrated in one
historical XMLTV snapshot and most are provider-supplied IDs. They are reported,
not rewritten: provider IDs can legitimately retain identity across metadata
corrections, and silently changing them would diverge from Schedule Direct and
damage existing SageTV database continuity.

## Source-data finding

`plutotv_guide.xml` contained 9,074 programme elements, of which 6,504 had a
stop timestamp less than or equal to the start timestamp. The importer correctly
rejected those invalid-duration records and imported all remaining 2,570.

Recommendation: fix the Pluto guide generator to emit `stop > start`; do not
weaken the importer duration check because SageTV cannot safely schedule a
zero/negative-duration airing.

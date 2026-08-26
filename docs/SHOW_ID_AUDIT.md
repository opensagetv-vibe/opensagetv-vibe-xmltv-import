# XMLTV Show-ID Audit

## Schedule Direct comparison

SageTV's Schedule Direct importer uses the provider's programme ID as the
external show ID for both `addShow` and `addAiring`. Episode IDs (`EP...`) and
series IDs (`SH...`) remain separate; `SDUtils.getSeriesForEpisode()` derives a
series reference only for a valid 14-character Sage/Gracenote episode ID.

The XMLTV importer now follows the same identity rule where the input permits:

1. Explicit programme IDs (`tms`, `dd_progid`, configured provider ID, and the
   supported Pluto forms) remain authoritative and unchanged.
2. A `<series-id>` is identity input, not a programme ID. It is combined with
   episode metadata by the deterministic fallback generator.
3. `episode-num` systems `series`, `season`, and `episode`, used by the tested
   zap2xml feed, are recognized.
4. With no programme-level metadata, start time remains part of the fallback;
   this avoids merging unknown episodes, although it cannot identify repeats.

Changing valid provider IDs or globally namespacing them would create duplicate
shows in existing SageTV databases, so this release deliberately does neither.

## Collision and determinism findings

The old `<series-id>` fallback merged all episodes carrying only a series ID.
That is the largest correctness issue and is fixed. Generated CRC input now
uses UTF-8 explicitly, so IDs do not vary with the host default charset.

The importer still uses a 32-bit CRC in its default legacy fallback format so
existing favourites, watched state, and recording history remain compatible.
An opt-in `v2` format now uses provider-scoped SHA-256 identity input and
atomically persisted Show/Series mappings. Its emitted ID retains SageTV's
`EP/SH + numeric SeriesInfo ID + four-digit episode token` convention so Core
can still link series artwork and metadata. Hash-backed collision allocation is
persisted rather than exposing non-numeric digest text that Core cannot parse.
It is intended for clean providers or planned migrations and is never enabled
implicitly. The corpus audit tool is `tests/audit_xmltv_ids.py`.

Some historical provider files reuse explicit IDs while descriptions or other
metadata differ. These are reported but not rewritten: an importer cannot know
whether this is a corrected description, a provider error, or a genuinely
different programme. Provider programme IDs have the same authority here that
they have in Schedule Direct.

## Corpus-backed field review

The 13-file corpus exercises titles, subtitles, descriptions, categories,
credits, dates, icons, ratings, star ratings, video/audio/subtitle flags,
new/live/premiere/previously-shown flags, language, length, URL, and country.
The importer already maps all SageTV-representable guide fields among these.
`length` and `url` have no corresponding argument in the SageTV public EPG show
API and remain intentionally ignored. Wrapper elements such as `video` and
`audio` are consumed through their supported child elements.

The corpus also found 6,504 Pluto records with `stop <= start`; those remain
rejected because zero/negative-duration airings are unsafe for scheduling.

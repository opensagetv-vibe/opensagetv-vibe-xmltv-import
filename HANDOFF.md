# Handoff

## Build and artifact

Run `opensagetv-dev.sh xmltv` from the sibling `opensagetv-build-env`
repository. On Windows run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\opensagetv-dev.ps1 xmltv
```

The unified Ubuntu 26/OpenJDK 11 environment consumes the locally built Core
`Sage.jar`, targets Java 8 bytecode, runs the complete suite, and writes
`output/packages/XMLTVImportPlugin.jar`. The current plugin is version 3.4.
The supported build never starts/stops SageTV and never removes `Wiz.*`, logs,
properties, or user data.

## Runtime behavior

Provider discovery needs no legacy EPG license. With no configuration,
`getLocalMarkets()` returns placeholder provider `867507149 / XMLTV Lineup`, so
the setup UI can offer XMLTV before a provider file exists. The container owns
the safe `Sage.properties` upsert; the JAR does not mutate it.

Version 3.4 reports guide completion truthfully. A required feed, parser,
`run.before`, or SageTV database error makes `updateGuide()` return `false`, and
the plugin does not call `setLineup()` after a partial failure. Feed downloads
are bounded, status checked, gzip aware, conditionally cached, and atomically
replaced. All SAX readers reject DTDs/external entities. Configuration secrets
and URL credentials/queries are redacted from logs.

The new boundaries are `XmltvConfiguration`, `FeedDownloader`,
`SecureXmlReader`/`XmltvParser`, `XmltvDateParser`, `ExternalCommandRunner`,
`ChannelMapper`, `ProgrammeMapper`, `SageGuideWriter`, `ImportResult`, and
`ShowIdAudit`. See `docs/XMLTV_MODERNIZATION.md` for design and regression
coverage. The build currently emits no unchecked raw-collection warnings.

## Show identities

`legacy` remains the default Show-ID strategy and is protected by exact-output
tests. Opt-in `v2` uses provider-scoped SHA-256 identities and atomically
persists show/series mappings to `xmltv-show-id-v2.properties`. Preserve that
file during backup/restore, and never silently switch an established lineup.
`xmltv.show_id.display=description|bonus` exposes final IDs temporarily.

Before migration, run:

```bash
./scripts/show-id-audit.sh --provider-id 999 /path/to/guide.xml > audit.tsv
```

It is read-only unless `--write-map` is supplied. A detected v2 collision exits
with status 2. See `docs/SHOW_ID_AUDIT.md` for the identity policy.

## Channel logos

Both `sagetv.channel.IconDownload` and the historical
`xmltv.channel.IconDownload` key work. Images are downloaded with time/size
limits, decoded independent of extension, normalized to PNG, aspect-preserving,
and bounded to 256x256 by default. Failures do not abort the guide import.

Historical Unraid commissioning on 2026-08-26 used plugin 3.2 and a 107-channel
private feed: 107/107 logos normalized successfully. This is historical
evidence, not a claim that private data was rerun for version 3.4.

## Validation status

The version 3.4 local suite passes secure acquisition/parser tests, provider
reload, failure injection, metadata mapping, multi-source imports, Show-ID
audit/identity tests, channel logos, and a generated 500-channel stress lineup.
Private corpus files are not retained. Their earlier results are documented in
`docs/PRIVATE_CORPUS_TEST_REPORT.md` and must not be presented as a 3.4 replay.

# Handoff

## Build and artifact

Run `opensagetv-dev.sh xmltv` from the sibling `opensagetv-build-env`
repository. On Windows run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\opensagetv-dev.ps1 xmltv
```

The unified Ubuntu 26/OpenJDK 11 environment consumes the locally built Core
`Sage.jar`, targets Java 8 bytecode, runs the complete suite, and writes
`output/packages/XMLTVImportPlugin.jar`. The current plugin is version 3.5.
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
replaced. SAX readers accept the common XMLTV DTD declaration without loading
the external DTD and reject inline/external entity expansion. Configuration
secrets and URL credentials/queries are redacted from logs.

The new boundaries are `XmltvConfiguration`, `FeedDownloader`,
`SecureXmlReader`/`XmltvParser`, `XmltvDateParser`, `ExternalCommandRunner`,
`ChannelMapper`, `ProgrammeMapper`, `SageGuideWriter`, `ImportResult`, and
`ShowIdAudit`. See `docs/XMLTV_MODERNIZATION.md` for design and regression
coverage. The build currently emits no unchecked raw-collection warnings.

Version 3.5 adds optional configuration profiles without changing legacy
resolution when `xmltv.profile` is absent. Profiles and `common.properties`
live beside `Sage.properties`. Resolution order is built-in defaults, ordinary
includes/common, selected profile, then the active `.xmltv.properties` file.
`xmltv.profile=auto` atomically creates a provider-named profile and changes
only the selection line; the existing provider settings remain authoritative.
The container seeds missing shipped profiles but never overwrites a user's
existing root-level profile.

For read-only commissioning against a server directory, run the compiled test
helper from that directory with the plugin and Sage JARs on the classpath:
`xmltv.ProfileRuntimeProbe CONFIG_FILE PROVIDER_ID PROVIDER_NAME`. It verifies
provider discovery and prints the resolved channel/profile fields.

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
evidence, not a claim that private data was rerun for version 3.5.

## Validation status

The version 3.5 local suite passes secure acquisition/parser tests, provider
reload, failure injection, metadata mapping, multi-source imports, Show-ID
audit/identity tests, profile compatibility/precedence/auto generation, channel
logos, and a generated 500-channel stress lineup.
Private corpus files are not retained. Their earlier results are documented in
`docs/PRIVATE_CORPUS_TEST_REPORT.md` and must not be presented as a 3.5 replay.

## Unraid commissioning result

Version 3.5 was commissioned on 2026-08-26 in
`OpenSageTV-sagetv-server-u26-gpu-j11` using the profile-backed FTA_60177
configuration. The real feed declared `xmltv.dtd`, used offset-free timestamps,
and contained duplicate displayed channel numbers. Those findings produced the
DTD containment, local-time parsing, and deterministic station-ID fallback
fixes in this release.

The final forced import reported `success=true`, `configurations=1/1`,
`feeds=1/1`, and `failures=0`. It imported all 107 source channels as 107
distinct SageTV station IDs and processed 29,922 programme records. A container
restart returned healthy and retained the tested JAR at SHA-256
`f41f405dbdaa50977aa23788ab13d656bccb364800189e663c7f2d16369af7fa` in both
the live JAR directory and the current container's embedded startup asset.

The pre-commissioning rollback copy remains at
`server/.backups/xmltv-profile-20260826-132101`. No feed or appdata content is
stored in this repository. The current container was patched without a full
image rebuild as requested; rebuilding `opensagetv-container` from commit
`052b6ff` or later is required before deleting/recreating that container from
an older image tag.

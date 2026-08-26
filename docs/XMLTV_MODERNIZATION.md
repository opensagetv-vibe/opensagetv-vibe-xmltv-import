# XMLTV Importer Modernization

## Scope

Version 3.4 modernizes the importer without changing the default legacy Show-ID
strategy or its SageTV EPG plugin class. It continues to target Java 8 bytecode
and is compiled/tested on OpenJDK 11 in the unified Ubuntu 26 build environment.

## Reliability contract

`XMLTVImportPlugin.updateGuide()` now returns `false` when a required stage
fails. Feed acquisition, secure validation, parsing, channel/show/series/airing
database calls, and `run.before` execution contribute to one `ImportResult`.
`setLineup()` is called only after every configured source completes without a
failure. Java `Error` instances are not converted into a false success.

This intentionally changes the historical behavior where broad exception
handlers could log an error, install a partial lineup, and still return success.

## Feed acquisition

`FeedDownloader` handles local paths, `file:`, HTTP, and HTTPS sources. It adds:

- bounded connect/read time, maximum decoded bytes, and redirect count;
- gzip detection by content rather than filename alone;
- validation of HTTP status and redirect targets;
- conditional requests using ETag and Last-Modified metadata;
- temporary-file writes followed by atomic replacement when supported; and
- cache preservation on failure without importing that stale cache as success.

Credentials and URL queries are redacted from configuration/log output.

## XML containment

`SecureXmlReader` creates the SAX reader used for both validation and import.
Secure processing is enabled; DOCTYPE declarations, external general entities,
external parameter entities, and external DTD/schema access are disabled. A
rejecting entity resolver is installed as defense in depth. Error and fatal SAX
callbacks propagate. Per-element accumulated text is bounded.

The optional invalid-character filter remains supported, but it no longer
weakens the parser security configuration.

## Extracted boundaries

- `XmltvConfiguration`: typed, bounded configuration and redacted diagnostics.
- `FeedDownloader`: deterministic source acquisition and HTTP cache behavior.
- `SecureXmlReader` / `XmltvParser`: one hardened parser construction path.
- `XmltvDateParser`: strict, immutable `java.time` timestamp/date parsing.
- `ExternalCommandRunner`: drained output, timeout, exit status, and teardown.
- `ChannelMapper`: station-ID and channel-number calculations.
- `ProgrammeMapper`: safe season/episode/part bounds and airing flags.
- `SageGuideWriter`: narrow adapter around the SageTV public guide API.
- `ShowIdGenerator` / `ShowIdAudit`: production identity and migration audit.

Importer parsing state is instance-owned rather than static. Configuration
files are re-read for provider discovery, relative includes resolve relative to
their parent, duplicate/cyclic includes terminate, and invalid providers are
skipped without hiding valid providers.

## Mapping corrections

- Preferred-language title, subtitle, and description selection is supported.
- Movie ratings such as `PG` and `PG-13` are no longer discarded by a TV-only
  check; mapped TV ratings also populate SageTV's parental-rating field.
- The previously unreachable `<subtitles>` mapping now emits the subtitle flag.
- `<previously-shown start="...">` is parsed and retained.
- Short season/episode values and multipart values cannot overrun fixed fields.
- A repeated channel across multiple configured feeds remains valid and appears
  once in the final lineup.

Default channel station-ID arithmetic is preserved, including legacy decimal
channel behavior, because changing it would detach an established lineup.

## Tests

`scripts/build.sh` compiles production sources with `javac --release 8` and
runs all tests before packaging. Coverage includes:

- missing feeds, malformed/truncated XML, hostile DOCTYPE/XXE input, rejected
  SageTV writes, and the invariant that failed imports never call `setLineup`;
- local/HTTP/gzip acquisition, redirects, conditional 304 responses, server
  errors, byte limits, read timeouts, and cache preservation;
- strict timestamps with multiple UTC offsets and invalid calendar values;
- command success, output draining, non-zero exit, and timeout termination;
- provider add/delete reload, relative includes, and invalid configurations;
- rating/language/previously-shown/subtitle/season/episode metadata;
- multiple-source channel retention and a generated 500-channel lineup;
- legacy/v2 identity compatibility, persistence, collision detection, and the
  dry-run migration audit; and
- existing channel-logo normalization and malformed-entity regressions.

The plugin JAR is written to `output/packages/XMLTVImportPlugin.jar`; individual
logs are written to `output/test-results/`.

## Version 3.5 profiles

Reusable format profiles are optional implicit includes located in the SageTV
server root. Legacy files without `xmltv.profile` take the unchanged code path.
The resolver keeps ordinary include chains and applies explicit provider-file
values last. Automatic generation writes a provider-named profile and updates
the selection atomically while leaving the original settings in place. Tests
cover legacy behavior, all precedence levels, missing profiles, include cycles,
case-insensitive profile selection, safe names, non-overwrite behavior, and
temporary-file cleanup.

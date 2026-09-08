# Changelog

## Unreleased

- Added disabled-by-default TMDB enrichment through the standalone
  `opensagetv-vibe-tmdb` public facade. It fills only missing programme fields,
  bounds and deduplicates lookups, keeps Show IDs stable, and fails open when
  the service is absent, stopped, offline, rate-limited, ambiguous, or returns
  no match. Unit and end-to-end TMDB-off/on import regressions pass.

- Replaced private commissioning examples with generic OTA/IPTV profiles and
  synthetic paths before public release; no feed, credentials, schedules,
  logs, or appdata are distributed.
- Added repository checks, third-party notices, and contribution/security
  guidance for public development.
- Added the common AI takeover, task, verified update, resumable unified gate,
  and changed-files handoff ZIP workflow without altering importer behavior.
- Updated Unraid commissioning references for the canonical
  `sagetv-vibe-server-u26-gpu-j11` container and appdata path.
- Renamed the project and its unified build and runtime-container references to
  the full `opensagetv-vibe-*` namespace; Java package and plugin class names
  remain unchanged for SageTV compatibility.
- Added optional root-level `xmltv_<name>.profile` inheritance while preserving
  unchanged behavior for every existing `xmltv.properties`,
  `*.xmltv.properties`, and `include=` configuration.
- Defined precedence as built-in defaults, includes/common, selected profile,
  then the active provider file so every explicit legacy setting still wins.
- Added atomic `xmltv.profile=auto` migration: create a provider-named profile,
  include `common.properties`, retain instance-specific values, preserve every
  existing override, and persist the generated selection without overwriting an
  existing profile.
- Added Generic, EPG123, Zap2XML, Pluto, Threadfin, xTeVe, IPTV, OTA/FTA, and
  generic OTA_Local root profile plus shared safe defaults.
- Refactored shipped provider examples to select profiles rather than duplicate
  format settings, and added precedence/compatibility/auto-generation tests.
- Changed the guide-update contract to return failure for acquisition, parser,
  command, or SageTV database errors and to withhold `setLineup` after any
  partial failure.
- Added bounded local/file/HTTP(S) feed acquisition with gzip detection,
  redirect/status validation, ETag/Last-Modified caching, and atomic writes
  that preserve the prior cache on failure.
- Hardened every SAX parser against XXE, external DTD/schema access, fatal-error
  continuation, truncated XML, and unbounded element text while accepting the
  standard XMLTV `xmltv.dtd` declaration without loading it.
- Accepted XMLTV timestamps without an explicit UTC offset using the SageTV
  server's configured time zone, while retaining strict calendar validation.
- Preserved every source channel when two XMLTV channel IDs resolve to the same
  legacy numeric station ID by assigning a deterministic, provider-scoped
  fallback instead of failing or silently dropping a station.
- Extracted typed configuration, feed download, secure parsing, date parsing,
  command execution, channel/programme mapping, and Sage guide-write adapters.
- Replaced shared mutable date parsers with strict `java.time` parsing and made
  importer parsing state instance-local.
- Added bounded `run.before` execution with continuously drained output,
  timeout/process teardown, checked exit status, and command redaction.
- Fixed preferred-language selection, movie/TV parental ratings, subtitle
  flags, `previously-shown` dates, short season/episode fields, multipart
  bounds, provider reload/includes, and repeated channels across input files.
- Added a read-only `ShowIdAudit` CLI comparing legacy and v2 identity, with an
  explicit opt-in for writing a migration map and collision exit status.
- Added hostile/truncated/metadata/multi-source/provider-collision fixtures,
  HTTP acquisition tests, failure injection, and a 500-channel stress import.
- Compiled with `javac --release 8`; the modernized build completes without the
  prior raw-collection/unchecked compiler warnings.
- Extracted generated Show-ID logic from the SAX importer into a separately
  tested identity component while preserving exact legacy CRC32 outputs.
- Added opt-in `xmltv.show_id.strategy=v2`, using provider-scoped SHA-256
  identities with atomic persistent Show/Series mapping and deterministic
  collision handling. Explicit provider IDs remain authoritative.
- Kept v2 external IDs compatible with SageTV Core's numeric SeriesInfo lookup
  convention while retaining the full SHA-256 identity in the mapping file.
- Added optional `xmltv.show_id.display=description|bonus` diagnostics so the
  final external ID can be inspected in SageTV; the default remains `none`.
- Added legacy compatibility, v2 determinism, provider isolation, mapping
  reload, ambiguous-field, invalid-strategy, and UI-display regressions.
- Removed the obsolete in-container clean-build script that stopped SageTV and
  deleted `Wiz.*`, logs, and server text files; supported builds remain
  non-destructive in the unified development container.
- Added concrete String types to Show identity/description/people collections
  and the split-movie map, reducing unchecked operations in the refactored path.
- Restored the legacy `xmltv.channel.IconDownload` configuration alias; the
  newer `sagetv.channel.IconDownload` name takes precedence when both exist.
- Replaced extension-based channel-logo writes with validated downloads and
  atomic PNG output. Logos now preserve aspect ratio/alpha and are downscaled
  to a configurable 256x256 maximum without upscaling.
- Added HTTP timeouts, encoded/decoded size limits, malformed-image handling,
  directory creation, and regression coverage for channel-logo processing.
- Removed the obsolete SageTV EPG license expectation by registering the XMLTV import class whenever its JAR is installed.
- Expose a safe default `XMLTV Lineup` provider when configuration is absent, keeping the plugin selectable during initial setup.
- Preserved upstream history and tags.
- Added a non-destructive OpenJDK 11 build targeting Java 8 bytecode.
- Added XML filtering and malformed-entity regression tests.
- Fixed an unbounded loop/memory-exhaustion path on truncated numeric entities.
- Integrated compilation and packaging into `opensagetv-vibe-build-env`.
- Tested all 13 authorized XMLTV corpus files through the real importer API.
- Preserved airings for valid short/non-numeric show IDs by safely skipping only unavailable SeriesInfo derivation.
- Added a test-only clock override for deterministic historical guide replay.
- Matched Schedule Direct identity semantics by keeping series IDs separate from programme IDs.
- Added zap2xml `series`, `season`, and `episode` number-system support and a two-episode regression test.
- Made fallback IDs charset-stable and fixed the one-actor movie bounds error.
- Added a reusable corpus identity/field-coverage audit and documented collision policy.

## 3.5 - 2026-08-26

- Added root-level provider profiles, explicit provider-over-profile precedence,
  atomic `xmltv.profile=auto` generation, and common/provider examples.
- Added safe standard-DTD handling, strict offset-free timestamps, and
  deterministic provider-scoped station-ID collision fallback.

## 3.4 - 2026-08-26

- Made required-stage failures abort lineup replacement; added bounded atomic
  feed acquisition, secure parsing, typed components, strict dates, safe
  command execution, Show-ID auditing, metadata fixes, and stress/hostile tests.

## 3.3 - 2026-08-26

- Extracted generated Show-ID logic, added opt-in provider-scoped SHA-256 IDs
  and collision-safe persistent maps, and added optional SageTV ID display.
- Removed the destructive legacy helper that stopped SageTV and deleted server
  data during a build.

## 3.2 - 2026-08-26

- Restored `xmltv.channel.IconDownload`, added validated atomic PNG conversion
  and bounded scaling, and contained invalid/oversized logo failures.

## 3.0 - 2022-09-16

- Added per-provider/debug logging, automatic discovery of provider property
  files, stable local feed copies, people/character/rating/show-icon metadata,
  custom ShowID/series-ID selection, and cached channel configuration.
- Reworked channel creation for EPG123/external channel data and CRC fallback,
  added airing bit masks and placeholder episode handling, and renamed legacy
  channel-number/logo settings into the `sagetv.*` namespace.

## 2.11 - 2022-11-13

- Retained Java compatibility by using `String.isEmpty()` rather than
  `String.isBlank()`.

## 2.10 - 2022-11-13

- Corrected programme-date handling to use the `previously-shown` date.

## 2.09 - 2022-11-13

- Used programme start time when no date is supplied and adopted
  `addAiringPublic2` masks for live/new, audio, quality, premiere, and multipart
  metadata.

## 2.08 - 2022-01-28

- Prevented channel-description crashes, retained a bounded amount of prior
  guide data on initial import, improved Pluto/onscreen/common episode values,
  centralized configuration, added channel offsets, and auto-discovered
  provider property files.

## 2.06 - 2022-01-18

- Added logging controls, provider-aware station-ID calculation, and safe logo
  filenames for channel names containing filesystem punctuation.

## 2.04 - 2022-01-16

- Added configurable short/long display-name selection, regex filtering,
  channel-number tag/index selection, and opt-in channel logo downloads.

## 2.03 - 2022-01-14

- Removed deprecated boxed-integer construction and migrated SAX reader
  creation to `SAXParserFactory`.

## 2.02 - 2022-01-14

- Fixed the first-channel marker, duplicate title years, TMS episode IDs,
  onscreen/common SxEx behavior, and channel short-name selection; began logo
  ingestion.

## 2.0 - 2022-01-13

- Derived displayed channel numbers from numeric channel IDs or display names,
  including decimal subchannels.

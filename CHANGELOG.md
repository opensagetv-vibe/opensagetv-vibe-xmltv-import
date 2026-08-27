# Changelog

## Unreleased

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
  commissioned FTA_60177 root profiles plus shared safe defaults.
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

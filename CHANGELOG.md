# Changelog

## Unreleased

- Preserved upstream history and tags.
- Added a non-destructive OpenJDK 11 build targeting Java 8 bytecode.
- Added XML filtering and malformed-entity regression tests.
- Fixed an unbounded loop/memory-exhaustion path on truncated numeric entities.
- Integrated compilation and packaging into `opensagetv-build-env`.
- Tested all 13 authorized XMLTV corpus files through the real importer API.
- Preserved airings for valid short/non-numeric show IDs by safely skipping only unavailable SeriesInfo derivation.
- Added a test-only clock override for deterministic historical guide replay.
- Matched Schedule Direct identity semantics by keeping series IDs separate from programme IDs.
- Added zap2xml `series`, `season`, and `episode` number-system support and a two-episode regression test.
- Made fallback IDs charset-stable and fixed the one-actor movie bounds error.
- Added a reusable corpus identity/field-coverage audit and documented collision policy.

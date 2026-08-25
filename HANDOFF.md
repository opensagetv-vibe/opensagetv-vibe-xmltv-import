# Handoff

Run `opensagetv-dev.sh xmltv` from the sibling build-environment repository.
The build consumes the locally built Core `Sage.jar` and writes the plugin JAR,
tests, warning log, and configuration examples under `output/`.

The supported build never starts/stops SageTV and never removes `Wiz.*`, logs,
or properties. Runtime installation is optional and does not force an EPG key.

Legacy raw-collection warnings are recorded in `output/test-results/javac.log`
and should be reduced incrementally with representative XML fixtures.

See `docs/PRIVATE_CORPUS_TEST_REPORT.md` for the 13-file, 323,682-programme
compatibility run. Private inputs are intentionally not retained.

# Handoff

Run `opensagetv-dev.sh xmltv` from the sibling build-environment repository.
The build consumes the locally built Core `Sage.jar` and writes the plugin JAR,
tests, warning log, and configuration examples under `output/`.

The supported build never starts/stops SageTV and never removes `Wiz.*`, logs,
or properties. Runtime installation is optional and does not force an EPG key.

Provider discovery has an explicit regression test. With no configuration,
`getLocalMarkets()` returns the stable placeholder provider `867507149 / XMLTV
Lineup`, allowing the setup UI to offer XMLTV before a provider file exists.
The logger is initialized before provider enumeration, so an unconfigured
plugin cannot fail with a null log target. The container owns the safe property
upsert; the plugin JAR itself does not mutate `Sage.properties`.

Legacy raw-collection warnings are recorded in `output/test-results/javac.log`
and should be reduced incrementally with representative XML fixtures.

See `docs/PRIVATE_CORPUS_TEST_REPORT.md` for the 13-file, 323,682-programme
compatibility run. Private inputs are intentionally not retained.

See `docs/SHOW_ID_AUDIT.md` for the Schedule Direct comparison, identity
precedence, collision policy, and corpus field-coverage findings. Run
`python3 tests/audit_xmltv_ids.py /path/to/xmltvdata` for aggregate diagnostics.

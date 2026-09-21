# OpenSageTV Vibe XMLTV Import tasks

This is the only active XMLTV backlog. Completed work is removed and recorded
in `CHANGELOG.md` and `HANDOFF.md`.

- [ ] Replace Vibe Core's automatic XMLTV importer discovery/property repair
  with a stock-compatible SageTV Standard-plugin wrapper. Registration or
  repair of `epg/epg_import_plugin` must require explicit user enablement or an
  empty/known-obsolete value, preserve every valid third-party importer, and
  safely handle restart, disable, uninstall, and rollback on an unmodified
  stock SageTV server. Add migration and regression tests; do not treat this as
  a solution for simultaneous multiple EPG import plugins.
- [ ] Perform final clean-server provider/profile commissioning with all
  supported sample feeds and record lineup/logo/ShowID results.
- [ ] Add any newly encountered provider-specific parsing behavior only with a
  redacted committed fixture and regression test.

# OpenSageTV Vibe XMLTV Import tasks

> **Pre-commit task maintenance:** Immediately before every repository commit, move
> completed `[x]` items out of active sections and into
> `## Checklist change ledger`. Preserve IDs, evidence, and context; never
> discard completion history. Active sections contain unchecked work only.

This is the only active XMLTV backlog. Completed work moves to the checklist
change ledger; release evidence is also recorded in `CHANGELOG.md` and
`HANDOFF.md`.

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

## Checklist change ledger

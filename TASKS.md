# OpenSageTV Vibe XMLTV Import tasks

This is the only active XMLTV backlog. Completed work is removed and recorded
in `CHANGELOG.md` and `HANDOFF.md`.

- [ ] Add an opt-in adapter to the standalone `opensagetv-vibe-tmdb` service.
  Preserve feed-supplied metadata and stable Show IDs, batch/deduplicate
  lookups, never access the TMDB SQLite schema directly, and keep a successful
  XMLTV import usable when the TMDB plugin is absent, unconfigured, offline,
  rate-limited, or returns no unambiguous match.
- [ ] Perform final clean-server provider/profile commissioning with all
  supported sample feeds and record lineup/logo/ShowID results.
- [ ] Add any newly encountered provider-specific parsing behavior only with a
  redacted committed fixture and regression test.

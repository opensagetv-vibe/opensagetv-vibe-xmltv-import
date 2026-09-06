# OpenSageTV Vibe XMLTV Import contributor rules

Read `README.md`, `HANDOFF.md`, `TASKS.md`, `WORKFLOW.md`, and
`docs/XMLTV_MODERNIZATION.md` first. `TASKS.md` is the sole local backlog;
remove completed work and update `CHANGELOG.md`/`HANDOFF.md` immediately.

Preserve license-free provider discovery, existing `xmltv.properties`
compatibility, profile precedence, stable ShowID mappings, secure XML parsing,
bounded/normalized logo handling, and graceful failure without server crashes.
Never package feed credentials, downloaded schedules, SageTV appdata, or logs.
Do not create per-version/prompt/review documentation.

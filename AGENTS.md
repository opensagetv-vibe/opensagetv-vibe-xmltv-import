# OpenSageTV Vibe XMLTV Import contributor rules

Read `README.md`, `HANDOFF.md`, `TASKS.md`, `WORKFLOW.md`, and
`docs/XMLTV_MODERNIZATION.md` first. `TASKS.md` is the sole local backlog;
remove completed work and update `CHANGELOG.md`/`HANDOFF.md` immediately.

Preserve license-free provider discovery, existing `xmltv.properties`
compatibility, profile precedence, stable ShowID mappings, secure XML parsing,
bounded/normalized logo handling, and graceful failure without server crashes.
Never package feed credentials, downloaded schedules, SageTV appdata, or logs.
Do not create per-version/prompt/review documentation.


## Stock-server test-control policy

- For any new testing, commissioning, diagnostic, or automation control, first
  implement or extend the stock-compatible `opensagetv-vibe-core-MCP-Plugin`
  using supported `sage.SageTV.api`/`apiUI` calls and verify it against an
  unmodified stock SageTV server.
- Do not patch `Sage.jar`, add private MiniClient events, or change Core merely
  to make a test easier. Existing public APIs, the bounded MCP bridge, and
  external test tooling are the required first option.
- Change Core only when the required production runtime behavior cannot be
  expressed through the stock plugin/API boundary. Document the proven API
  gap, keep the extension optional and negotiated with a safe stock fallback,
  and verify older clients and installations remain unaffected.

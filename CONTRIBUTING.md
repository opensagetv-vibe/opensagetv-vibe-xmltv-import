# Contributing

Read `AGENTS.md`, `README.md`, `WORKFLOW.md`, `TASKS.md`, and
`docs/XMLTV_MODERNIZATION.md`. Use the sibling unified build environment:

```bash
./dev.sh test
./dev.sh validate
./dev.sh build
```

Every parser/provider fix needs a redacted or synthetic regression fixture.
Preserve legacy `xmltv.properties`, profile precedence, Show-ID stability,
bounded downloads, secure XML parsing, and truthful failure reporting. Update
`CHANGELOG.md`, `HANDOFF.md`, and `TASKS.md` in the same change.

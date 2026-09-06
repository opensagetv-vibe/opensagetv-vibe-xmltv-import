# Common project workflow

Run `dev.cmd` or `./dev.sh` with `test`, `validate`, `build`, `install`, or
`all`. The unified XMLTV build compiles the JAR and runs its complete regression
suite. Install is intentionally `SKIPPED`; the container stages the tested JAR.

Put update ZIPs in `artifacts/downloads`, run `update.cmd`/`update.sh`, and use
`create_ai_handoff_zip.cmd` to create the verified changed-files package. See
the sibling build environment's `WORKFLOW.md` for the common format.

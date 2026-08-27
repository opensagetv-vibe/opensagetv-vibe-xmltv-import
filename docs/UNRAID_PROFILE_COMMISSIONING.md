# Unraid XMLTV Profile Commissioning

## Commissioned layout

The 2026-08-26 commissioning used the clean development instance now named
`opensagetv-vibe-server`. Profiles are ordinary files in the
SageTV server root; no private XMLTV feed or other appdata is committed.

The active provider file contains only instance values and an explicit profile
selection:

```properties
provider.name=FTA_60177
provider.id=2
xmltv.files=/unraid/appdata/xmltvdata/xmltv_60177.xml
sagetv.channel.IconDownload=true
xmltv.profile=FTA_60177
```

`xmltv_FTA_60177.profile` contains the reusable feed-format mapping and includes
`common.properties`. Settings in the active provider file always override both
files. An existing provider file without `xmltv.profile` follows the exact
legacy include/loading path.

## Verified result

- Plugin version: 3.5
- JAR SHA-256: `f41f405dbdaa50977aa23788ab13d656bccb364800189e663c7f2d16369af7fa`
- Feed input: 107 channels and 29,922 programmes
- SageTV station output: 107 distinct station IDs
- Import result: `success=true, configurations=1/1, feeds=1/1, failures=0`
- Restart result: container healthy; live and embedded JAR hashes unchanged

The feed exposed three compatibility cases covered by automated regressions:
the standard `xmltv.dtd` declaration, offset-free XMLTV timestamps, and
different source stations sharing a displayed channel number.

## Validation

Check the provider log after a forced guide update:

```bash
grep -E 'Import summary|Import failure|Station ID collision' \
  /mnt/user/appdata/opensagetv-vibe-server/server/xmltv_FTA_60177.log | tail -n 20
```

A valid pass ends with exactly one successful summary and zero failures. Old
failed attempts remain in the append-only log, so use the timestamps around the
latest `Start guide update` and `Finish guide update` rather than grepping for
the mere existence of any historical error.

## Rollback

The untouched pre-commissioning files are stored under:

```text
/mnt/user/appdata/opensagetv-vibe-server/server/.backups/xmltv-profile-20260826-132101
```

That directory contains `Sage.properties`, `XMLTVImportPlugin.jar`,
`xmltv.properties`, and `xmltv_ANT_60177.xmltv.properties`. Stop SageTV before
restoring them. The original JAR must be restored to both the live
`server/JARs` location and `/usr/local/share/sagetv-options/xmltv` inside the
current container; otherwise its startup copy will reinstall version 3.5.

This deployment intentionally patched the current container instead of
rebuilding the complete image. A normal restart is verified, but deleting and
recreating from the older image tag discards its writable-layer embedded JAR.
Build the updated `opensagetv-vibe-container` source for recreation-safe deployment.

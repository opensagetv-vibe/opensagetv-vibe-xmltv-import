# Unraid XMLTV profile commissioning

Profiles are ordinary files in the SageTV server root. No private XMLTV feed,
credentials, schedules, logs, or appdata belong in this repository.

Create a provider file containing only instance-specific values and a profile
selection. For example:

```properties
provider.name=OTA_Local
provider.id=2
xmltv.files=/unraid/appdata/xmltvdata/guide.xml
sagetv.channel.IconDownload=true
xmltv.profile=OTA_Local
```

`xmltv_OTA_Local.profile` provides reusable feed-format mappings and includes
`common.properties`. Values in the provider file override both. Existing
provider files without `xmltv.profile` retain the legacy loading path.

Build and test the JAR in `opensagetv-vibe-build-env`, install it through a
verified component update, restart only the test SageTV container, select the
provider, and force one guide update. A valid log ends with one successful
summary and zero failures. Verify lineup count, distinct station IDs, logo
dimensions, and stable Show IDs before using the provider in production.

The component update workflow creates a timestamped backup under the selected
appdata root. Use the container repository's rollback command rather than
copying files from a running container layer.

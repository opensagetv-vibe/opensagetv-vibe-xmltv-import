# OpenSageTV XMLTV Import Plugin

This Apache-2.0 project builds reproducibly against SageTV Core using the common
Ubuntu 26/OpenJDK 11 development image. It does not compile inside or delete
data from a running SageTV server.

Run `./opensagetv-dev.sh xmltv` from the sibling `opensagetv-build-env`
repository. On Windows use `powershell -NoProfile -ExecutionPolicy Bypass -File
.\opensagetv-dev.ps1 xmltv`. Output is under `output/`.

Installation registers `xmltv.XMLTVImportPlugin` so XMLTV appears as an EPG
option without any legacy SageTV license key. No lineup is selected and no
guide import occurs until the user chooses a provider. If configuration is
missing, a safe `XMLTV Lineup` placeholder remains selectable while examples
are available under `.config/xmltv-examples`.

## How to install Plugin with SageTV 
1.  Stop SageTV Server
2.  Rename on add/modify/ xmltv_EXAMPLE.properties examples
3.  Copy all files and folder contents(not folder) of folder SAGETV_SERVER_ROOT to SageTV folder.  Only Jar and *.properties are required if not compiling required
4.  Add the following line in Sage.properties epg/epg_import_plugin=xmltv.XMLTVImportPlugin
5.  Start SageTV Server
6.  Monitor in server folder xmltv.log, sagetv_0.txt,Sage.properties.  If epg/epg_import_plugin=xmltv.XMLTVImportPlugin is removed from Sage.properties something is installed not correctly
7.  In SageTV guide setup select the XMLTV provider. No SageTV license or trial key is required.

## Supported Unraid container installation

The `opensagetv-container` release embeds the tested JAR, copies it into
`server/JARs`, and safely upserts the plugin property at startup. Do not edit
`Sage.properties` with the obsolete `sed`/`sudo` commands from the historical
project. The CA template's `XMLTV EPG Provider` value must remain
`xmltv.XMLTVImportPlugin`, and XML source paths under `/mnt/user` are visible in
the container below `/unraid`.

Compile on the unified Ubuntu 26 build image, then move the finished container
image to a low-CPU Unraid system. Building the plugin inside the running server
container is unsupported.

## Channel logos

Set `sagetv.channel.IconDownload=true` in the selected provider's
`*.xmltv.properties` file to download each channel's `<icon src="...">` URL.
The historical key `xmltv.channel.IconDownload=true` remains supported for
existing configurations. The importer creates `ChannelLogos`, accepts image
URLs without a useful file extension, and converts supported input formats to
PNG. Logos preserve aspect ratio and transparency, are never enlarged, and are
downscaled to a 256x256 bounding box by default. The limits can be changed with:

```properties
sagetv.channel.IconMaxWidth=256
sagetv.channel.IconMaxHeight=256
```

Values are constrained to 16-2048 pixels. Downloads have connection/read
timeouts, an 8 MiB encoded-size limit, and a 16-megapixel decoded-size limit so
a bad or hostile icon cannot consume unbounded server resources. Failures are
logged to `xmltv.log` and do not abort the guide import.

## Generated Show IDs

The default remains fully compatible with existing SageTV databases:

```properties
xmltv.show_id.strategy=legacy
```

For a clean provider/database, the opt-in v2 strategy uses a provider-scoped
SHA-256 identity instead of the historical 32-bit CRC fallback:

```properties
xmltv.show_id.strategy=v2
xmltv.show_id.v2.map_file=xmltv-show-id-v2.properties
```

When `provider.id` is configured it is the provider namespace; otherwise the
provider name is used. The map is written atomically in the SageTV server
directory and keeps both
external Show IDs and SageTV's numeric SeriesInfo IDs stable if collision
resolution is ever required. Back up this file with `Sage.properties` and
`Wiz.bin`. Explicit provider programme IDs such as `tms`/`dd_progid` are never
rewritten by either strategy.

The emitted v2 ID deliberately retains SageTV's numeric series prefix and
four-digit episode token. The full SHA-256 identity remains in the mapping;
putting hexadecimal digest text directly in the external ID would prevent
SageTV Core from linking SeriesInfo and artwork.

Do not enable v2 on an established lineup without planning a migration. The
new fallback IDs are intentionally different, so SageTV can initially treat
existing programmes, favourites, and watched history as different records.

For commissioning or troubleshooting, expose the final ID in the SageTV UI:

```properties
xmltv.show_id.display=description
```

Valid values are `none` (default), `description`, and `bonus`. Disable the
display setting after validation if you do not want IDs shown in guide text.

# Examples `.properties` for channel
![](https://github.com/jzhvymetal/SageTv_XMLTVImportPlugin/blob/main/SAGETV_SERVER_ROOT_Contents/xmltv_src/DOC/PROP_Channel.png)

# Examples `.properties` for show
![](https://github.com/jzhvymetal/SageTv_XMLTVImportPlugin/blob/main/SAGETV_SERVER_ROOT_Contents/xmltv_src/DOC/PROP_Show.png)

# Third-party notices

This project is licensed under Apache-2.0; see `LICENSE`. It compiles against
the SageTV public API from `opensagetv-vibe-core`, which remains under its own
Apache-2.0 and third-party terms. The SageTV JAR is a build input and is not
embedded in `XMLTVImportPlugin.jar`.

Channel-logo decoding uses Java platform image APIs. Test fixtures are
synthetic and contain no provider schedules, credentials, or user appdata.

Optional metadata enrichment calls the separately distributed
`opensagetv-vibe-tmdb` plugin through its public facade. This XMLTV artifact
does not bundle TMDB libraries, credentials, or cached API content.

This product uses the TMDB API but is not endorsed or certified by TMDB.

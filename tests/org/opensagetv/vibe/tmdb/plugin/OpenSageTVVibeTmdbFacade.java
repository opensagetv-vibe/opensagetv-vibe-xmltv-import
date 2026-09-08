package org.opensagetv.vibe.tmdb.plugin;

/** Test-only reflection target; never packaged in XMLTVImportPlugin.jar. */
public final class OpenSageTVVibeTmdbFacade {
    public static boolean available = true;
    public static boolean fail;
    public static int calls;

    private OpenSageTVVibeTmdbFacade() {}

    public static boolean isAvailable() { return available; }

    public static String[][] getProgrammeMetadata(String type, String title, int year,
            int season, int episode) {
        calls++;
        if (fail) throw new IllegalStateException("synthetic failure");
        return new String[][] {
            {"tmdb_id", "123"}, {"media_type", type}, {"title", title},
            {"description", "TMDB description"}, {"year", "2024"},
            {"original_language", "en"}, {"genres", "Drama\u001fMystery"},
            {"countries", "United States\u001fCanada"},
            {"episode_name", season > 0 ? "Episode from TMDB" : ""},
            {"episode_description", season > 0 ? "Episode description" : ""},
            {"episode_air_date", season > 0 ? "2024-03-04" : ""},
        };
    }

    public static void reset() { available = true; fail = false; calls = 0; }
}

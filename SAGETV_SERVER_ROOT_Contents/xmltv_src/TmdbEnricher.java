/*
 * Copyright 2026 OpenSageTV Vibe contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package xmltv;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

/** Optional fail-open bridge to the standalone OpenSageTV Vibe TMDB service. */
final class TmdbEnricher {
    private static final String FACADE =
            "org.opensagetv.vibe.tmdb.plugin.OpenSageTVVibeTmdbFacade";
    private static final TmdbEnricher DISABLED = new TmdbEnricher(false, 0, null, null);

    private final boolean enabled;
    private final int maxLookups;
    private final Method availableMethod;
    private final Method metadataMethod;
    private final Map<String, Map<String, String>> results =
            new LinkedHashMap<String, Map<String, String>>();
    private int lookups;
    private int matches;
    private int failures;
    private String status;

    private TmdbEnricher(boolean enabled, int maxLookups,
            Method availableMethod, Method metadataMethod) {
        this.enabled = enabled;
        this.maxLookups = maxLookups;
        this.availableMethod = availableMethod;
        this.metadataMethod = metadataMethod;
        this.status = enabled ? "ready" : "disabled";
    }

    static TmdbEnricher create(Properties properties) {
        if (!Boolean.parseBoolean(properties.getProperty("xmltv.tmdb.enrich", "false").trim())) {
            return DISABLED;
        }
        int max = positiveInt(properties.getProperty("xmltv.tmdb.max_lookups_per_import"), 250);
        try {
            Class<?> facade = Class.forName(FACADE);
            Method available = facade.getMethod("isAvailable");
            Method metadata = facade.getMethod("getProgrammeMetadata", String.class,
                    String.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            if (!Boolean.TRUE.equals(available.invoke(null))) {
                TmdbEnricher result = new TmdbEnricher(false, max, available, metadata);
                result.status = "plugin unavailable; source metadata preserved";
                return result;
            }
            return new TmdbEnricher(true, max, available, metadata);
        } catch (ClassNotFoundException e) {
            TmdbEnricher result = new TmdbEnricher(false, max, null, null);
            result.status = "plugin not installed; source metadata preserved";
            return result;
        } catch (ReflectiveOperationException e) {
            TmdbEnricher result = new TmdbEnricher(false, max, null, null);
            result.status = "plugin API unavailable; source metadata preserved";
            return result;
        } catch (LinkageError e) {
            TmdbEnricher result = new TmdbEnricher(false, max, null, null);
            result.status = "plugin linkage unavailable; source metadata preserved";
            return result;
        }
    }

    /** Enrich only empty source fields. Existing XMLTV values always win. */
    void enrichMissing(Show show, List<String> translatedCategories, String originalCategory) {
        if (!enabled || show == null || !needsMetadata(show, translatedCategories)
                || lookups >= maxLookups) {
            return;
        }
        String mediaType = "Movie".equals(originalCategory) ? "movie" : "tv";
        int year = parseYear(show.year);
        String key = mediaType + '\u001f' + safe(show.title).toLowerCase(Locale.ROOT)
                + '\u001f' + year + '\u001f' + show.season + '\u001f' + show.episode;
        Map<String, String> values = results.get(key);
        if (values == null) {
            lookups++;
            values = lookup(mediaType, show.title, year, show.season, show.episode);
            results.put(key, values);
        }
        if (values.isEmpty()) return;
        matches++;
        String episodeDescription = values.get("episode_description");
        if (show.descriptions.isEmpty()) {
            addIfPresent(show.descriptions, firstNonEmpty(episodeDescription,
                    values.get("description")));
        }
        if (isEmpty(show.episodeName)) show.episodeName = emptyToNull(values.get("episode_name"));
        if (isEmpty(show.year)) show.year = emptyToNull(values.get("year"));
        if (isEmpty(show.language)) show.language = emptyToNull(values.get("original_language"));
        if (show.date == null) show.date = parseDate(values.get("episode_air_date"));
        if (show.countries.isEmpty()) addSeparated(show.countries, values.get("countries"));
        if (translatedCategories.isEmpty()) addSeparated(translatedCategories, values.get("genres"));
    }

    String summary() {
        return "TMDB enrichment " + status + "; lookups=" + lookups
                + ", matches=" + matches + ", failures=" + failures
                + ", deduplicated=" + Math.max(0, matches - results.size());
    }

    private Map<String, String> lookup(String type, String title, int year,
            int season, int episode) {
        if (isEmpty(title)) return Collections.emptyMap();
        try {
            if (!Boolean.TRUE.equals(availableMethod.invoke(null))) {
                status = "plugin stopped; source metadata preserved";
                return Collections.emptyMap();
            }
            Object value = metadataMethod.invoke(null, type, title, Integer.valueOf(year),
                    Integer.valueOf(season), Integer.valueOf(episode));
            if (!(value instanceof String[][])) return Collections.emptyMap();
            Map<String, String> result = new LinkedHashMap<String, String>();
            for (String[] row : (String[][]) value) {
                if (row != null && row.length >= 2 && row[0] != null) result.put(row[0], row[1]);
            }
            return result;
        } catch (IllegalAccessException e) {
            return failed();
        } catch (InvocationTargetException e) {
            return failed();
        } catch (RuntimeException e) {
            return failed();
        } catch (LinkageError e) {
            return failed();
        }
    }

    private Map<String, String> failed() {
        failures++;
        status = "request failed; source metadata preserved";
        return Collections.emptyMap();
    }

    private static boolean needsMetadata(Show show, List<String> categories) {
        return show.descriptions.isEmpty() || isEmpty(show.episodeName) || isEmpty(show.year)
                || isEmpty(show.language) || show.countries.isEmpty() || categories.isEmpty();
    }

    private static int positiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value == null ? "" : value.trim());
            return parsed > 0 && parsed <= 10000 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseYear(String value) {
        try { return Integer.parseInt(value == null ? "" : value.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static Date parseDate(String value) {
        if (isEmpty(value)) return null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try { return format.parse(value); }
        catch (ParseException e) { return null; }
    }

    private static void addSeparated(java.util.Collection<String> target, String value) {
        if (isEmpty(value)) return;
        for (String item : value.split(String.valueOf('\u001f'), -1)) addIfPresent(target, item);
    }

    private static void addIfPresent(java.util.Collection<String> target, String value) {
        if (!isEmpty(value)) target.add(value.trim());
    }

    private static String firstNonEmpty(String first, String second) {
        return isEmpty(first) ? second : first;
    }

    private static String emptyToNull(String value) { return isEmpty(value) ? null : value.trim(); }
    private static boolean isEmpty(String value) { return value == null || value.trim().length() == 0; }
    private static String safe(String value) { return value == null ? "" : value; }
}

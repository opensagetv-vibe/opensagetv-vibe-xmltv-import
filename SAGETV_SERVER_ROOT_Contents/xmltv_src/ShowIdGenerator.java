/*
 * Copyright 2026 The XMLTVImportPlugin for SageTV Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package xmltv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.zip.CRC32;

/**
 * Generates SageTV external show IDs independently from XML parsing and guide
 * database writes. The legacy strategy is byte-for-byte compatible with the
 * historical importer. The opt-in v2 strategy uses provider-scoped SHA-256
 * identities and persists assigned show/series IDs for stable collision
 * resolution.
 */
final class ShowIdGenerator {
    static final String LEGACY = "legacy";
    static final String V2 = "v2";
    static final String DEFAULT_V2_MAP_FILE = "xmltv-show-id-v2.properties";

    private static final String MAP_FORMAT = "v2-sha256-numeric-v1";
    private static final char[] LEGACY_SHOW_ID_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz@#".toCharArray();

    static final class Result {
        private final String externalId;
        private final Integer seriesId;

        Result(String aExternalId, Integer aSeriesId) {
            this.externalId = aExternalId;
            this.seriesId = aSeriesId;
        }

        String getExternalId() {
            return this.externalId;
        }

        Integer getSeriesId() {
            return this.seriesId;
        }
    }

    private final String strategy;
    private final String providerNamespace;
    private final File mappingFile;
    private final long splitMovieDetectTime;
    private final Properties mappings = new Properties();
    private final Map<String, String> showIdOwners = new HashMap<String, String>();
    private final Map<Integer, String> seriesIdOwners = new HashMap<Integer, String>();
    private boolean dirty;

    ShowIdGenerator(String aStrategy, String aProviderNamespace,
            File aMappingFile, long aSplitMovieDetectTime) throws IOException {
        String normalizedStrategy = aStrategy == null
                ? LEGACY : aStrategy.trim().toLowerCase(Locale.ROOT);
        if (!LEGACY.equals(normalizedStrategy) && !V2.equals(normalizedStrategy)) {
            throw new IllegalArgumentException("Unsupported XMLTV show ID strategy: "
                    + aStrategy + " (expected legacy or v2)");
        }
        this.strategy = normalizedStrategy;
        this.providerNamespace = normalize(aProviderNamespace);
        this.mappingFile = aMappingFile;
        this.splitMovieDetectTime = aSplitMovieDetectTime;
        if (V2.equals(this.strategy)) {
            if (this.mappingFile == null) {
                throw new IllegalArgumentException("v2 show IDs require a mapping file");
            }
            loadMappings();
        }
    }

    String getStrategy() {
        return this.strategy;
    }

    Result generate(Show aShow, String aCategory, Channel aChannel) {
        if (aShow == null) {
            throw new IllegalArgumentException("show is required");
        }
        if (aShow.showId != null && aShow.showId.length() > 0) {
            return new Result(aShow.showId, seriesIdFromExternalId(aShow.showId));
        }
        if (LEGACY.equals(this.strategy)) {
            String externalId = generateLegacy(aShow, aCategory);
            externalId = applySplitMovieSuffix(externalId, aShow, aCategory, aChannel);
            return new Result(externalId, seriesIdFromExternalId(externalId));
        }

        String seriesDigest = sha256Hex(canonicalSeriesIdentity(aShow, aCategory));
        Integer seriesId = getOrCreateSeriesId(seriesDigest);
        String showCanonical = canonicalShowIdentity(aShow, aCategory);
        String showDigest = sha256Hex(showCanonical);
        String prefix = aShow.season > 0 || aShow.episode > 0 ? "EP" : "SH";
        String externalId = getOrCreateShowId(showDigest, prefix,
                v2EpisodeSuffix(aShow), seriesId);
        externalId = applySplitMovieSuffix(externalId, aShow, aCategory, aChannel);
        return new Result(externalId, seriesId);
    }

    synchronized void flush() throws IOException {
        if (!V2.equals(this.strategy) || !this.dirty) {
            return;
        }
        Path target = this.mappingFile.toPath().toAbsolutePath();
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = Files.createTempFile(parent, this.mappingFile.getName(), ".tmp");
        try {
            this.mappings.setProperty("_format", MAP_FORMAT);
            this.mappings.setProperty("_scope", "provider-scoped-identities");
            try (FileOutputStream output = new FileOutputStream(temporary.toFile())) {
                this.mappings.store(output,
                        "OpenSageTV XMLTV v2 Show-ID mappings; do not edit while SageTV is running");
                output.getFD().sync();
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            this.dirty = false;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void loadMappings() throws IOException {
        if (!this.mappingFile.isFile()) {
            return;
        }
        try (FileInputStream input = new FileInputStream(this.mappingFile)) {
            try {
                this.mappings.load(input);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid v2 Show-ID mapping file: "
                        + this.mappingFile, e);
            }
        }
        String format = this.mappings.getProperty("_format");
        if (format != null && !MAP_FORMAT.equals(format)) {
            throw new IOException("Unsupported v2 Show-ID mapping format: " + format);
        }
        for (String key : this.mappings.stringPropertyNames()) {
            String value = this.mappings.getProperty(key);
            if (key.startsWith("show.")) {
                String digest = key.substring("show.".length());
                validateDigest(digest, key);
                if (!value.matches("(?:EP|SH)[0-9]{5,14}(?:-[0-9]+)?")) {
                    throw new IOException("Invalid mapped Show ID for " + key);
                }
                String owner = this.showIdOwners.put(value, digest);
                if (owner != null && !owner.equals(digest)) {
                    throw new IOException("Duplicate mapped Show ID " + value);
                }
            } else if (key.startsWith("series.")) {
                String digest = key.substring("series.".length());
                validateDigest(digest, key);
                try {
                    Integer seriesId = Integer.valueOf(value);
                    if (seriesId.intValue() <= 0) {
                        throw new NumberFormatException("not positive");
                    }
                    String owner = this.seriesIdOwners.put(seriesId, digest);
                    if (owner != null && !owner.equals(digest)) {
                        throw new IOException("Duplicate mapped Series ID " + value);
                    }
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid mapped Series ID for " + key, e);
                }
            }
        }
    }

    private synchronized String getOrCreateShowId(String aDigest,
            String aPrefix, String aPreferredSuffix, Integer aSeriesId) {
        String key = "show." + aDigest;
        String mapped = this.mappings.getProperty(key);
        if (mapped != null) {
            return mapped;
        }
        String base = aPrefix + aSeriesId.toString();
        String suffix = aPreferredSuffix;
        String candidate = suffix == null ? null : base + suffix;
        String owner = candidate == null ? null : this.showIdOwners.get(candidate);
        if (candidate == null || (owner != null && !owner.equals(aDigest))) {
            byte[] bytes = hexToBytes(aDigest);
            int token = (((bytes[4] & 0xff) << 8) | (bytes[5] & 0xff)) % 10000;
            int attempts = 0;
            do {
                suffix = String.format(Locale.ROOT, "%04d", Integer.valueOf(token));
                candidate = base + suffix;
                owner = this.showIdOwners.get(candidate);
                token = (token + 1) % 10000;
                attempts++;
            } while (owner != null && !owner.equals(aDigest) && attempts < 10000);
            if (owner != null && !owner.equals(aDigest)) {
                throw new IllegalStateException("No available v2 episode token for series "
                        + aSeriesId);
            }
        }
        this.mappings.setProperty(key, candidate);
        this.showIdOwners.put(candidate, aDigest);
        this.dirty = true;
        return candidate;
    }

    private synchronized Integer getOrCreateSeriesId(String aDigest) {
        String key = "series." + aDigest;
        String mapped = this.mappings.getProperty(key);
        if (mapped != null) {
            return Integer.valueOf(mapped);
        }
        byte[] bytes = hexToBytes(aDigest);
        int candidate = ((bytes[0] & 0x7f) << 24)
                | ((bytes[1] & 0xff) << 16)
                | ((bytes[2] & 0xff) << 8)
                | (bytes[3] & 0xff);
        if (candidate == 0) {
            candidate = 1;
        }
        while (this.seriesIdOwners.containsKey(Integer.valueOf(candidate))
                && !aDigest.equals(this.seriesIdOwners.get(Integer.valueOf(candidate)))) {
            candidate = candidate == Integer.MAX_VALUE ? 1 : candidate + 1;
        }
        Integer result = Integer.valueOf(candidate);
        this.mappings.setProperty(key, result.toString());
        this.seriesIdOwners.put(result, aDigest);
        this.dirty = true;
        return result;
    }

    private String generateLegacy(Show aShow, String aCategory) {
        String episodeSuffix;
        String prefix;
        DecimalFormat twoDigits = new DecimalFormat("00");
        if (aShow.season > 0 || aShow.episode > 0) {
            episodeSuffix = aShow.season > 0
                    ? twoDigits.format(aShow.season - 1) : "00";
            episodeSuffix += aShow.episode > 0
                    ? twoDigits.format(aShow.episode) : "00";
            prefix = "EP";
        } else if (aShow.freeFormEpisodeNumber != null) {
            episodeSuffix = aShow.freeFormEpisodeNumber;
            for (int i = aShow.freeFormEpisodeNumber.length(); i < 4; ++i) {
                episodeSuffix += ' ';
            }
            prefix = "SH";
        } else {
            episodeSuffix = "0000";
            prefix = "SH";
        }
        if (aShow.part > 0) {
            episodeSuffix += "-" + aShow.part;
        }

        CRC32 crc32 = new CRC32();
        updateLegacyChecksum(crc32, aShow.seriesId);
        updateLegacyChecksum(crc32, aShow.title);
        boolean uidGenerated = !"0000".equals(episodeSuffix);
        if (aShow.episodeName != null) {
            updateLegacyChecksum(crc32, aShow.episodeName);
            uidGenerated = true;
        } else if ("Movie".equals(aCategory)) {
            String director = aShow.getDirector();
            if (director != null) {
                updateLegacyChecksum(crc32, director);
                uidGenerated = true;
            } else if (aShow.year != null) {
                updateLegacyChecksum(crc32, aShow.year);
                uidGenerated = true;
            } else {
                List<String> actors = aShow.getLeadActors();
                if (!actors.isEmpty()) {
                    updateLegacyChecksum(crc32, String.valueOf(actors.get(0)));
                    uidGenerated = true;
                    if (actors.size() > 1) {
                        updateLegacyChecksum(crc32, String.valueOf(actors.get(1)));
                    }
                }
            }
        }
        if (!uidGenerated) {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss Z", Locale.ROOT);
            updateLegacyChecksum(crc32, format.format(aShow.start));
        }
        return prefix + legacyChecksumToShowId(crc32.getValue()) + episodeSuffix;
    }

    private String canonicalShowIdentity(Show aShow, String aCategory) {
        StringBuilder identity = new StringBuilder();
        append(identity, "format", "v2");
        append(identity, "provider", this.providerNamespace);
        append(identity, "kind", "Movie".equals(aCategory) ? "movie" : "programme");
        if (aShow.seriesId != null && aShow.seriesId.trim().length() > 0) {
            append(identity, "series", aShow.seriesId);
        } else {
            append(identity, "title", aShow.title);
        }

        boolean unique = aShow.season > 0 || aShow.episode > 0
                || aShow.freeFormEpisodeNumber != null || aShow.episodeName != null;
        if (aShow.season > 0 || aShow.episode > 0) {
            append(identity, "season", Integer.toString(aShow.season));
            append(identity, "episode", Integer.toString(aShow.episode));
        } else if (aShow.freeFormEpisodeNumber != null) {
            append(identity, "freeform", aShow.freeFormEpisodeNumber);
        } else if (aShow.episodeName != null) {
            append(identity, "episode_name", aShow.episodeName);
        }
        append(identity, "part", Integer.toString(aShow.part));
        if ("Movie".equals(aCategory)) {
            append(identity, "title", aShow.title);
            append(identity, "year", aShow.year);
            String director = aShow.getDirector();
            append(identity, "director", director);
            List<String> actors = aShow.getLeadActors();
            if (!actors.isEmpty()) {
                append(identity, "actor_1", String.valueOf(actors.get(0)));
                if (actors.size() > 1) {
                    append(identity, "actor_2", String.valueOf(actors.get(1)));
                }
            }
            unique = unique || director != null || aShow.year != null || !actors.isEmpty();
        }
        if (!unique) {
            append(identity, "start", formatUtc(aShow.start));
        }
        return identity.toString();
    }

    private String canonicalSeriesIdentity(Show aShow, String aCategory) {
        StringBuilder identity = new StringBuilder();
        append(identity, "format", "v2-series");
        append(identity, "provider", this.providerNamespace);
        append(identity, "kind", "Movie".equals(aCategory) ? "movie" : "programme");
        if (aShow.seriesId != null && aShow.seriesId.trim().length() > 0) {
            append(identity, "series", aShow.seriesId);
        } else {
            append(identity, "title", aShow.title);
        }
        if ("Movie".equals(aCategory) && aShow.seriesId == null) {
            append(identity, "year", aShow.year);
        }
        return identity.toString();
    }

    private String applySplitMovieSuffix(String aExternalId, Show aShow,
            String aCategory, Channel aChannel) {
        if (this.splitMovieDetectTime <= 0 || !"Movie".equals(aCategory)
                || aChannel == null) {
            return aExternalId;
        }
        String baseId = aExternalId;
        String showId = baseId;
        int count = 0;
        while (aChannel.movieIds.containsKey(showId)
                && aShow.start.getTime()
                        - ((Date) aChannel.movieIds.get(showId)).getTime()
                        < this.splitMovieDetectTime) {
            showId = baseId + "-" + ++count;
        }
        aChannel.movieIds.put(showId, aShow.start);
        return showId;
    }

    private static Integer seriesIdFromExternalId(String aExternalId) {
        String numeric = aExternalId.replaceAll("[^0-9]", "")
                .replaceAll("^0+(?!$)", "");
        if (numeric.length() <= 4) {
            return null;
        }
        try {
            return Integer.valueOf(numeric.substring(0, numeric.length() - 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String v2EpisodeSuffix(Show aShow) {
        if (aShow.season <= 0 && aShow.episode <= 0) {
            return null;
        }
        if (aShow.season > 100 || aShow.episode > 99) {
            return null;
        }
        DecimalFormat twoDigits = new DecimalFormat("00");
        String season = aShow.season > 0 ? twoDigits.format(aShow.season - 1) : "00";
        String episode = aShow.episode > 0 ? twoDigits.format(aShow.episode) : "00";
        return season + episode;
    }

    private static void append(StringBuilder aBuilder, String aName, String aValue) {
        String value = normalize(aValue);
        aBuilder.append(aName.length()).append(':').append(aName)
                .append('=').append(value.length()).append(':').append(value).append(';');
    }

    private static String normalize(String aValue) {
        if (aValue == null) {
            return "";
        }
        return Normalizer.normalize(aValue, Normalizer.Form.NFKC).trim()
                .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String formatUtc(Date aDate) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(aDate);
    }

    private static void updateLegacyChecksum(CRC32 aChecksum, String aValue) {
        if (aValue != null) {
            aChecksum.update(aValue.toLowerCase().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String legacyChecksumToShowId(long aChecksum) {
        char[] chars = new char[6];
        for (int i = 0; i < chars.length; ++i) {
            chars[i] = LEGACY_SHOW_ID_CHARS[(int) ((aChecksum
                    >> ((5 - i) * 6)) & 0x3fL)];
        }
        return new String(chars);
    }

    private static String sha256Hex(String aValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(aValue.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String bytesToHex(byte[] aBytes) {
        StringBuilder result = new StringBuilder(aBytes.length * 2);
        for (byte value : aBytes) {
            result.append(Character.forDigit((value >>> 4) & 0x0f, 16));
            result.append(Character.forDigit(value & 0x0f, 16));
        }
        return result.toString().toUpperCase(Locale.ROOT);
    }

    private static byte[] hexToBytes(String aHex) {
        byte[] result = new byte[aHex.length() / 2];
        for (int i = 0; i < result.length; ++i) {
            int high = Character.digit(aHex.charAt(i * 2), 16);
            int low = Character.digit(aHex.charAt(i * 2 + 1), 16);
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }

    private static void validateDigest(String aDigest, String aKey) throws IOException {
        if (!aDigest.matches("[0-9A-Fa-f]{64}")) {
            throw new IOException("Invalid digest in v2 Show-ID mapping: " + aKey);
        }
    }
}

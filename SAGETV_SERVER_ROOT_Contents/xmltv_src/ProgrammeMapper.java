package xmltv;

import java.util.Locale;
import sage.EPGDBPublic2;

/** Pure conversion of XMLTV programme flags/numbers to SageTV airing fields. */
final class ProgrammeMapper {
    static final class AiringFields {
        final short season;
        final short episode;
        final byte multipart;
        final int miscellaneous;

        AiringFields(short season, short episode, byte multipart, int miscellaneous) {
            this.season = season;
            this.episode = episode;
            this.multipart = multipart;
            this.miscellaneous = miscellaneous;
        }
    }

    private ProgrammeMapper() {
    }

    static AiringFields map(Show show) {
        return new AiringFields(safeShort(show.season), safeShort(show.episode),
                multipart(show.part, show.parts), miscellaneous(show));
    }

    private static short safeShort(int value) {
        return value < 0 || value > Short.MAX_VALUE ? 0 : (short) value;
    }

    private static byte multipart(int part, int total) {
        if (part < 0 || part > 15 || total < 0 || total > 15) return 0;
        return (byte) ((part << 4) | total);
    }

    private static int miscellaneous(Show show) {
        int result = 0;
        if (show.is_live) result |= EPGDBPublic2.LIVE_MASK;
        if (show.is_new) result |= EPGDBPublic2.NEW_MASK;
        String audio = lower(show.audio);
        if (audio.contains("stereo")) result |= EPGDBPublic2.STEREO_MASK;
        if (audio.contains("sap")) result |= EPGDBPublic2.SAP_MASK;
        if (audio.contains("dd 5.1") || audio.contains("dolby digital 5.1")) {
            result |= EPGDBPublic2.DD51_MASK;
        } else if (audio.contains("dd") || audio.contains("dolby digital")) {
            result |= EPGDBPublic2.DOLBY_MASK;
        }
        if (audio.contains("dubbed")) result |= EPGDBPublic2.DUBBED_MASK;
        if (audio.contains("surround")) result |= EPGDBPublic2.SURROUND_MASK;
        if (audio.contains("subtitled")) result |= EPGDBPublic2.SUBTITLE_MASK;
		if (show.subtitles != null) result |= EPGDBPublic2.SUBTITLE_MASK;
        if (audio.contains("cc")) result |= EPGDBPublic2.CC_MASK;
        if (audio.contains("tape")) result |= EPGDBPublic2.TAPE_MASK;

        String quality = lower(show.quality);
        if (quality.contains("hdtv")) result |= EPGDBPublic2.HDTV_MASK;
        if (quality.contains("3d")) result |= EPGDBPublic2.THREED_MASK;
        if (quality.contains("letterbox")) result |= EPGDBPublic2.LETTERBOX_MASK;
        if (quality.contains("ws") || quality.contains("widescreen")) {
            result |= EPGDBPublic2.WIDESCREEN_MASK;
        }

        if (show.is_premiere) {
            String premiere = lower(show.premiere);
            result |= EPGDBPublic2.PREMIERE_MASK;
            if (premiere.contains("channel")) result |= EPGDBPublic2.CHANNEL_PREMIERE_MASK;
            if (premiere.contains("finale")) {
                if (premiere.contains("season")) result |= EPGDBPublic2.SEASON_FINALE_MASK;
                if (premiere.contains("series")) result |= EPGDBPublic2.SERIES_FINALE_MASK;
            } else {
                if (premiere.contains("season")) result |= EPGDBPublic2.SEASON_PREMIERE_MASK;
                if (premiere.contains("series")) result |= EPGDBPublic2.SERIES_PREMIERE_MASK;
            }
        }
        return result;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}

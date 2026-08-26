package xmltv;

import java.util.Map;
import sage.EPGDBPublic2;

/** Narrow adapter around SageTV's public EPG database API. */
final class SageGuideWriter {
    private final EPGDBPublic2 guide;

    SageGuideWriter(EPGDBPublic2 guide) {
        if (guide == null) throw new IllegalArgumentException("guide database is required");
        this.guide = guide;
    }

    boolean addChannel(String name, String longName, String network, int stationId) {
        return this.guide.addChannelPublic(name, longName, network, stationId);
    }

    boolean addShow(String title, String episodeName, String description, long duration,
            String[] categories,
            String[] people, byte[] roles, String rating, String[] expandedRatings, String year,
            String parentalRating, String[] bonus, String externalId, String language,
            long originalAirDate, short season, short episode) {
        return this.guide.addShowPublic2(title, episodeName, description, duration, categories,
                people, roles, rating, expandedRatings, year, parentalRating, bonus,
                externalId, language, originalAirDate, season, episode, false);
    }

    boolean addSeries(int seriesId, String title, String imageUrl, String[] people,
            String[] characters) {
        return this.guide.addSeriesInfoPublic(seriesId, title, "", "", "", "", "", "",
                "", imageUrl, people, characters);
    }

    boolean addAiring(String externalId, int stationId, long start, long duration,
            byte multipart, int miscellaneous, String parentalRating) {
        return this.guide.addAiringPublic2(externalId, stationId, start, duration,
                multipart, miscellaneous, parentalRating);
    }

    void setLineup(long providerId, Map<Integer, String[]> lineup) {
        this.guide.setLineup(providerId, lineup);
    }
}

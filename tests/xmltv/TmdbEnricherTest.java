package xmltv;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.opensagetv.vibe.tmdb.plugin.OpenSageTVVibeTmdbFacade;

public final class TmdbEnricherTest {
    private TmdbEnricherTest() {}

    public static void main(String[] args) {
        disabledIsNoOp();
        missingFieldsAreFilledAndDeduplicated();
        feedMetadataAlwaysWins();
        failureIsNonFatal();
        unavailableIsNonFatal();
        System.out.println("[PASS] opt-in TMDB enrichment preserves XMLTV metadata and failures");
    }

    private static void disabledIsNoOp() {
        OpenSageTVVibeTmdbFacade.reset();
        Show show = show();
        TmdbEnricher.create(new Properties()).enrichMissing(show,
                new LinkedList<String>(), "Series");
        require(OpenSageTVVibeTmdbFacade.calls == 0, "disabled lookup");
        require(show.descriptions.isEmpty(), "disabled mutation");
    }

    private static void missingFieldsAreFilledAndDeduplicated() {
        OpenSageTVVibeTmdbFacade.reset();
        Properties properties = enabled();
        TmdbEnricher enricher = TmdbEnricher.create(properties);
        Show first = show();
        first.season = 1;
        first.episode = 2;
        List<String> categories = new LinkedList<String>();
        enricher.enrichMissing(first, categories, "Series");
        Show second = show();
        second.season = 1;
        second.episode = 2;
        enricher.enrichMissing(second, new LinkedList<String>(), "Series");
        require(OpenSageTVVibeTmdbFacade.calls == 1, "lookup deduplication");
        require("Episode description".equals(first.descriptions.get(0)), "episode description");
        require("Episode from TMDB".equals(first.episodeName), "episode name");
        require("2024".equals(first.year) && "en".equals(first.language), "scalar metadata");
        require(categories.size() == 2 && first.countries.size() == 2, "list metadata");
        require(first.date != null, "episode air date");
    }

    private static void feedMetadataAlwaysWins() {
        OpenSageTVVibeTmdbFacade.reset();
        Show show = show();
        show.descriptions.add("Feed description");
        show.episodeName = "Feed episode";
        show.year = "1999";
        show.language = "fr";
        show.date = new Date(1L);
        show.countries.add("France");
        List<String> categories = new LinkedList<String>();
        categories.add("News");
        TmdbEnricher.create(enabled()).enrichMissing(show, categories, "News");
        require("Feed description".equals(show.descriptions.get(0)), "description overwrite");
        require("Feed episode".equals(show.episodeName), "episode overwrite");
        require("1999".equals(show.year) && "fr".equals(show.language), "scalar overwrite");
        require(show.date.getTime() == 1L, "date overwrite");
        require(categories.size() == 1 && show.countries.size() == 1, "list overwrite");
        require(OpenSageTVVibeTmdbFacade.calls == 0, "complete feed should not call TMDB");
    }

    private static void failureIsNonFatal() {
        OpenSageTVVibeTmdbFacade.reset();
        OpenSageTVVibeTmdbFacade.fail = true;
        Show show = show();
        TmdbEnricher enricher = TmdbEnricher.create(enabled());
        enricher.enrichMissing(show, new LinkedList<String>(), "Series");
        require(show.descriptions.isEmpty(), "failure mutated source");
        require(enricher.summary().contains("failures=1"), "failure telemetry");
    }

    private static void unavailableIsNonFatal() {
        OpenSageTVVibeTmdbFacade.reset();
        OpenSageTVVibeTmdbFacade.available = false;
        Show show = show();
        TmdbEnricher enricher = TmdbEnricher.create(enabled());
        enricher.enrichMissing(show, new LinkedList<String>(), "Series");
        require(OpenSageTVVibeTmdbFacade.calls == 0, "unavailable lookup");
        require(enricher.summary().contains("source metadata preserved"), "unavailable status");
    }

    private static Properties enabled() {
        Properties properties = new Properties();
        properties.setProperty("xmltv.tmdb.enrich", "true");
        properties.setProperty("xmltv.tmdb.max_lookups_per_import", "10");
        return properties;
    }

    private static Show show() {
        Show show = new Show(new Date(1000L), new Date(2000L));
        show.title = "Example Show";
        return show;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}

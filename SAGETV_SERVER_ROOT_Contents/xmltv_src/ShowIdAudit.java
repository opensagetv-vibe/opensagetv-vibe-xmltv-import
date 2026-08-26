package xmltv;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import sage.EPGDBPublic;

/** Dry-run CLI that compares legacy and v2 IDs without touching SageTV. */
public final class ShowIdAudit {
    private static final Pattern XMLTV_NS = Pattern.compile(
            "\\s*([0-9]+)?\\s*(?:/([0-9]+)?)?\\s*\\.\\s*"
            + "([0-9]+)?\\s*(?:/([0-9]+)?)?\\s*\\.\\s*"
            + "([0-9]+)?\\s*(?:/([0-9]+)?)?\\s*");

    private ShowIdAudit() {
    }

    public static void main(String[] args) throws Exception {
        Arguments options = Arguments.parse(args);
        File mapping = options.mappingFile == null
                ? new File(ShowIdGenerator.DEFAULT_V2_MAP_FILE) : options.mappingFile;
        String namespace = options.providerId == null
                ? "name:" + options.providerName : "id:" + options.providerId;
        ShowIdGenerator legacy = new ShowIdGenerator(ShowIdGenerator.LEGACY,
                namespace, null, 14400000L);
        ShowIdGenerator v2 = new ShowIdGenerator(ShowIdGenerator.V2,
                namespace, mapping, 14400000L);
        AuditHandler handler = new AuditHandler(legacy, v2);

        System.out.println("file\tchannel\tstart\ttitle\tidentity_source\tlegacy_id\tv2_id\tseries_id");
        for (File input : options.inputs) {
            if (!input.isFile()) throw new IOException("XMLTV file not found: " + input);
            handler.file = input.getName();
            XmltvParser.parse(input, false, true, handler, handler);
        }
        if (options.writeMap) v2.flush();
        System.err.println("programmes=" + handler.programmes
                + " explicit=" + handler.explicit
                + " legacy_collisions=" + handler.legacyCollisions
                + " v2_collisions=" + handler.v2Collisions
                + " map_written=" + options.writeMap);
        if (handler.v2Collisions > 0) System.exit(2);
    }

    private static final class AuditHandler extends DefaultHandler {
        private final ShowIdGenerator legacy;
        private final ShowIdGenerator v2;
        private final Map<String, Channel> channels = new HashMap<String, Channel>();
        private final Map<String, String> legacyOwners = new HashMap<String, String>();
        private final Map<String, String> v2Owners = new HashMap<String, String>();
        private Show show;
        private Channel channel;
        private String channelId;
        private String system;
        private StringBuilder text;
        private String file;
        private long programmes;
        private long explicit;
        private long legacyCollisions;
        private long v2Collisions;

        AuditHandler(ShowIdGenerator legacy, ShowIdGenerator v2) {
            this.legacy = legacy;
            this.v2 = v2;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            this.text = new StringBuilder();
            if ("programme".equals(qName)) {
                try {
                    this.show = new Show(XmltvDateParser.parseTimestamp(attributes.getValue("start")),
                            XmltvDateParser.parseTimestamp(attributes.getValue("stop")));
                } catch (IllegalArgumentException e) {
                    throw new SAXException(e);
                }
                this.channelId = attributes.getValue("channel");
                this.channel = this.channels.get(this.channelId);
                if (this.channel == null) {
                    this.channel = new Channel(this.channelId == null ? "unknown" : this.channelId);
                    this.channels.put(this.channel.xmltvId, this.channel);
                }
            } else if ("episode-num".equals(qName) || "series-id".equals(qName)) {
                this.system = attributes.getValue("system");
            }
        }

        public void characters(char[] characters, int start, int length) {
            if (this.text != null) this.text.append(characters, start, length);
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            String value = this.text == null ? "" : this.text.toString().trim();
            if (this.show != null) {
                if ("title".equals(qName)) this.show.title = value;
                else if ("sub-title".equals(qName)) this.show.episodeName = value;
                else if ("desc".equals(qName)) this.show.descriptions.add(value);
                else if ("category".equals(qName)) {
                    for (String item : value.split("/")) this.show.categories.add(item.trim());
                } else if ("date".equals(qName)) {
                    if (value.length() == 4) this.show.year = value;
                    else if (value.length() >= 8) {
                        try {
                            this.show.date = XmltvDateParser.parseDay(value);
                            this.show.year = XmltvDateParser.year(this.show.date);
                        } catch (IllegalArgumentException e) {
                            throw new SAXException(e);
                        }
                    }
                } else if ("director".equals(qName)) {
                    addPerson(value, EPGDBPublic.DIRECTOR_ROLE);
                } else if ("actor".equals(qName)) {
                    addPerson(value, EPGDBPublic.ACTOR_ROLE);
                } else if ("series-id".equals(qName)) {
                    this.show.seriesId = value;
                } else if ("episode-num".equals(qName)) {
                    parseEpisode(value);
                } else if ("programme".equals(qName)) {
                    emit();
                    this.show = null;
                    this.channel = null;
                    this.channelId = null;
                }
            }
            this.text = null;
        }

        private void addPerson(String name, byte role) {
            if (name.length() == 0) return;
            this.show.people.add(name);
            this.show.characters.add(role == EPGDBPublic.DIRECTOR_ROLE ? "Director" : "Actor");
            this.show.roles.write(role);
        }

        private void parseEpisode(String value) {
            String normalized = this.system == null ? "" : this.system.toLowerCase(Locale.ROOT);
            if ("dd_progid".equals(normalized)) this.show.showId = value.replaceFirst("\\.", "");
            else if ("tms".equals(normalized) || "plutod".equals(normalized)) this.show.showId = value;
            else if ("series".equals(normalized)) this.show.seriesId = value;
            else if ("season".equals(normalized)) this.show.season = positiveInt(value);
            else if ("episode".equals(normalized)) this.show.episode = positiveInt(value);
            else if ("onscreen".equals(normalized) || "common".equals(normalized)) {
                this.show.freeFormEpisodeNumber = value;
            } else if ("xmltv_ns".equals(normalized)) {
                Matcher match = XMLTV_NS.matcher(value);
                if (match.matches()) {
                    this.show.season = plusOne(match.group(1));
                    this.show.seasons = positiveInt(match.group(2));
                    this.show.episode = plusOne(match.group(3));
                    this.show.episodes = positiveInt(match.group(4));
                    this.show.part = plusOne(match.group(5));
                    this.show.parts = positiveInt(match.group(6));
                }
            }
        }

        private void emit() {
            String category = this.show.categories.isEmpty()
                    ? null : this.show.categories.iterator().next();
            ShowIdGenerator.Result oldResult = this.legacy.generate(this.show, category, this.channel);
            ShowIdGenerator.Result newResult = this.v2.generate(this.show, category, this.channel);
            String identity = fingerprint(this.show);
            if (collision(this.legacyOwners, oldResult.getExternalId(), identity)) {
                this.legacyCollisions++;
            }
            if (collision(this.v2Owners, newResult.getExternalId(), identity)) {
                this.v2Collisions++;
            }
            String source = this.show.showId == null ? "generated" : "provider";
            if (this.show.showId != null) this.explicit++;
            this.programmes++;
            System.out.println(tsv(this.file) + '\t' + tsv(this.channelId) + '\t'
                    + this.show.start.getTime() + '\t' + tsv(this.show.title) + '\t' + source
                    + '\t' + oldResult.getExternalId() + '\t' + newResult.getExternalId()
                    + '\t' + (newResult.getSeriesId() == null ? "" : newResult.getSeriesId()));
        }

        private static boolean collision(Map<String, String> owners, String id, String identity) {
            String prior = owners.put(id, identity);
            return prior != null && !prior.equals(identity);
        }

        private static String fingerprint(Show show) {
            return String.valueOf(show.title) + '\u001f' + String.valueOf(show.episodeName)
                    + '\u001f' + show.season + '\u001f' + show.episode + '\u001f'
                    + String.valueOf(show.seriesId);
        }

        private static String tsv(String value) {
            return value == null ? "" : value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
        }

        private static int positiveInt(String value) {
            if (value == null) return 0;
            try {
                int result = Integer.parseInt(value.trim());
                return result < 0 ? 0 : result;
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private static int plusOne(String value) {
            int parsed = positiveInt(value);
            return value == null || value.length() == 0 ? 0 : parsed + 1;
        }
    }

    private static final class Arguments {
        String providerId;
        String providerName = "XMLTV Lineup";
        File mappingFile;
        boolean writeMap;
        final java.util.List<File> inputs = new java.util.ArrayList<File>();

        static Arguments parse(String[] args) {
            Arguments result = new Arguments();
            for (int i = 0; i < args.length; i++) {
                String argument = args[i];
                if ("--provider-id".equals(argument)) result.providerId = require(args, ++i, argument);
                else if ("--provider-name".equals(argument)) result.providerName = require(args, ++i, argument);
                else if ("--map".equals(argument)) result.mappingFile = new File(require(args, ++i, argument));
                else if ("--write-map".equals(argument)) result.writeMap = true;
                else if (argument.startsWith("--")) usage("unknown option: " + argument);
                else result.inputs.add(new File(argument));
            }
            if (result.inputs.isEmpty()) usage("at least one XMLTV file is required");
            return result;
        }

        private static String require(String[] args, int index, String option) {
            if (index >= args.length) usage("missing value for " + option);
            return args[index];
        }

        private static void usage(String error) {
            throw new IllegalArgumentException(error + "\nUsage: ShowIdAudit [--provider-id ID] "
                    + "[--provider-name NAME] [--map FILE] [--write-map] XMLTV_FILE...");
        }
    }
}

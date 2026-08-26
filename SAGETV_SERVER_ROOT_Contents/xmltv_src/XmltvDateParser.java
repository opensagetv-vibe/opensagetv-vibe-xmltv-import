package xmltv;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Date;
import java.util.Locale;

/** Strict, immutable XMLTV date parsers safe for concurrent imports. */
final class XmltvDateParser {
    private static final DateTimeFormatter SECONDS = new DateTimeFormatterBuilder()
            .parseCaseSensitive().appendPattern("uuuuMMddHHmmss xx").toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter MINUTES = new DateTimeFormatterBuilder()
            .parseCaseSensitive().appendPattern("uuuuMMddHHmm xx").toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DAY = new DateTimeFormatterBuilder()
            .parseCaseSensitive().appendPattern("uuuuMMdd").toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private XmltvDateParser() {
    }

    static Date parseTimestamp(String input) {
        if (input == null) return null;
        String value = input.trim().replaceFirst(" Z$", " +0000");
        try {
            DateTimeFormatter formatter;
            int separator = value.indexOf(' ');
            if (separator == 12) formatter = MINUTES;
            else if (separator == 14) formatter = SECONDS;
            else throw new IllegalArgumentException("Unknown XMLTV timestamp format: " + input);
            return Date.from(OffsetDateTime.parse(value, formatter).toInstant());
        } catch (java.time.DateTimeException e) {
            throw new IllegalArgumentException("Invalid XMLTV timestamp: " + input, e);
        }
    }

    static Date parseDay(String input) {
        if (input == null || input.length() < 8) return null;
        try {
            LocalDate day = LocalDate.parse(input.substring(0, 8), DAY);
            return Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (java.time.DateTimeException e) {
            throw new IllegalArgumentException("Invalid XMLTV date: " + input, e);
        }
    }

    static String year(Date value) {
        if (value == null) return null;
        return Integer.toString(value.toInstant().atZone(ZoneId.systemDefault()).getYear());
    }
}

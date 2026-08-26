package xmltv;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeMap;

/** Typed and redaction-aware view of one XMLTV provider configuration. */
final class XmltvConfiguration {
    private final Properties properties;

    XmltvConfiguration(Properties properties) {
        if (properties == null) throw new IllegalArgumentException("configuration is required");
        this.properties = properties;
    }

    String get(String name) {
        String value = this.properties.getProperty(name);
        return value == null ? null : value.trim();
    }

    String get(String name, String defaultValue) {
        String value = get(name);
        return value == null || value.length() == 0 ? defaultValue : value;
    }

    boolean getBoolean(String name, boolean defaultValue) {
        String value = get(name);
        if (value == null || value.length() == 0) return defaultValue;
        if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)
                || "1".equals(value)) return true;
        if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value)
                || "0".equals(value)) return false;
        throw new IllegalArgumentException(name + " must be true or false");
    }

    int getInt(String name, int defaultValue, int minimum, int maximum) {
        long value = getLong(name, defaultValue, minimum, maximum);
        return (int) value;
    }

    long getLong(String name, long defaultValue, long minimum, long maximum) {
        String text = get(name);
        long value;
        try {
            value = text == null || text.length() == 0 ? defaultValue : Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be an integer", e);
        }
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be between " + minimum
                    + " and " + maximum);
        }
        return value;
    }

    List<String> getList(String name) {
        String value = get(name);
        if (value == null || value.length() == 0) return Collections.emptyList();
        String[] parts = value.split(" *, *");
        List<String> result = new ArrayList<String>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.length() > 0) result.add(trimmed);
        }
        return Collections.unmodifiableList(result);
    }

    Properties asProperties() {
        return this.properties;
    }

    String redactedDump(String newline) {
        TreeMap<String, String> sorted = new TreeMap<String, String>();
        Enumeration<?> names = this.properties.propertyNames();
        while (names.hasMoreElements()) {
            String name = String.valueOf(names.nextElement());
            sorted.put(name, redact(name, this.properties.getProperty(name)));
        }
        StringBuilder result = new StringBuilder();
        for (java.util.Map.Entry<String, String> entry : sorted.entrySet()) {
            result.append(newline).append(entry.getKey()).append('=').append(entry.getValue());
        }
        return result.toString();
    }

    private static String redact(String name, String value) {
        if (value == null) return "";
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("password") || lower.contains("secret")
                || lower.contains("token") || lower.contains("authorization")
                || lower.contains("license") || lower.endsWith(".key")) {
            return "<redacted>";
        }
        if ("xmltv.files".equals(lower)) {
            String[] sources = value.split(" *, *");
            StringBuilder safe = new StringBuilder();
            for (String source : sources) {
                if (safe.length() > 0) safe.append(", ");
                safe.append(redactUri(source));
            }
            return safe.toString();
        }
        return value;
    }

    static String redactUri(String source) {
        if (source == null || source.indexOf("://") < 0) return source;
        try {
            URI uri = new URI(source);
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    uri.getPath(), null, null).toString();
        } catch (URISyntaxException e) {
            return "<invalid URL>";
        }
    }
}

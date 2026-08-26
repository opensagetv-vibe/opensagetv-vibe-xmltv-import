package xmltv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Resolves backwards-compatible XMLTV configuration profiles. */
final class ConfigurationProfiles {
    static final String PROFILE_PROPERTY = "xmltv.profile";
    private static final String AUTO = "auto";

    interface LogSink {
        void log(String message);
    }

    private ConfigurationProfiles() {
    }

    static Properties load(File configurationFile, Properties defaults, LogSink log)
            throws IOException {
        File source = configurationFile.getCanonicalFile();
        Properties sourceProperties = read(source);
        String profileName = trim(sourceProperties.getProperty(PROFILE_PROPERTY));

        if (AUTO.equalsIgnoreCase(profileName)) {
            profileName = createAutomaticProfile(source, sourceProperties, defaults, log);
            sourceProperties.setProperty(PROFILE_PROPERTY, profileName);
        }

        List<File> layers = new ArrayList<File>();
        Map<File, Properties> propertiesByFile = new HashMap<File, Properties>();
        layers.add(source);
        propertiesByFile.put(source, sourceProperties);
        collectIncludes(source, sourceProperties, layers, propertiesByFile, log);

        if (profileName != null) {
            File profile = findProfileFile(source.getParentFile(), profileName);
            if (profile.isFile()) {
                profile = profile.getCanonicalFile();
                if (!layers.contains(profile)) {
                    Properties profileProperties = read(profile);
                    // Insert the profile immediately below the source file. Its
                    // includes are appended below the pre-existing include
                    // chain, preserving exact legacy ordering when no profile
                    // is selected.
                    layers.add(1, profile);
                    propertiesByFile.put(profile, profileProperties);
                    collectIncludes(profile, profileProperties, layers,
                            propertiesByFile, log);
                }
            } else {
                log.log("XMLTV profile not found; continuing with existing configuration: "
                        + profile.getAbsolutePath());
            }
        }

        Properties resolved = new Properties(defaults);
        for (int i = layers.size() - 1; i >= 0; i--) {
            Properties properties = propertiesByFile.get(layers.get(i));
            if (properties != null) copy(properties, resolved);
        }
        return resolved;
    }

    static File profileFile(File directory, String profileName) throws IOException {
        String safeName = safeProfileName(profileName);
        if (safeName.length() == 0) throw new IOException("XMLTV profile name is empty");
        return new File(directory, "xmltv_" + safeName + ".profile").getCanonicalFile();
    }

    private static File findProfileFile(File directory, String profileName) throws IOException {
        File exact = profileFile(directory, profileName);
        if (exact.isFile()) return exact;
        File[] candidates = directory.listFiles();
        if (candidates != null) {
            for (File candidate : candidates) {
                if (candidate.isFile() && candidate.getName().equalsIgnoreCase(exact.getName())) {
                    return candidate.getCanonicalFile();
                }
            }
        }
        return exact;
    }

    static String safeProfileName(String value) {
        String input = trim(value);
        if (input == null) return "";
        if (input.regionMatches(true, 0, "xmltv_", 0, 6)) input = input.substring(6);
        if (input.toLowerCase(java.util.Locale.ROOT).endsWith(".profile")) {
            input = input.substring(0, input.length() - 8);
        }
        StringBuilder result = new StringBuilder();
        boolean separator = false;
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            if (Character.isLetterOrDigit(character) || character == '-' || character == '_') {
                result.append(character);
                separator = false;
            } else if (!separator && result.length() > 0) {
                result.append('_');
                separator = true;
            }
        }
        while (result.length() > 0 && result.charAt(result.length() - 1) == '_') {
            result.setLength(result.length() - 1);
        }
        return result.toString();
    }

    private static String createAutomaticProfile(File source, Properties sourceProperties,
            Properties defaults, LogSink log) throws IOException {
        String providerName = trim(sourceProperties.getProperty("provider.name"));
        if (providerName == null) providerName = trim(defaults.getProperty("provider.name"));
        if (providerName == null) providerName = "Generic";
        String generatedName = safeProfileName(providerName);
        if (generatedName.length() == 0) generatedName = "Generic";
        File profile = profileFile(source.getParentFile(), generatedName);

        if (!profile.exists()) {
            writeGeneratedProfile(profile, providerName, sourceProperties);
            log.log("Created XMLTV profile " + profile.getName()
                    + " from explicit settings in " + source.getName());
        } else {
            log.log("Using existing auto-selected XMLTV profile " + profile.getName());
        }

        upsertProfileSelection(source, generatedName);
        log.log("Set " + PROFILE_PROPERTY + "=" + generatedName + " in " + source.getName());
        return generatedName;
    }

    private static void writeGeneratedProfile(File profile, String providerName,
            Properties sourceProperties) throws IOException {
        File temporary = temporarySibling(profile);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(temporary), StandardCharsets.ISO_8859_1))) {
            writer.write("# Automatically generated XMLTV profile for "
                    + safeProfileName(providerName));
            writer.newLine();
            writer.write("# Explicit values in the selecting .xmltv.properties file take priority.");
            writer.newLine();
            writer.write("include=common.properties");
            writer.newLine();

            List<String> names = new ArrayList<String>();
            for (String name : sourceProperties.stringPropertyNames()) {
                if (isProfileSetting(name)) names.add(name);
            }
            Collections.sort(names);
            for (String name : names) {
                writer.write(escape(name, true));
                writer.write('=');
                writer.write(escape(sourceProperties.getProperty(name), false));
                writer.newLine();
            }
        }
        replace(temporary, profile);
    }

    private static boolean isProfileSetting(String name) {
        return !PROFILE_PROPERTY.equals(name)
                && !"provider.name".equals(name)
                && !"provider.id".equals(name)
                && !"xmltv.files".equals(name)
                && !"configurations".equals(name)
                && !"include".equals(name)
                && !"run.before".equals(name)
                && !"timeslot".equals(name)
                && !name.startsWith("channel.");
    }

    private static void upsertProfileSelection(File source, String profileName)
            throws IOException {
        File temporary = temporarySibling(source);
        boolean found = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(source), StandardCharsets.ISO_8859_1));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(temporary), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("#") && !trimmed.startsWith("!")
                        && propertyName(trimmed).equals(PROFILE_PROPERTY)) {
                    if (!found) {
                        writer.write(PROFILE_PROPERTY + "=" + escape(profileName, false));
                        writer.newLine();
                        found = true;
                    }
                } else {
                    writer.write(line);
                    writer.newLine();
                }
            }
            if (!found) {
                writer.write(PROFILE_PROPERTY + "=" + escape(profileName, false));
                writer.newLine();
            }
        }
        replace(temporary, source);
    }

    private static String propertyName(String line) {
        int separator = -1;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '=' || character == ':' || Character.isWhitespace(character)) {
                separator = i;
                break;
            }
        }
        return separator < 0 ? line : line.substring(0, separator);
    }

    private static void collectFile(File file, List<File> layers,
            Map<File, Properties> propertiesByFile, LogSink log) throws IOException {
        File canonical = file.getCanonicalFile();
        if (layers.contains(canonical)) return;
        layers.add(canonical);
        if (!canonical.isFile()) {
            log.log("Skipping missing XMLTV include: " + canonical.getAbsolutePath());
            return;
        }
        Properties properties = read(canonical);
        propertiesByFile.put(canonical, properties);
        collectIncludes(canonical, properties, layers, propertiesByFile, log);
    }

    private static void collectIncludes(File owner, Properties properties, List<File> layers,
            Map<File, Properties> propertiesByFile, LogSink log) throws IOException {
        String[] includes = split(properties.getProperty("include"));
        for (int i = 0; i < includes.length; i++) {
            File include = new File(includes[i]);
            if (!include.isAbsolute()) include = new File(owner.getParentFile(), includes[i]);
            collectFile(include, layers, propertiesByFile, log);
        }
    }

    private static Properties read(File file) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        }
        return properties;
    }

    private static void copy(Properties source, Properties target) {
        for (String name : source.stringPropertyNames()) {
            target.setProperty(name, source.getProperty(name));
        }
    }

    private static String[] split(String value) {
        String trimmed = trim(value);
        return trimmed == null ? new String[0] : trimmed.split(" *, *");
    }

    private static String trim(String value) {
        if (value == null) return null;
        String result = value.trim();
        return result.length() == 0 ? null : result;
    }

    private static File temporarySibling(File destination) throws IOException {
        File directory = destination.getAbsoluteFile().getParentFile();
        if (directory == null || (!directory.isDirectory() && !directory.mkdirs())) {
            throw new IOException("Unable to create profile directory: " + directory);
        }
        return File.createTempFile(destination.getName() + ".", ".tmp", directory);
    }

    private static void replace(File source, File destination) throws IOException {
        try {
            Files.move(source.toPath(), destination.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source.toPath(), destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (source.exists()) source.delete();
        }
    }

    private static String escape(String value, boolean key) {
        if (value == null) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '\n') {
                result.append("\\n");
                continue;
            }
            if (character == '\r') {
                result.append("\\r");
                continue;
            }
            if (character == '\t') {
                result.append("\\t");
                continue;
            }
            if (character < 0x20 || character > 0x7e) {
                result.append(String.format(java.util.Locale.ROOT, "\\u%04x",
                        Integer.valueOf(character)));
                continue;
            }
            if (character == '\\' || character == '=' || character == ':'
                    || (key && Character.isWhitespace(character))) result.append('\\');
            result.append(character);
        }
        return result.toString();
    }
}

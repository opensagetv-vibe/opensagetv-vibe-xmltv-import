package xmltv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public final class ConfigurationProfilesTest {
    private ConfigurationProfilesTest() {
    }

    public static void main(String[] args) throws Exception {
        File work = Files.createTempDirectory("xmltv-profiles-").toFile();
        try {
            List<String> logs = new ArrayList<String>();
            ConfigurationProfiles.LogSink sink = logs::add;
            Properties defaults = new Properties();
            defaults.setProperty("shared", "default");
            defaults.setProperty("default.only", "yes");

            write(new File(work, "common.properties"),
                    "shared=common\ncommon.only=yes\n");
            write(new File(work, "custom.properties"),
                    "shared=include\ninclude.only=yes\n");
            write(new File(work, "xmltv_EPG123.profile"),
                    "include=common.properties\nshared=profile\nprofile.only=yes\n"
                    + "xmltv.channel.display-name.ShortNameIndex=1\n");

            File legacy = new File(work, "legacy.xmltv.properties");
            write(legacy, "include=common.properties\nshared=legacy\nlegacy.only=yes\n");
            Properties legacyResult = ConfigurationProfiles.load(legacy, defaults, sink);
            equals("legacy", legacyResult.getProperty("shared"),
                    "legacy source did not override include");
            equals("yes", legacyResult.getProperty("common.only"),
                    "legacy include stopped working");
            equals("yes", legacyResult.getProperty("default.only"),
                    "built-in defaults stopped working");

            write(new File(work, "first.properties"),
                    "include=common.properties\nfirst.only=yes\n");
            write(new File(work, "second.properties"), "shared=second\n");
            File legacyDiamond = new File(work, "legacy-diamond.xmltv.properties");
            write(legacyDiamond, "include=first.properties,second.properties\n");
            Properties diamondResult = ConfigurationProfiles.load(
                    legacyDiamond, defaults, sink);
            equals("common", diamondResult.getProperty("shared"),
                    "historical shared-include ordering changed");

            write(new File(work, "cycle-a.properties"),
                    "include=cycle-b.properties\ncycle.a=yes\n");
            write(new File(work, "cycle-b.properties"),
                    "include=cycle-a.properties\ncycle.b=yes\n");
            File cyclic = new File(work, "cyclic.xmltv.properties");
            write(cyclic, "include=cycle-a.properties\n");
            Properties cycleResult = ConfigurationProfiles.load(cyclic, defaults, sink);
            equals("yes", cycleResult.getProperty("cycle.a"),
                    "cyclic include lost first file");
            equals("yes", cycleResult.getProperty("cycle.b"),
                    "cyclic include lost second file");

            File layered = new File(work, "layered.xmltv.properties");
            write(layered, "include=custom.properties\nxmltv.profile=EPG123\n"
                    + "shared=source\nxmltv.channel.display-name.ShortNameIndex=9\n");
            Properties layeredResult = ConfigurationProfiles.load(layered, defaults, sink);
            equals("source", layeredResult.getProperty("shared"),
                    "source did not override selected profile");
            equals("9", layeredResult.getProperty(
                    "xmltv.channel.display-name.ShortNameIndex"),
                    "source setting did not override profile setting");
            equals("yes", layeredResult.getProperty("profile.only"),
                    "selected profile was not loaded");
            equals("yes", layeredResult.getProperty("include.only"),
                    "existing include was not retained");
            equals("yes", layeredResult.getProperty("common.only"),
                    "profile include was not loaded");

            File profileWins = new File(work, "profile-wins.xmltv.properties");
            write(profileWins, "include=custom.properties\n"
                    + "xmltv.profile=xmltv_EPG123.profile\n");
            Properties profileResult = ConfigurationProfiles.load(profileWins, defaults, sink);
            equals("profile", profileResult.getProperty("shared"),
                    "profile did not override ordinary include");

            File caseInsensitive = new File(work, "case-insensitive.xmltv.properties");
            write(caseInsensitive, "xmltv.profile=epg123\n");
            Properties caseResult = ConfigurationProfiles.load(
                    caseInsensitive, defaults, sink);
            equals("yes", caseResult.getProperty("profile.only"),
                    "profile selection unexpectedly required exact filename case");

            File missing = new File(work, "missing.xmltv.properties");
            write(missing, "xmltv.profile=DoesNotExist\nshared=still-works\n");
            Properties missingResult = ConfigurationProfiles.load(missing, defaults, sink);
            equals("still-works", missingResult.getProperty("shared"),
                    "missing profile broke existing configuration");
            assertTrue(logs.stream().anyMatch(value -> value.contains("profile not found")),
                    "missing profile was not diagnosed");

            File automatic = new File(work, "automatic.xmltv.properties");
            write(automatic, "provider.name=My Provider / East\nprovider.id=123\n"
                    + "xmltv.files=/guide.xml\nxmltv.profile=auto\n"
                    + "run.before=refresh-guide\ntimeslot=0100-0200\n"
                    + "xmltv.channel.display-name.ShortNameIndex=4\n"
                    + "channel.2.names=CALL,Channel Two\n");
            Properties automaticResult = ConfigurationProfiles.load(
                    automatic, defaults, sink);
            File generated = new File(work, "xmltv_My_Provider_East.profile");
            assertTrue(generated.isFile(), "auto profile was not created");
            Properties generatedProperties = load(generated);
            equals("common.properties", generatedProperties.getProperty("include"),
                    "auto profile does not include common.properties");
            equals("4", generatedProperties.getProperty(
                    "xmltv.channel.display-name.ShortNameIndex"),
                    "auto profile did not retain a format setting");
            assertTrue(generatedProperties.getProperty("provider.name") == null,
                    "auto profile copied provider identity");
            assertTrue(generatedProperties.getProperty("xmltv.files") == null,
                    "auto profile copied feed location");
            assertTrue(generatedProperties.getProperty("run.before") == null,
                    "auto profile copied source acquisition command");
            assertTrue(generatedProperties.getProperty("timeslot") == null,
                    "auto profile copied source update timeslot");
            assertTrue(generatedProperties.getProperty("channel.2.names") == null,
                    "auto profile copied a channel override");
            equals("4", automaticResult.getProperty(
                    "xmltv.channel.display-name.ShortNameIndex"),
                    "auto migration changed explicit behavior");

            Properties rewritten = load(automatic);
            equals("My_Provider_East", rewritten.getProperty("xmltv.profile"),
                    "auto selection was not persisted");
            String profileBefore = new String(Files.readAllBytes(generated.toPath()),
                    StandardCharsets.UTF_8);
            ConfigurationProfiles.load(automatic, defaults, sink);
            String profileAfter = new String(Files.readAllBytes(generated.toPath()),
                    StandardCharsets.UTF_8);
            equals(profileBefore, profileAfter, "existing generated profile was overwritten");
            assertTrue(findTemporaryFiles(work).isEmpty(),
                    "atomic profile generation left temporary files");

            if (args.length > 0) validateShippedProfiles(new File(args[0]));
            System.out.println("[PASS] legacy configuration and XMLTV profile precedence/auto generation");
        } finally {
            deleteTree(work);
        }
    }

    private static void validateShippedProfiles(File directory) throws Exception {
        assertTrue(new File(directory, "common.properties").isFile(),
                "shipped common.properties missing");
        for (String name : Arrays.asList("Generic", "EPG123", "Zap2XML", "Pluto",
                "Threadfin", "xTeVe", "IPTV", "OTA_FTA", "FTA_60177")) {
            File profile = ConfigurationProfiles.profileFile(directory, name);
            assertTrue(profile.isFile(), "shipped profile missing: " + profile);
            equals("common.properties", load(profile).getProperty("include"),
                    "shipped profile does not include common.properties: " + name);
        }
    }

    private static List<File> findTemporaryFiles(File directory) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".tmp"));
        return files == null ? new ArrayList<File>() : Arrays.asList(files);
    }

    private static Properties load(File file) throws Exception {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        }
        return properties;
    }

    private static void write(File file, String content) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void equals(String expected, String actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void deleteTree(File file) throws Exception {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        Files.deleteIfExists(file.toPath());
    }
}

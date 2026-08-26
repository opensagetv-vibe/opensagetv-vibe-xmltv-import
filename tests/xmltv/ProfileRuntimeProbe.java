package xmltv;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

/** Read-only commissioning probe for a profile-backed server configuration. */
public final class ProfileRuntimeProbe {
    private ProfileRuntimeProbe() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "usage: ProfileRuntimeProbe CONFIG_FILE PROVIDER_ID PROVIDER_NAME");
        }
        File configurationFile = new File(args[0]);
        Properties resolved = ConfigurationProfiles.load(configurationFile,
                new Properties(), message -> System.err.println(message));
        XMLTVImportPlugin plugin = new XMLTVImportPlugin();
        String[][] providers = plugin.getLocalMarkets();
        boolean found = false;
        for (String[] provider : providers) {
            if (args[1].equals(provider[0]) && args[2].equals(provider[1])) found = true;
        }
        if (!found) {
            throw new AssertionError("provider not discovered: "
                    + Arrays.deepToString(providers));
        }
        System.out.println("provider=" + args[1] + "/" + args[2]
                + " profile=" + resolved.getProperty("xmltv.profile")
                + " shortIndex=" + resolved.getProperty(
                        "xmltv.channel.display-name.ShortNameIndex")
                + " shortRegex=" + resolved.getProperty(
                        "xmltv.channel.display-name.ShortNameRegex")
                + " longIndex=" + resolved.getProperty(
                        "xmltv.channel.display-name.LongNameIndex")
                + " numberTag=" + resolved.getProperty("xmltv.channel.NumberTag")
                + " numberTagIndex=" + resolved.getProperty("xmltv.channel.NumberTagIndex")
                + " numberRegex=" + resolved.getProperty("xmltv.channel.NumberTagRegEx")
                + " iconDownload=" + resolved.getProperty("sagetv.channel.IconDownload"));
    }
}

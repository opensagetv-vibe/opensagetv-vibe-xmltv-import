package xmltv;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

public final class ProviderReloadTest {
    public static void main(String[] args) throws Exception {
        File nested = new File("nested");
        if (!nested.mkdirs()) throw new IllegalStateException("unable to create nested test directory");
        Files.write(new File(nested, "base.properties").toPath(),
                Arrays.asList("provider.name=Included Provider"), StandardCharsets.ISO_8859_1);
        File first = new File("one.xmltv.properties");
        Files.write(first.toPath(), Arrays.asList("provider.id=101", "include=nested/base.properties"),
                StandardCharsets.ISO_8859_1);

        XMLTVImportPlugin plugin = new XMLTVImportPlugin();
        assertProvider(plugin.getLocalMarkets(), "101", "Included Provider", 1);

        File second = new File("two.xmltv.properties");
        Files.write(second.toPath(), Arrays.asList("provider.id=102", "provider.name=Second Provider"),
                StandardCharsets.ISO_8859_1);
        String[][] providers = plugin.getLocalMarkets();
        if (providers.length != 2) throw new AssertionError("new configuration was not discovered");

        if (!first.delete()) throw new IllegalStateException("unable to delete first provider");
        assertProvider(plugin.getLocalMarkets(), "102", "Second Provider", 1);

        File invalid = new File("invalid.xmltv.properties");
        Files.write(invalid.toPath(), Arrays.asList("provider.id=-1", "provider.name=Invalid"),
                StandardCharsets.ISO_8859_1);
        assertProvider(plugin.getLocalMarkets(), "102", "Second Provider", 1);
        System.out.println("[PASS] provider configuration add/delete reload, relative include, and validation");
    }

    private static void assertProvider(String[][] providers, String id, String name, int count) {
        if (providers.length != count || !id.equals(providers[0][0])
                || !name.equals(providers[0][1])) {
            throw new AssertionError("unexpected providers: " + Arrays.deepToString(providers));
        }
    }
}

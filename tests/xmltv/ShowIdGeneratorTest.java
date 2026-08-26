package xmltv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.Properties;

/** Characterization and v2 persistence tests for generated XMLTV Show IDs. */
public final class ShowIdGeneratorTest {
  public static void main(String[] args) throws Exception {
    File work = Files.createTempDirectory("xmltv-show-id-test-").toFile();
    try {
      testLegacyCompatibility(work);
      testExplicitIdsRemainAuthoritative(work);
      testV2DeterminismAndPersistence(work);
      testV2UnnumberedEpisodes(work);
      testProviderNamespaceAndUnambiguousFields(work);
      testInvalidStrategy(work);
      testInvalidMappingIsRejected(work);
      System.out.println("[PASS] legacy and v2 XMLTV Show-ID generation");
    } finally {
      delete(work);
    }
  }

  private static void testLegacyCompatibility(File work) throws Exception {
    ShowIdGenerator generator = new ShowIdGenerator("legacy", "999:Identity Test",
        new File(work, "unused.properties"), 14400000L);
    Show first = episode("First", 1);
    Show second = episode("Second", 2);
    assertEquals("EP2YRXUU0001",
        generator.generate(first, "Series", new Channel("test")).getExternalId(),
        "first legacy ID changed");
    assertEquals("EP1KbtI40002",
        generator.generate(second, "Series", new Channel("test")).getExternalId(),
        "second legacy ID changed");
    assertTrue(!new File(work, "unused.properties").exists(),
        "legacy strategy created a mapping file");
  }

  private static void testExplicitIdsRemainAuthoritative(File work) throws Exception {
    Show show = episode("Explicit", 3);
    show.showId = "EP12345678900003";
    ShowIdGenerator v2 = new ShowIdGenerator("v2", "999:Identity Test",
        new File(work, "explicit.properties"), 14400000L);
    ShowIdGenerator.Result result = v2.generate(show, "Series", new Channel("test"));
    assertEquals(show.showId, result.getExternalId(), "v2 rewrote a provider ID");
    assertEquals(Integer.valueOf(1234567890), result.getSeriesId(),
        "explicit provider SeriesInfo ID changed");
    v2.flush();
    assertTrue(!new File(work, "explicit.properties").exists(),
        "provider IDs should not create fallback mappings");
  }

  private static void testV2DeterminismAndPersistence(File work) throws Exception {
    File mapping = new File(work, "v2.properties");
    ShowIdGenerator firstRun = new ShowIdGenerator("v2", "999:Identity Test",
        mapping, 14400000L);
    ShowIdGenerator.Result first = firstRun.generate(episode("First", 1),
        "Series", new Channel("test"));
    ShowIdGenerator.Result second = firstRun.generate(episode("Second", 2),
        "Series", new Channel("test"));
    assertTrue(first.getExternalId().matches("EP[0-9]{1,10}0001"),
        "v2 ID format is invalid: " + first.getExternalId());
    assertTrue(!first.getExternalId().equals(second.getExternalId()),
        "different episodes received the same v2 ID");
    assertEquals(first.getSeriesId(), second.getSeriesId(),
        "episodes in one series received different numeric series IDs");
    assertSeriesLinkage(first);
    assertSeriesLinkage(second);
    firstRun.flush();
    assertTrue(mapping.isFile(), "v2 mapping file was not persisted");

    Properties persisted = new Properties();
    try (FileInputStream input = new FileInputStream(mapping)) {
      persisted.load(input);
    }
    assertEquals("v2-sha256-numeric-v1", persisted.getProperty("_format"),
        "mapping format marker is missing");
    assertEquals(Integer.valueOf(3), Integer.valueOf(countMappings(persisted)),
        "expected two show mappings and one shared series mapping key");

    ShowIdGenerator secondRun = new ShowIdGenerator("v2", "999:Identity Test",
        mapping, 14400000L);
    ShowIdGenerator.Result repeated = secondRun.generate(episode("First", 1),
        "Series", new Channel("test"));
    assertEquals(first.getExternalId(), repeated.getExternalId(),
        "v2 ID changed after reloading its mapping");
    assertEquals(first.getSeriesId(), repeated.getSeriesId(),
        "v2 series ID changed after reloading its mapping");
  }

  private static void testProviderNamespaceAndUnambiguousFields(File work) throws Exception {
    ShowIdGenerator providerA = new ShowIdGenerator("v2", "1:Provider",
        new File(work, "provider-a.properties"), 0);
    ShowIdGenerator providerB = new ShowIdGenerator("v2", "2:Provider",
        new File(work, "provider-b.properties"), 0);
    String a = providerA.generate(episode("First", 1), "Series",
        new Channel("test")).getExternalId();
    String b = providerB.generate(episode("First", 1), "Series",
        new Channel("test")).getExternalId();
    assertTrue(!a.equals(b), "provider namespaces produced the same v2 ID");

    Show left = episode(null, 1);
    left.seriesId = "ab";
    left.title = "c";
    Show right = episode(null, 1);
    right.seriesId = "a";
    right.title = "bc";
    String leftId = providerA.generate(left, "Series", new Channel("test"))
        .getExternalId();
    String rightId = providerA.generate(right, "Series", new Channel("test"))
        .getExternalId();
    assertTrue(!leftId.equals(rightId),
        "length-ambiguous identity fields produced the same v2 ID");
  }

  private static void testV2UnnumberedEpisodes(File work) throws Exception {
    ShowIdGenerator generator = new ShowIdGenerator("v2", "id:44",
        new File(work, "unnumbered.properties"), 0);
    Show first = episode("Pilot", 0);
    Show second = episode("Finale", 0);
    first.season = 0;
    second.season = 0;
    ShowIdGenerator.Result firstResult = generator.generate(first, "Series",
        new Channel("test"));
    ShowIdGenerator.Result secondResult = generator.generate(second, "Series",
        new Channel("test"));
    assertEquals(firstResult.getSeriesId(), secondResult.getSeriesId(),
        "unnumbered episodes in one series received different series IDs");
    assertTrue(!firstResult.getExternalId().equals(secondResult.getExternalId()),
        "unnumbered episodes received the same v2 episode token");
    assertSeriesLinkage(firstResult);
    assertSeriesLinkage(secondResult);
  }

  private static void testInvalidStrategy(File work) throws Exception {
    boolean rejected = false;
    try {
      new ShowIdGenerator("future", "provider", new File(work, "bad.properties"), 0);
    } catch (IllegalArgumentException expected) {
      rejected = true;
    }
    assertTrue(rejected, "unknown Show-ID strategy was accepted");
  }

  private static void testInvalidMappingIsRejected(File work) throws Exception {
    File mapping = new File(work, "invalid-map.properties");
    Properties invalid = new Properties();
    invalid.setProperty("_format", "unknown");
    try (FileOutputStream output = new FileOutputStream(mapping)) {
      invalid.store(output, "invalid test mapping");
    }
    boolean rejected = false;
    try {
      new ShowIdGenerator("v2", "provider", mapping, 0);
    } catch (java.io.IOException expected) {
      rejected = true;
    }
    assertTrue(rejected, "unknown persisted mapping format was accepted");
  }

  private static Show episode(String episodeName, int episode) {
    Show show = new Show(new Date(1787659200000L + episode * 1800000L),
        new Date(1787661000000L + episode * 1800000L));
    show.seriesId = "SH01234567";
    show.title = "Example Series";
    show.episodeName = episodeName;
    show.season = 1;
    show.episode = episode;
    return show;
  }

  private static int countMappings(Properties properties) {
    int count = 0;
    for (String key : properties.stringPropertyNames()) {
      if (key.startsWith("show.") || key.startsWith("series.")) count++;
    }
    return count;
  }

  private static void assertSeriesLinkage(ShowIdGenerator.Result result) {
    String externalId = result.getExternalId();
    Integer parsed = Integer.valueOf(externalId.substring(2, externalId.length() - 4));
    assertEquals(result.getSeriesId(), parsed,
        "v2 external ID cannot be linked by SageTV Show.getSeriesInfo()");
  }

  private static void delete(File file) {
    if (file == null || !file.exists()) return;
    File[] children = file.listFiles();
    if (children != null) for (File child : children) delete(child);
    if (!file.delete()) file.deleteOnExit();
  }

  private static void assertEquals(Object expected, Object actual, String message) {
    if (expected == null ? actual != null : !expected.equals(actual)) {
      throw new AssertionError(message + ": expected=" + expected + " actual=" + actual);
    }
  }

  private static void assertTrue(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }
}

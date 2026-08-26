package xmltv;

/** Verifies that an installed importer remains selectable before configuration. */
public final class ProviderDiscoveryTest {
  public static void main(String[] args) {
    String[][] providers = new XMLTVImportPlugin().getLocalMarkets();
    if (providers.length != 1 || providers[0].length != 2 ||
        !"867507149".equals(providers[0][0]) ||
        !"XMLTV Lineup".equals(providers[0][1])) {
      throw new AssertionError("default XMLTV provider missing");
    }
    System.out.println("[PASS] Unconfigured XMLTV provider remains selectable without a license");
  }
}

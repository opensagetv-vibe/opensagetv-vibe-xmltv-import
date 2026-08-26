package xmltv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class XMLInputStreamFilterTest {
  private static String filter(String value) throws Exception {
    XMLInputStreamFilter in = new XMLInputStreamFilter(
        new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int valueRead;
    while ((valueRead = in.read()) != -1) out.write(valueRead);
    return out.toString("UTF-8");
  }

  public static void main(String[] args) throws Exception {
    if (!"AB".equals(filter("A\u0001B"))) throw new AssertionError("control byte not removed");
    if (!"AB".equals(filter("A&#1;B"))) throw new AssertionError("invalid charref not removed");
    if (!"A&#123".equals(filter("A&#123"))) throw new AssertionError("truncated charref not replayed");
    StringBuilder longRefBuilder = new StringBuilder("A&#");
    for (int i = 0; i < 80; i++) longRefBuilder.append('1');
    String longRef = longRefBuilder.toString();
    if (!longRef.equals(filter(longRef))) throw new AssertionError("oversized charref not bounded");
    System.out.println("[PASS] XML input filtering and malformed-entity containment");
  }
}

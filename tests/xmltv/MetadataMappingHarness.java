package xmltv;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import sage.EPGDBPublic2;

public final class MetadataMappingHarness {
    public static void main(String[] args) throws Exception {
        final Map<String, Long> calls = new HashMap<String, Long>();
        final boolean[] movie = new boolean[1];
        final boolean[] series = new boolean[1];
        final boolean[] subtitle = new boolean[1];
        final long expectedPrevious = Instant.parse("2025-08-25T16:30:00Z").toEpochMilli();
        EPGDBPublic2 db = (EPGDBPublic2) Proxy.newProxyInstance(
                MetadataMappingHarness.class.getClassLoader(),
                new Class<?>[] { EPGDBPublic2.class }, (proxy, method, values) -> {
                    calls.put(method.getName(), calls.getOrDefault(method.getName(), 0L) + 1L);
                    if ("addShowPublic2".equals(method.getName())) {
                        String title = String.valueOf(values[0]);
                        if ("Test Movie".equals(title)) {
                            movie[0] = "English Subtitle".equals(values[1])
                                    && String.valueOf(values[2]).startsWith("English description.")
                                    && "PG-13".equals(values[7]);
                        }
                        if ("Test Series".equals(title)) {
                            series[0] = "TV14".equals(values[7])
                                    && "TV14".equals(values[10])
                                    && ((Long) values[14]).longValue() == expectedPrevious
                                    && ((Short) values[15]).shortValue() == 2
                                    && ((Short) values[16]).shortValue() == 2;
                        }
                    } else if ("addAiringPublic2".equals(method.getName())) {
                        int miscellaneous = ((Integer) values[5]).intValue();
                        if ((miscellaneous & EPGDBPublic2.SUBTITLE_MASK) != 0) subtitle[0] = true;
                    }
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return true;
                    if (result == int.class) return 1;
                    if (result == long.class) return 1L;
                    if (result == byte.class) return (byte) 1;
                    if (result == short.class) return (short) 1;
                    return null;
                });
        boolean result = new XMLTVImportPlugin().updateGuide("999", db);
        System.out.println("result=" + result + " movie=" + movie[0] + " series=" + series[0]
                + " subtitle=" + subtitle[0] + " calls=" + calls);
        if (!result || !movie[0] || !series[0] || !subtitle[0]) System.exit(2);
        if (calls.getOrDefault("addShowPublic2", 0L).longValue() != 2L
                || calls.getOrDefault("addAiringPublic2", 0L).longValue() != 2L) {
            throw new AssertionError("invalid-duration programme was not contained");
        }
    }
}

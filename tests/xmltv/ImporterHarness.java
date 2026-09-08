package xmltv;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import sage.EPGDBPublic2;

public final class ImporterHarness {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) throw new IllegalArgumentException("XMLTV file required");
    final Map<String,Long> calls = new HashMap<>();
    final Map<String,String> identities = new HashMap<>();
    final Set<String> conflicts = new HashSet<>();
    final Set<String> showIds = new TreeSet<>();
    final long[] displayed = new long[2];
    final long[] tmdbEnriched = new long[1];
    final int[] lineupStations = new int[]{-1};
    EPGDBPublic2 db = (EPGDBPublic2) Proxy.newProxyInstance(
        ImporterHarness.class.getClassLoader(), new Class<?>[]{EPGDBPublic2.class}, (p,m,a) -> {
          calls.put(m.getName(), calls.getOrDefault(m.getName(), 0L) + 1);
          if ("addShowPublic2".equals(m.getName())) {
            String id=String.valueOf(a[12]);
            showIds.add(id);
            String fingerprint=String.valueOf(a[0])+"\u001f"+String.valueOf(a[1])+"\u001f"+
                String.valueOf(a[2])+"\u001f"+String.valueOf(a[15])+"\u001f"+String.valueOf(a[16]);
            String previous=identities.putIfAbsent(id,fingerprint);
            if (previous!=null && !previous.equals(fingerprint)) conflicts.add(id);
            String marker="Show ID: "+id;
            if (a[2] != null && String.valueOf(a[2]).contains(marker)) displayed[0]++;
            if (a[11] instanceof String[]) {
              for (String bonus : (String[])a[11]) if (marker.equals(bonus)) displayed[1]++;
            }
            // The importer derives a programme year from the XMLTV airing date
            // before optional enrichment. TMDB is deliberately fill-only, so
            // that existing value must not be replaced by the synthetic 2024
            // response. Count only fields that were actually missing.
            if ("Episode description".equals(a[2])
                && "en".equals(a[13])) tmdbEnriched[0]++;
          } else if ("setLineup".equals(m.getName()) && a[1] instanceof Map) {
            lineupStations[0]=((Map<?,?>)a[1]).size();
          }
          Class<?> r=m.getReturnType();
          if (r==boolean.class) return true;
          if (r==int.class) return 1;
          if (r==long.class) return 1L;
          if (r==byte.class) return (byte)1;
          if (r==short.class) return (short)1;
          return null;
        });
    boolean ok = new XMLTVImportPlugin().updateGuide("999", db);
    long channels=calls.getOrDefault("addChannelPublic",0L);
    long shows=calls.getOrDefault("addShowPublic2",0L)+calls.getOrDefault("addShowPublic",0L);
    long airings=calls.getOrDefault("addAiringPublic2",0L)+calls.getOrDefault("addAiringPublic",0L);
    Object displayedIds=showIds.size() <= 20 ? showIds : "[" + showIds.size() + " IDs]";
    System.out.printf("result=%s channels=%d shows=%d uniqueShowIds=%d conflictingShowIds=%d airings=%d showIdDescriptions=%d showIdBonus=%d ids=%s calls=%s file=%s lineupStations=%d tmdbEnriched=%d%n",ok,channels,shows,identities.size(),conflicts.size(),airings,displayed[0],displayed[1],displayedIds,calls,new File(args[0]).getName(),lineupStations[0],tmdbEnriched[0]);
    if (!ok || channels==0) System.exit(2);
    if (shows != airings) System.exit(3);
  }
}

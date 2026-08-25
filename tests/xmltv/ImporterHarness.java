package xmltv;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import sage.EPGDBPublic2;

public final class ImporterHarness {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) throw new IllegalArgumentException("XMLTV file required");
    final Map<String,Long> calls = new HashMap<>();
    final Map<String,String> identities = new HashMap<>();
    final Set<String> conflicts = new HashSet<>();
    EPGDBPublic2 db = (EPGDBPublic2) Proxy.newProxyInstance(
        ImporterHarness.class.getClassLoader(), new Class<?>[]{EPGDBPublic2.class}, (p,m,a) -> {
          calls.put(m.getName(), calls.getOrDefault(m.getName(), 0L) + 1);
          if ("addShowPublic2".equals(m.getName())) {
            String id=String.valueOf(a[12]);
            String fingerprint=String.valueOf(a[0])+"\u001f"+String.valueOf(a[1])+"\u001f"+
                String.valueOf(a[2])+"\u001f"+String.valueOf(a[15])+"\u001f"+String.valueOf(a[16]);
            String previous=identities.putIfAbsent(id,fingerprint);
            if (previous!=null && !previous.equals(fingerprint)) conflicts.add(id);
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
    System.out.printf("result=%s channels=%d shows=%d uniqueShowIds=%d conflictingShowIds=%d airings=%d calls=%s file=%s%n",ok,channels,shows,identities.size(),conflicts.size(),airings,calls,new File(args[0]).getName());
    if (!ok || channels==0) System.exit(2);
    if (shows != airings) System.exit(3);
  }
}

package xmltv;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import sage.EPGDBPublic2;

public final class ImportFailureHarness {
    public static void main(String[] args) throws Exception {
        final boolean rejectShow = args.length > 0 && "reject-show".equals(args[0]);
        final Map<String, Long> calls = new HashMap<String, Long>();
        EPGDBPublic2 db = (EPGDBPublic2) Proxy.newProxyInstance(
                ImportFailureHarness.class.getClassLoader(), new Class<?>[] { EPGDBPublic2.class },
                (proxy, method, values) -> {
                    calls.put(method.getName(), calls.getOrDefault(method.getName(), 0L) + 1L);
                    if (rejectShow && "addShowPublic2".equals(method.getName())) return false;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return true;
                    if (result == int.class) return 1;
                    if (result == long.class) return 1L;
                    if (result == byte.class) return (byte) 1;
                    if (result == short.class) return (short) 1;
                    return null;
                });
        boolean result = new XMLTVImportPlugin().updateGuide("999", db);
        System.out.println("result=" + result + " calls=" + calls);
        if (result) throw new AssertionError("failure scenario returned success");
        if (calls.containsKey("setLineup")) {
            throw new AssertionError("failed import replaced the existing lineup");
        }
    }
}

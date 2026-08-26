package xmltv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import sage.EPGDBPublic2;

public final class ModernInfrastructureTest {
    public static void main(String[] args) throws Exception {
        File work = Files.createTempDirectory("xmltv-modern-").toFile();
        try {
            testDates();
            testSecureXml();
            testConfigurationRedaction();
            testChannelAndProgrammeMapping();
            testLocalAndGzipFeeds(work);
            testHttpFeeds(work);
            testExternalCommands();
            System.out.println("[PASS] secure acquisition, XML parsing, dates, configuration, and commands");
        } finally {
            delete(work);
        }
    }

    private static void testDates() {
        assertEquals(Instant.parse("2026-08-26T12:34:56Z"),
                XmltvDateParser.parseTimestamp("20260826123456 +0000").toInstant(), "UTC seconds");
        assertEquals(Instant.parse("2026-08-26T07:04:00Z"),
                XmltvDateParser.parseTimestamp("202608261234 +0530").toInstant(), "half-hour offset");
        assertEquals(Instant.parse("2026-08-26T12:34:00Z"),
                XmltvDateParser.parseTimestamp("202608261234 Z").toInstant(), "Z offset");
        assertEquals(LocalDateTime.of(2026, 8, 26, 12, 34, 56)
                        .atZone(ZoneId.systemDefault()).toInstant(),
                XmltvDateParser.parseTimestamp("20260826123456").toInstant(),
                "XMLTV local seconds without offset");
        expectFailure(new CheckedRunnable() {
            public void run() { XmltvDateParser.parseTimestamp("20260230120000 +0000"); }
        }, "invalid calendar date");
    }

    private static void testSecureXml() throws Exception {
        DefaultHandler handler = new DefaultHandler();
        XMLReader valid = SecureXmlReader.create(handler, handler);
        valid.parse(new InputSource(new StringReader("<tv><channel id='1'/></tv>")));
        XMLReader xmltvDtd = SecureXmlReader.create(handler, handler);
        xmltvDtd.parse(new InputSource(new StringReader(
                "<!DOCTYPE tv SYSTEM 'xmltv.dtd'><tv><channel id='1'/></tv>")));

        expectFailure(new CheckedRunnable() {
            public void run() throws Exception {
                DefaultHandler xxeHandler = new DefaultHandler();
                XMLReader xxe = SecureXmlReader.create(xxeHandler, xxeHandler);
                xxe.parse(new InputSource(new StringReader(
                        "<!DOCTYPE tv [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]>"
                        + "<tv><channel id='1'><display-name>&xxe;</display-name>"
                        + "</channel></tv>")));
            }
        }, "inline external entity rejection");
    }

    private static void testConfigurationRedaction() {
        Properties properties = new Properties();
        properties.setProperty("api.token", "secret-value");
        properties.setProperty("xmltv.files", "https://user:pass@example.test/feed.xml?token=secret");
        String dump = new XmltvConfiguration(properties).redactedDump("\n");
        assertTrue(!dump.contains("secret-value") && !dump.contains("user:pass")
                && !dump.contains("token=secret"), "configuration secrets must be redacted");
    }

    private static void testChannelAndProgrammeMapping() {
        LinkedHashSet<String> decimal = new LinkedHashSet<String>();
        decimal.add("2.1");
        assertEquals(1012000210, ChannelMapper.stationId("12", "station", decimal),
                "legacy decimal station ID");
        java.util.HashSet<Integer> usedStationIds = new java.util.HashSet<Integer>();
        usedStationIds.add(Integer.valueOf(1012000210));
        int fallbackStationId = ChannelMapper.collisionStationId(
                "12", "different-station", usedStationIds);
        assertTrue(fallbackStationId >= 1500000000 && fallbackStationId < 2000000000
                        && fallbackStationId != 1012000210,
                "station-ID collision fallback is outside its deterministic range");
        assertEquals(Integer.valueOf(fallbackStationId), Integer.valueOf(
                ChannelMapper.collisionStationId("12", "different-station", usedStationIds)),
                "station-ID collision fallback is not deterministic");
        LinkedHashSet<String> hyphen = new LinkedHashSet<String>();
        hyphen.add("5-1");
        assertEquals("6-1", ChannelMapper.normalizeNumbers(hyphen, 1, "-")
                .iterator().next(), "hyphenated channel normalization");

        Show show = new Show(Date.from(Instant.parse("2026-08-26T12:00:00Z")),
                Date.from(Instant.parse("2026-08-26T13:00:00Z")));
        show.season = 2;
        show.episode = 3;
        show.part = 2;
        show.parts = 3;
        show.is_live = true;
        show.is_new = true;
        show.audio = "stereo DD 5.1 surround";
        show.quality = "HDTV widescreen";
        ProgrammeMapper.AiringFields fields = ProgrammeMapper.map(show);
        assertEquals((short) 2, fields.season, "season mapping");
        assertEquals((byte) 0x23, fields.multipart, "multipart mapping");
        assertTrue((fields.miscellaneous & EPGDBPublic2.LIVE_MASK) != 0
                && (fields.miscellaneous & EPGDBPublic2.DD51_MASK) != 0
                && (fields.miscellaneous & EPGDBPublic2.WIDESCREEN_MASK) != 0,
                "SageTV airing flags");
        show.season = Integer.MAX_VALUE;
        show.part = 16;
        fields = ProgrammeMapper.map(show);
        assertEquals((short) 0, fields.season, "out-of-range season containment");
        assertEquals((byte) 0, fields.multipart, "out-of-range multipart containment");
    }

    private static void testLocalAndGzipFeeds(File work) throws Exception {
        byte[] xml = "<tv><channel id='local'/></tv>".getBytes(StandardCharsets.UTF_8);
        File source = new File(work, "source.xml.gz");
        try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(source.toPath()))) {
            gzip.write(xml);
        }
        File target = new File(work, "local.xml");
        FeedDownloader.Options options = new FeedDownloader.Options(1000, 1000, 1024, 2);
        FeedDownloader.acquire(source.getAbsolutePath(), target, options);
        assertEquals(new String(xml, StandardCharsets.UTF_8),
                new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8), "gzip decode");

        File oversized = new File(work, "oversized.xml");
        Files.write(oversized.toPath(), new byte[2048]);
        expectFailure(new CheckedRunnable() {
            public void run() throws Exception {
                FeedDownloader.acquire(oversized.getAbsolutePath(), target, options);
            }
        }, "local size limit");
        assertEquals(new String(xml, StandardCharsets.UTF_8),
                new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8),
                "failed update must preserve prior cache");
    }

    private static void testHttpFeeds(File work) throws Exception {
        final byte[] xml = "<tv><channel id='http'/></tv>".getBytes(StandardCharsets.UTF_8);
        final byte[] gzip = gzip(xml);
        final AtomicInteger completeResponses = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/feed", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                if ("\"fixture-v1\"".equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
                    exchange.sendResponseHeaders(304, -1);
                    exchange.close();
                    return;
                }
                completeResponses.incrementAndGet();
                exchange.getResponseHeaders().set("ETag", "\"fixture-v1\"");
                exchange.getResponseHeaders().set("Content-Encoding", "gzip");
                exchange.sendResponseHeaders(200, gzip.length);
                try (OutputStream output = exchange.getResponseBody()) { output.write(gzip); }
            }
        });
        server.createContext("/redirect", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().set("Location", "/feed");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            }
        });
        server.createContext("/error", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
            }
        });
        server.createContext("/loop", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().set("Location", "/loop");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            }
        });
        server.createContext("/large", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                byte[] body = new byte[8192];
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream output = exchange.getResponseBody()) { output.write(body); }
            }
        });
        server.createContext("/slow", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                exchange.sendResponseHeaders(200, xml.length);
                try (OutputStream output = exchange.getResponseBody()) { output.write(xml); }
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            FeedDownloader.Options options = new FeedDownloader.Options(1000, 1000, 4096, 2);
            File target = new File(work, "http.xml");
            String url = "http://127.0.0.1:" + port + "/redirect";
            FeedDownloader.acquire(url, target, options);
            FeedDownloader.acquire(url, target, options);
            assertEquals(1, completeResponses.get(), "ETag conditional cache");
            assertEquals(new String(xml, StandardCharsets.UTF_8),
                    new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8), "HTTP gzip");
            expectFailure(new CheckedRunnable() {
                public void run() throws Exception {
                    FeedDownloader.acquire("http://127.0.0.1:" + server.getAddress().getPort()
                            + "/error", target, options);
                }
            }, "HTTP status validation");
            expectFailure(new CheckedRunnable() {
                public void run() throws Exception {
                    FeedDownloader.acquire("http://127.0.0.1:" + server.getAddress().getPort()
                            + "/loop", target, options);
                }
            }, "HTTP redirect limit");
            expectFailure(new CheckedRunnable() {
                public void run() throws Exception {
                    FeedDownloader.acquire("http://127.0.0.1:" + server.getAddress().getPort()
                            + "/large", target, options);
                }
            }, "HTTP download limit");
            expectFailure(new CheckedRunnable() {
                public void run() throws Exception {
                    FeedDownloader.Options shortTimeout = new FeedDownloader.Options(1000, 100, 4096, 2);
                    FeedDownloader.acquire("http://127.0.0.1:" + server.getAddress().getPort()
                            + "/slow", target, shortTimeout);
                }
            }, "HTTP read timeout");
        } finally {
            server.stop(0);
        }
    }

    private static void testExternalCommands() throws Exception {
        final List<String> lines = new ArrayList<String>();
        ExternalCommandRunner.LogSink sink = new ExternalCommandRunner.LogSink() {
            public void log(String line) { lines.add(line); }
        };
        assertTrue(ExternalCommandRunner.run("printf command-ok", 2000L, sink).isSuccessful(),
                "successful command");
        assertTrue(lines.toString().contains("command-ok"), "command output");
        assertTrue(!ExternalCommandRunner.run("exit 7", 2000L, sink).isSuccessful(),
                "non-zero command");
        ExternalCommandRunner.Result timeout = ExternalCommandRunner.run("sleep 2", 100L, sink);
        assertTrue(timeout.timedOut, "command timeout");
    }

    private static byte[] gzip(byte[] input) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (GZIPOutputStream output = new GZIPOutputStream(result)) { output.write(input); }
        return result.toByteArray();
    }

    private static void expectFailure(CheckedRunnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " did not fail");
        } catch (AssertionError e) {
            throw e;
        } catch (Exception expected) {
            // expected
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        file.delete();
    }

    private interface CheckedRunnable {
        void run() throws Exception;
    }
}

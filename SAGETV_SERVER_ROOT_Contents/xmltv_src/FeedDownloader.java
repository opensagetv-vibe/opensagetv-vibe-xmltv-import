package xmltv;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FilterInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/** Downloads or copies one XMLTV source into a bounded, atomically replaced cache file. */
final class FeedDownloader {
    static final class Options {
        final int connectTimeoutMillis;
        final int readTimeoutMillis;
        final long maximumBytes;
        final int maximumRedirects;

        Options(int connectTimeoutMillis, int readTimeoutMillis, long maximumBytes,
                int maximumRedirects) {
            if (connectTimeoutMillis < 1 || readTimeoutMillis < 1
                    || maximumBytes < 1 || maximumRedirects < 0) {
                throw new IllegalArgumentException("invalid feed download limits");
            }
            this.connectTimeoutMillis = connectTimeoutMillis;
            this.readTimeoutMillis = readTimeoutMillis;
            this.maximumBytes = maximumBytes;
            this.maximumRedirects = maximumRedirects;
        }
    }

    private FeedDownloader() {
    }

    static File acquire(String source, File destination, Options options) throws IOException {
        if (source == null || source.trim().length() == 0) {
            throw new IOException("XMLTV source is empty");
        }
        Path target = destination.toPath().toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent == null) throw new IOException("XMLTV cache has no parent directory");
        Files.createDirectories(parent);

        if (isUrl(source)) return acquireUrl(source.trim(), target, options).toFile();
        Path input = new File(source.trim()).toPath().toAbsolutePath().normalize();
        if (!Files.isRegularFile(input)) throw new IOException("XMLTV file not found: " + input);
        writeAtomically(new FileInputStream(input.toFile()), target, options.maximumBytes);
        return target.toFile();
    }

    private static Path acquireUrl(String source, Path target, Options options)
            throws IOException {
        URI uri;
        try {
            uri = new URI(source);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid XMLTV URL", e);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if ("file".equals(scheme)) {
            try {
                Path input = new File(uri).toPath().toAbsolutePath().normalize();
                if (!Files.isRegularFile(input)) throw new IOException("XMLTV file not found: " + input);
                writeAtomically(new FileInputStream(input.toFile()), target, options.maximumBytes);
                return target;
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid XMLTV file URL", e);
            }
        }
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IOException("Unsupported XMLTV URL scheme: " + scheme);
        }

        Path metadataPath = target.resolveSibling(target.getFileName().toString() + ".http.properties");
        Properties metadata = loadMetadata(metadataPath);
        String sourceHash = sha256(source);
        URL current = uri.toURL();
        for (int redirects = 0; redirects <= options.maximumRedirects; redirects++) {
            URLConnection raw = current.openConnection();
            if (!(raw instanceof HttpURLConnection)) {
                throw new IOException("XMLTV URL is not HTTP after redirect");
            }
            HttpURLConnection http = (HttpURLConnection) raw;
            http.setInstanceFollowRedirects(false);
            http.setConnectTimeout(options.connectTimeoutMillis);
            http.setReadTimeout(options.readTimeoutMillis);
            http.setRequestProperty("Accept-Encoding", "gzip");
            http.setRequestProperty("User-Agent", "OpenSageTV-XMLTV/3.3");
            if (sourceHash.equals(metadata.getProperty("source.sha256")) && Files.isRegularFile(target)) {
                String etag = metadata.getProperty("etag");
                String modified = metadata.getProperty("last-modified");
                if (etag != null && etag.length() > 0) http.setRequestProperty("If-None-Match", etag);
                if (modified != null && modified.length() > 0) {
                    http.setRequestProperty("If-Modified-Since", modified);
                }
            }

            try {
                int status = http.getResponseCode();
                if (status == HttpURLConnection.HTTP_NOT_MODIFIED && Files.isRegularFile(target)) {
                    return target;
                }
                if (status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_SEE_OTHER
                        || status == 307 || status == 308) {
                    String location = http.getHeaderField("Location");
                    if (location == null) throw new IOException("HTTP redirect has no Location header");
                    if (redirects == options.maximumRedirects) {
                        throw new IOException("Too many XMLTV HTTP redirects");
                    }
                    current = current.toURI().resolve(location).toURL();
                    String redirectedScheme = current.getProtocol().toLowerCase(Locale.ROOT);
                    if (!"http".equals(redirectedScheme) && !"https".equals(redirectedScheme)) {
                        throw new IOException("HTTP redirect changed to unsupported scheme");
                    }
                    continue;
                }
                if (status < 200 || status >= 300) {
                    throw new IOException("XMLTV HTTP request failed with status " + status);
                }
                long contentLength = http.getContentLengthLong();
                if (contentLength > options.maximumBytes) {
                    throw new IOException("XMLTV response exceeds configured download limit");
                }
                InputStream response = new BoundedInputStream(http.getInputStream(),
                        options.maximumBytes, "compressed XMLTV response");
                writeAtomically(response, target, options.maximumBytes);

                Properties updated = new Properties();
                updated.setProperty("source.sha256", sourceHash);
                String etag = http.getHeaderField("ETag");
                String modified = http.getHeaderField("Last-Modified");
                if (etag != null) updated.setProperty("etag", etag);
                if (modified != null) updated.setProperty("last-modified", modified);
                storeMetadata(updated, metadataPath);
                return target;
            } catch (URISyntaxException e) {
                throw new IOException("Invalid XMLTV redirect", e);
            } finally {
                http.disconnect();
            }
        }
        throw new IOException("Too many XMLTV HTTP redirects");
    }

    private static void writeAtomically(InputStream original, Path target, long maximumBytes)
            throws IOException {
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            try (InputStream source = compressionAware(new BufferedInputStream(original));
                 OutputStream output = new BufferedOutputStream(new FileOutputStream(temporary.toFile()))) {
                byte[] buffer = new byte[65536];
                long total = 0L;
                int read;
                while ((read = source.read(buffer)) != -1) {
                    total += read;
                    if (total > maximumBytes) {
                        throw new IOException("Decoded XMLTV feed exceeds configured size limit");
                    }
                    output.write(buffer, 0, read);
                }
                output.flush();
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) Files.deleteIfExists(temporary);
        }
    }

    private static InputStream compressionAware(InputStream input) throws IOException {
        PushbackInputStream pushback = new PushbackInputStream(input, 2);
        int first = pushback.read();
        int second = pushback.read();
        if (second >= 0) pushback.unread(second);
        if (first >= 0) pushback.unread(first);
        return first == 0x1f && second == 0x8b ? new GZIPInputStream(pushback) : pushback;
    }

    private static boolean isUrl(String source) {
        return source.indexOf("://") > 0;
    }

    private static Properties loadMetadata(Path path) {
        Properties properties = new Properties();
        if (!Files.isRegularFile(path)) return properties;
        try (InputStream input = new FileInputStream(path.toFile())) {
            properties.load(input);
        } catch (IOException ignored) {
            properties.clear();
        }
        return properties;
    }

    private static void storeMetadata(Properties properties, Path path) throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            try (OutputStream output = new FileOutputStream(temporary.toFile())) {
                properties.store(output, "OpenSageTV XMLTV HTTP cache");
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) Files.deleteIfExists(temporary);
        }
    }

    private static String sha256(String value) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : digest) result.append(String.format(Locale.ROOT, "%02x", item & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is unavailable", e);
        }
    }

    private static final class BoundedInputStream extends FilterInputStream {
        private final long maximum;
        private final String description;
        private long count;

        BoundedInputStream(InputStream input, long maximum, String description) {
            super(input);
            this.maximum = maximum;
            this.description = description;
        }

        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) add(1);
            return value;
        }

        public int read(byte[] buffer, int offset, int length) throws IOException {
            int value = super.read(buffer, offset, length);
            if (value > 0) add(value);
            return value;
        }

        private void add(long amount) throws IOException {
            this.count += amount;
            if (this.count > this.maximum) {
                throw new IOException(this.description + " exceeds configured size limit");
            }
        }
    }
}

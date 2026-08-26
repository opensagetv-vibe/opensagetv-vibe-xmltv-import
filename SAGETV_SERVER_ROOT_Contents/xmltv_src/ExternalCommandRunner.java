package xmltv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Executes the optional pre-import command with bounded lifetime and output. */
final class ExternalCommandRunner {
    interface LogSink {
        void log(String line);
    }

    static final class Result {
        final int exitCode;
        final boolean timedOut;

        Result(int exitCode, boolean timedOut) {
            this.exitCode = exitCode;
            this.timedOut = timedOut;
        }

        boolean isSuccessful() {
            return !this.timedOut && this.exitCode == 0;
        }
    }

    private ExternalCommandRunner() {
    }

    static Result run(String command, long timeoutMillis, final LogSink log)
            throws IOException, InterruptedException {
        if (command == null || command.trim().length() == 0) return new Result(0, false);
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        ProcessBuilder builder = windows
                ? new ProcessBuilder("cmd.exe", "/d", "/s", "/c", command)
                : new ProcessBuilder("/bin/sh", "-c", command);
        builder.redirectErrorStream(true);
        final Process process = builder.start();
        Thread outputReader = new Thread(new Runnable() {
            public void run() {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    int count = 0;
                    while ((line = reader.readLine()) != null) {
                        if (count++ < 10000) log.log(line);
                        else if (count == 10001) log.log("run.before output truncated after 10000 lines");
                    }
                } catch (IOException e) {
                    log.log("Unable to read run.before output: " + e.getMessage());
                }
            }
        }, "xmltv-run-before-output");
        outputReader.setDaemon(true);
        outputReader.start();

        boolean finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroy();
            if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
        }
        outputReader.join(2000L);
        return new Result(finished ? process.exitValue() : -1, !finished);
    }
}

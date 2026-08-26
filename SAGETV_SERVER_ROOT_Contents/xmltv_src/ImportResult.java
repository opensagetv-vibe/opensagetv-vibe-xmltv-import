package xmltv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Collects one provider update's outcome without hiding partial failures. */
final class ImportResult {
    private final List<String> failures = new ArrayList<String>();
    private int configurationsAttempted;
    private int configurationsSucceeded;
    private int feedsAttempted;
    private int feedsSucceeded;

    void configurationStarted() {
        this.configurationsAttempted++;
    }

    void configurationSucceeded() {
        this.configurationsSucceeded++;
    }

    void feedStarted() {
        this.feedsAttempted++;
    }

    void feedSucceeded() {
        this.feedsSucceeded++;
    }

    void fail(String phase, Throwable failure) {
        String message = failure == null ? "unknown failure" : failure.getMessage();
        if (message == null || message.trim().length() == 0) {
            message = failure == null ? "unknown failure" : failure.getClass().getName();
        }
        this.failures.add(phase + ": " + message);
    }

    void fail(String phase, String message) {
        this.failures.add(phase + ": " + message);
    }

    boolean isSuccessful() {
        return this.failures.isEmpty()
                && this.configurationsAttempted > 0
                && this.configurationsAttempted == this.configurationsSucceeded
                && this.feedsAttempted > 0
                && this.feedsAttempted == this.feedsSucceeded;
    }

    List<String> getFailures() {
        return Collections.unmodifiableList(this.failures);
    }

    boolean hasFailures() {
        return !this.failures.isEmpty();
    }

    int getFailureCount() {
        return this.failures.size();
    }

    public String toString() {
        return "success=" + isSuccessful()
                + ", configurations=" + this.configurationsSucceeded + "/"
                + this.configurationsAttempted
                + ", feeds=" + this.feedsSucceeded + "/" + this.feedsAttempted
                + ", failures=" + this.failures.size();
    }
}

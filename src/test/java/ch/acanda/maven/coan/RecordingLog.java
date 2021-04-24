package ch.acanda.maven.coan;

import lombok.Data;
import lombok.Getter;
import org.apache.maven.plugin.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class RecordingLog implements Log {

    @Getter
    private final List<LogEntry> logEntries = new ArrayList<>();

    public String getLogAsString() {
        return logEntries.stream().map(LogEntry::toString).collect(joining("\n"));
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(final CharSequence content) {
        log("debug", content.toString(), null);
    }

    @Override
    public void debug(final CharSequence content, final Throwable error) {
        log("debug", content.toString(), error);
    }

    @Override
    public void debug(final Throwable error) {
        log("debug", null, error);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(final CharSequence content) {
        log("info", content.toString(), null);
    }

    @Override
    public void info(final CharSequence content, final Throwable error) {
        log("info", content.toString(), error);
    }

    @Override
    public void info(final Throwable error) {
        log("info", null, error);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(final CharSequence content) {
        log("warn", content.toString(), null);
    }

    @Override
    public void warn(final CharSequence content, final Throwable error) {
        log("warn", content.toString(), error);
    }

    @Override
    public void warn(final Throwable error) {
        log("warn", null, error);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(final CharSequence content) {
        log("error", content.toString(), null);
    }

    @Override
    public void error(final CharSequence content, final Throwable error) {
        log("error", content.toString(), error);
    }

    @Override
    public void error(final Throwable error) {
        log("error", null, error);
    }

    private void log(final String level, final String content, final Throwable error) {
        logEntries.add(new LogEntry(level, content, error));
    }

    @Data
    public static class LogEntry {
        private final String level;
        private final String content;
        private final Throwable error;

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append('[').append(level).append(']');
            if (content != null) {
                builder.append(' ').append(content);
            }
            if (error != null) {
                builder.append('\n').append(error.toString());
            }
            return builder.toString();
        }
    }

}

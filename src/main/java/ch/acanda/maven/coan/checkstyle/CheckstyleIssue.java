package ch.acanda.maven.coan.checkstyle;

import ch.acanda.maven.coan.Issue;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;

import java.nio.file.Path;
import java.nio.file.Paths;

public record CheckstyleIssue(AuditEvent event) implements Issue {

    @Override
    public Path file() {
        return Paths.get(event.getFileName());
    }

    @Override
    public int line() {
        return event.getLine();
    }

    @Override
    public int column() {
        return event.getColumn();
    }

    @Override
    public String name() {
        final String sourceName = event.getSourceName();
        final int pos = sourceName.lastIndexOf('.');
        return pos == -1 ? sourceName : sourceName.substring(pos + 1);
    }

    @Override
    public String description() {
        return event.getMessage();
    }

    @Override
    public Severity severity() {
        return switch (event.getSeverityLevel()) {
            case ERROR -> Severity.HIGHEST;
            case WARNING -> Severity.HIGH;
            case INFO -> Severity.MEDIUM;
            case IGNORE -> Severity.IGNORE;
        };
    }

}

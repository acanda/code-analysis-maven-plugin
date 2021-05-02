package ch.acanda.maven.coan.checkstyle;

import ch.acanda.maven.coan.Issue;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
public class CheckstyleIssue implements Issue {

    private final AuditEvent event;

    @Override
    public Path getFile() {
        return Paths.get(event.getFileName());
    }

    @Override
    public int getLine() {
        return event.getLine();
    }

    @Override
    public int getColumn() {
        return event.getColumn();
    }

    @Override
    public String getName() {
        final String sourceName = event.getSourceName();
        final int pos = sourceName.lastIndexOf('.');
        return pos == -1 ? sourceName : sourceName.substring(pos + 1);
    }

    @Override
    public String getDescription() {
        return event.getMessage();
    }

    @Override
    public Severity getSeverity() {
        switch (event.getSeverityLevel()) {
            case ERROR:
                return Severity.HIGHEST;
            case WARNING:
                return Severity.HIGH;
            case INFO:
                return Severity.MEDIUM;
            case IGNORE:
                return Severity.IGNORE;
            default:
                throw new IllegalStateException("Unexpected Checkstyle severity level: " + event.getSeverityLevel());
        }
    }

}

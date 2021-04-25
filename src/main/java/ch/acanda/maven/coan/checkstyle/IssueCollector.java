package ch.acanda.maven.coan.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

class IssueCollector implements AuditListener {

    @Getter
    private final List<CheckstyleIssue> issues = new ArrayList<>();

    @Override
    public void auditStarted(final AuditEvent event) {
        // nothing to do
    }

    @Override
    public void auditFinished(final AuditEvent event) {
        // nothing to do
    }

    @Override
    public void fileStarted(final AuditEvent event) {
        // nothing to do
    }

    @Override
    public void fileFinished(final AuditEvent event) {
        // nothing to do
    }

    @Override
    public void addError(final AuditEvent event) {
        issues.add(new CheckstyleIssue(event));
    }

    @Override
    public void addException(final AuditEvent event, final Throwable throwable) {
        // nothing to do
    }

}

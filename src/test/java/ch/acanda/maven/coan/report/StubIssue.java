package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Issue;

import java.nio.file.Path;

public record StubIssue(
    Path file,
    int line,
    int column,
    String name,
    String description,
    Severity severity
) implements Issue {
}

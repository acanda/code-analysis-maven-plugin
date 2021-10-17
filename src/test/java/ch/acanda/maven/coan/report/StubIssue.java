package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Issue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

@RequiredArgsConstructor
@Getter
public class StubIssue implements Issue {

    private final Path file;
    private final int line;
    private final int column;
    private final String name;
    private final String description;
    private final Severity severity;

}

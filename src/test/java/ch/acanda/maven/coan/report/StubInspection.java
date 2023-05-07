package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Inspection;
import ch.acanda.maven.coan.Issue;
import org.apache.maven.project.MavenProject;

import java.util.List;

public record StubInspection(
    String toolName,
    List<? extends Issue> issues,
    MavenProject project
) implements Inspection {
}

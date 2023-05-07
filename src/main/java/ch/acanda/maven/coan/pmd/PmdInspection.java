package ch.acanda.maven.coan.pmd;

import ch.acanda.maven.coan.Inspection;
import org.apache.maven.project.MavenProject;

import java.util.List;

public record PmdInspection(
    MavenProject project,
    List<PmdIssue> issues
) implements Inspection {

    @Override
    public String toolName() {
        return "PMD";
    }

}

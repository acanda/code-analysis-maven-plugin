package ch.acanda.maven.coan.pmd;

import ch.acanda.maven.coan.Analysis;
import org.apache.maven.project.MavenProject;

import java.util.List;

public record PmdAnalysis(
    MavenProject project,
    List<PmdIssue> issues
) implements Analysis {

    @Override
    public String toolName() {
        return "PMD";
    }

}

package ch.acanda.maven.coan.checkstyle;

import ch.acanda.maven.coan.Analysis;
import org.apache.maven.project.MavenProject;

import java.util.List;

record CheckstyleAnalysis(
    MavenProject project,
    List<CheckstyleIssue> issues
) implements Analysis {

    @Override
    public String toolName() {
        return "Checkstyle";
    }

}

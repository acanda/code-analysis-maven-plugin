package ch.acanda.maven.coan.checkstyle;

import ch.acanda.maven.coan.Inspection;
import org.apache.maven.project.MavenProject;

import java.util.List;

record CheckstyleInspection(
    MavenProject project,
    List<CheckstyleIssue> issues
) implements Inspection {

    @Override
    public String toolName() {
        return "Checkstyle";
    }

}

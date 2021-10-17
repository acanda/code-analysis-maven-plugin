package ch.acanda.maven.coan.checkstyle;

import ch.acanda.maven.coan.Analysis;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.maven.project.MavenProject;

import java.util.List;

@RequiredArgsConstructor
class CheckstyleAnalysis implements Analysis {

    @Getter
    private final MavenProject project;

    @Getter
    private final List<CheckstyleIssue> issues;

    @Override
    public String getToolName() {
        return "Checkstyle";
    }

}

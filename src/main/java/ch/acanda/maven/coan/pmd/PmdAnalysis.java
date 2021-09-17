package ch.acanda.maven.coan.pmd;

import ch.acanda.maven.coan.Analysis;
import ch.acanda.maven.coan.Issue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.maven.project.MavenProject;

import java.util.List;

@RequiredArgsConstructor
public class PmdAnalysis implements Analysis {

    @Getter
    private final MavenProject project;
    @Getter
    private final List<? extends Issue> issues;

    @Override
    public String getToolName() {
        return "PMD";
    }

}

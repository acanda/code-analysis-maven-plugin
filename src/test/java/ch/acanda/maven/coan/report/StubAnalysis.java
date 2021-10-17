package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Analysis;
import ch.acanda.maven.coan.Issue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.maven.project.MavenProject;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class StubAnalysis implements Analysis {

    private final String toolName;
    private final List<? extends Issue> issues;
    private final MavenProject project;

}

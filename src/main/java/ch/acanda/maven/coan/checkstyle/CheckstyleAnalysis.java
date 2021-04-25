package ch.acanda.maven.coan.checkstyle;

import ch.acanda.maven.coan.Analysis;
import ch.acanda.maven.coan.Issue;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
class CheckstyleAnalysis implements Analysis {

    private final List<? extends Issue> issues;

    @Override
    public String getToolName() {
        return "Checkstyle";
    }

    @Override
    public List<? extends Issue> getIssues() {
        return issues;
    }
}

package ch.acanda.maven.coan.pmd;

import ch.acanda.maven.coan.Analysis;
import ch.acanda.maven.coan.Issue;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PmdAnalysis implements Analysis {

    private final List<? extends Issue> issues;

    @Override
    public String getToolName() {
        return "PMD";
    }

    @Override
    public List<? extends Issue> getIssues() {
        return issues;
    }

}

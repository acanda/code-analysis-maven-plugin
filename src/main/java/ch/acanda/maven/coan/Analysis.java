package ch.acanda.maven.coan;

import java.util.List;

public interface Analysis {

    String getToolName();

    default boolean foundIssues() {
        return !getIssues().isEmpty();
    }

    List<Issue> getIssues();

}

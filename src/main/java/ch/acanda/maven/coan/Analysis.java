package ch.acanda.maven.coan;

import org.apache.maven.project.MavenProject;

import java.util.List;

public interface Analysis {

    String getToolName();

    default boolean foundIssues() {
        return !getIssues().isEmpty();
    }

    @SuppressWarnings("java:S1452")
    List<? extends Issue> getIssues();

    default int getNumberOfIssues() {
        return getIssues().size();
    }

    MavenProject getProject();

}

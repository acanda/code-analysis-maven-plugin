package ch.acanda.maven.coan;

import org.apache.maven.project.MavenProject;

import java.util.List;

public interface Analysis {

    String getToolName();

    default boolean foundIssues() {
        return !getIssues().isEmpty();
    }

    List<? extends Issue> getIssues();

    MavenProject getProject();

}

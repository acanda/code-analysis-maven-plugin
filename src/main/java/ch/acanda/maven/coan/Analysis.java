package ch.acanda.maven.coan;

import org.apache.maven.project.MavenProject;

import java.util.List;

public interface Analysis {

    String toolName();

    default boolean foundIssues() {
        return !issues().isEmpty();
    }

    @SuppressWarnings("java:S1452")
    List<? extends Issue> issues();

    default int getNumberOfIssues() {
        return issues().size();
    }

    MavenProject project();

}

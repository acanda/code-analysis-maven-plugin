package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Inspection;
import ch.acanda.maven.coan.Issue;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class GitHubReportTest {

    @Test
    void createGitHubReport(@TempDir final Path tempDir) throws MojoFailureException, IOException {
        final Path baseDir = tempDir.resolve("baseDir");
        final Path targetDir = baseDir.resolve("target");
        Files.createDirectories(targetDir);
        final Path reportFile = targetDir.resolve("report.md");
        final MavenProject project = new MavenProject();
        project.setName("code-inspection-maven-plugin");
        project.setVersion("1.0.0");
        final Path javaMain = baseDir.resolve("src").resolve("main").resolve("java");
        final Path hello = javaMain.resolve("Hello.java");
        final Path world = javaMain.resolve("World.java");
        final GitHubReport report = new GitHubReport(project, baseDir,
            inspection("ABC", List.of(
                issue(hello, 12, "IssueA", "Issue A description", Issue.Severity.HIGHEST),
                issue(world, 25, "IssueB", "Issue B description", Issue.Severity.HIGH),
                issue(hello, 7, "IssueC", "Issue C description", Issue.Severity.MEDIUM),
                issue(world, 38, "IssueD", "Issue D description", Issue.Severity.LOW),
                issue(hello, 24, "IssueE", "Issue E description", Issue.Severity.LOWEST),
                issue(world, 15, "IssueF", "Issue F description", Issue.Severity.IGNORE)
            ))
        );

        report.writeTo(reportFile);

        assertThat(Files.readString(reportFile, UTF_8).replaceAll("\\r?\\n", "\n")).isEqualTo(
            """
                # Code Analysis for code-inspection-maven-plugin 1.0.0

                ## Summary

                ABC found 6 issues.

                ## ABC Report

                - src/main/java/Hello.java
                  - :bangbang: IssueA Issue A description (12:0)
                  - :grey_exclamation: IssueC Issue C description (7:0)
                  - :speak_no_evil: IssueE Issue E description (24:0)

                - src/main/java/World.java
                  - :exclamation: IssueB Issue B description (25:0)
                  - :warning: IssueD Issue D description (38:0)
                  - :information_source: IssueF Issue F description (15:0)

                """);
    }

    private static Inspection inspection(final String tool, final List<? extends Issue> issues) {
        return new StubInspection(tool, issues, new MavenProject());
    }

    private static Issue issue(final Path file, final int line, final String name, final String description,
        final Issue.Severity severity) {
        return new StubIssue(file, line, 0, name, description, severity);
    }

}

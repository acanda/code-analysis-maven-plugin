package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Analysis;
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
        project.setName("code-analysis-maven-plugin");
        project.setVersion("1.0.0");
        final GitHubReport report = new GitHubReport(project, baseDir,
            analysis("ABC", List.of(
                issue(baseDir.resolve("src").resolve("main").resolve("java").resolve("Hello.java"),
                    12, "IssueA", "Issue A description", Issue.Severity.HIGHEST),
                issue(baseDir.resolve("src").resolve("main").resolve("java").resolve("World.java"),
                    25, "IssueB", "Issue B description", Issue.Severity.HIGH),
                issue(baseDir.resolve("src").resolve("main").resolve("java").resolve("Hello.java"),
                    7, "IssueC", "Issue C description", Issue.Severity.MEDIUM),
                issue(baseDir.resolve("src").resolve("main").resolve("java").resolve("World.java"),
                    38, "IssueD", "Issue D description", Issue.Severity.LOW),
                issue(baseDir.resolve("src").resolve("main").resolve("java").resolve("Hello.java"),
                    24, "IssueE", "Issue E description", Issue.Severity.LOWEST),
                issue(baseDir.resolve("src").resolve("main").resolve("java").resolve("World.java"),
                    15, "IssueF", "Issue F description", Issue.Severity.IGNORE)
            ))
        );

        report.writeTo(reportFile);

        assertThat(Files.readString(reportFile, UTF_8).replaceAll("\\r?\\n", "\n")).isEqualTo(
            "# Code Analysis for code-analysis-maven-plugin 1.0.0\n"
            + "\n"
            + "## Summary\n"
            + "\n"
            + "ABC found 6 issues.\n"
            + "\n"
            + "## ABC Report\n"
            + "\n"
            + "- src/main/java/Hello.java\n"
            + "  - :bangbang: IssueA Issue A description (12:0)\n"
            + "  - :grey_exclamation: IssueC Issue C description (7:0)\n"
            + "  - :speak_no_evil: IssueE Issue E description (24:0)\n"
            + "\n"
            + "- src/main/java/World.java\n"
            + "  - :exclamation: IssueB Issue B description (25:0)\n"
            + "  - :warning: IssueD Issue D description (38:0)\n"
            + "  - :information_source: IssueF Issue F description (15:0)\n"
            + "\n");
    }

    private static Analysis analysis(final String tool, final List<? extends Issue> issues) {
        return new StubAnalysis(tool, issues, new MavenProject());
    }

    private static Issue issue(final Path file, final int line, final String name, final String description,
        final Issue.Severity severity) {
        return new StubIssue(file, line, 0, name, description, severity);
    }

}

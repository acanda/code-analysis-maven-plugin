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
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

class GitLabReportTest {

    @Test
    void createGitLabReport(@TempDir final Path tempDir) throws MojoFailureException, IOException {
        final Path baseDir = tempDir.resolve("baseDir");
        final Path targetDir = baseDir.resolve("target");
        Files.createDirectories(targetDir);
        final Path reportFile = targetDir.resolve("report.json");
        final Path javaMain = baseDir.resolve("src").resolve("main").resolve("java");
        final Path hello = javaMain.resolve("Hello.java");
        final Path world = javaMain.resolve("World.java");
        final GitLabReport report = new GitLabReport(baseDir,
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

        assertThatJson(Files.readString(reportFile, UTF_8)).isEqualTo(
            """
                [
                  {
                    "description": "ABC [IssueA]: Issue A description",
                    "fingerprint": "64ca83ea-1e88-3657-8323-a931cde1f4e8",
                    "location": {
                      "lines": {
                        "begin": 12
                      },
                      "path": "src/main/java/Hello.java"
                    },
                    "severity": "blocker"
                  },
                  {
                    "description": "ABC [IssueB]: Issue B description",
                    "fingerprint": "7559b8af-f4f6-3f4b-937a-95bbcd303490",
                    "location": {
                      "lines": {
                        "begin": 25
                      },
                      "path": "src/main/java/World.java"
                    },
                    "severity": "critical"
                  },
                  {
                    "description": "ABC [IssueC]: Issue C description",
                    "fingerprint": "b1e44652-eb31-3411-bdae-3b97e7f63060",
                    "location": {
                      "lines": {
                        "begin": 7
                      },
                      "path": "src/main/java/Hello.java"
                    },
                    "severity": "major"
                  },
                  {
                    "description": "ABC [IssueD]: Issue D description",
                    "fingerprint": "9a78c4ab-5f02-3c4e-849e-d41857736094",
                    "location": {
                      "lines": {
                        "begin": 38
                      },
                      "path": "src/main/java/World.java"
                    },
                    "severity": "minor"
                  },
                  {
                    "description": "ABC [IssueE]: Issue E description",
                    "fingerprint": "0cc3d491-2b87-3cd3-ab12-7863c1acef71",
                    "location": {
                      "lines": {
                        "begin": 24
                      },
                      "path": "src/main/java/Hello.java"
                    },
                    "severity": "minor"
                  },
                  {
                    "description": "ABC [IssueF]: Issue F description",
                    "fingerprint": "b3c9e744-694b-3ff7-abb8-422c9e36fff3",
                    "location": {
                      "lines": {
                        "begin": 15
                      },
                      "path": "src/main/java/World.java"
                    },
                    "severity": "info"
                  }
                ]""");
    }

    private static Inspection inspection(final String tool, final List<? extends Issue> issues) {
        return new StubInspection(tool, issues, new MavenProject());
    }

    private static Issue issue(final Path file, final int line, final String name, final String description,
        final Issue.Severity severity) {
        return new StubIssue(file, line, 0, name, description, severity);
    }

}

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
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

class GitLabReportTest {

    @Test
    void createGitLabReport(@TempDir final Path tempDir) throws MojoFailureException, IOException {
        final Path baseDir = tempDir.resolve("baseDir");
        final Path targetDir = baseDir.resolve("target");
        Files.createDirectories(targetDir);
        final Path reportFile = targetDir.resolve("report.json");
        final GitLabReport report = new GitLabReport(baseDir,
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

        assertThatJson(Files.readString(reportFile, UTF_8)).isEqualTo(
            "[\n"
            + "  {\n"
            + "    \"description\": \"ABC [IssueA]: Issue A description\",\n"
            + "    \"fingerprint\": \"64ca83ea-1e88-3657-8323-a931cde1f4e8\",\n"
            + "    \"location\": {\n"
            + "      \"lines\": {\n"
            + "        \"begin\": 12\n"
            + "      },\n"
            + "      \"path\": \"src/main/java/Hello.java\"\n"
            + "    },\n"
            + "    \"severity\": \"blocker\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"description\": \"ABC [IssueB]: Issue B description\",\n"
            + "    \"fingerprint\": \"7559b8af-f4f6-3f4b-937a-95bbcd303490\",\n"
            + "    \"location\": {\n"
            + "      \"lines\": {\n"
            + "        \"begin\": 25\n"
            + "      },\n"
            + "      \"path\": \"src/main/java/World.java\"\n"
            + "    },\n"
            + "    \"severity\": \"critical\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"description\": \"ABC [IssueC]: Issue C description\",\n"
            + "    \"fingerprint\": \"b1e44652-eb31-3411-bdae-3b97e7f63060\",\n"
            + "    \"location\": {\n"
            + "      \"lines\": {\n"
            + "        \"begin\": 7\n"
            + "      },\n"
            + "      \"path\": \"src/main/java/Hello.java\"\n"
            + "    },\n"
            + "    \"severity\": \"major\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"description\": \"ABC [IssueD]: Issue D description\",\n"
            + "    \"fingerprint\": \"9a78c4ab-5f02-3c4e-849e-d41857736094\",\n"
            + "    \"location\": {\n"
            + "      \"lines\": {\n"
            + "        \"begin\": 38\n"
            + "      },\n"
            + "      \"path\": \"src/main/java/World.java\"\n"
            + "    },\n"
            + "    \"severity\": \"minor\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"description\": \"ABC [IssueE]: Issue E description\",\n"
            + "    \"fingerprint\": \"0cc3d491-2b87-3cd3-ab12-7863c1acef71\",\n"
            + "    \"location\": {\n"
            + "      \"lines\": {\n"
            + "        \"begin\": 24\n"
            + "      },\n"
            + "      \"path\": \"src/main/java/Hello.java\"\n"
            + "    },\n"
            + "    \"severity\": \"minor\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"description\": \"ABC [IssueF]: Issue F description\",\n"
            + "    \"fingerprint\": \"b3c9e744-694b-3ff7-abb8-422c9e36fff3\",\n"
            + "    \"location\": {\n"
            + "      \"lines\": {\n"
            + "        \"begin\": 15\n"
            + "      },\n"
            + "      \"path\": \"src/main/java/World.java\"\n"
            + "    },\n"
            + "    \"severity\": \"info\"\n"
            + "  }\n"
            + "]");
    }

    private static Analysis analysis(final String tool, final List<? extends Issue> issues) {
        return new StubAnalysis(tool, issues, new MavenProject());
    }

    private static Issue issue(final Path file, final int line, final String name, final String description,
        final Issue.Severity severity) {
        return new StubIssue(file, line, 0, name, description, severity);
    }

}

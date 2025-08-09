package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Inspection;
import ch.acanda.maven.coan.Issue;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

/**
 * Test for the BitBucketReport class.
 * This test verifies that the report makes the REST requests to the BitBucket API.
 */
@Disabled("This does not yet work")
class BitBucketReportTest {

    @Test
    void testPublishToBitBucket(@TempDir final Path tempDir) throws MojoFailureException {

        final String workspace = "acanda";
        final String repoSlug = "repo";
        final String commit = "1234abcd";
        final String reportId = "code-analysis-maven-plugin";

        final WireMockServer server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        server.start();
        try {

            final String host = "http://localhost:" + server.port();
            System.out.println("WireMock server running at " + host);
            final String path = "/2.0/repositories/%s/%s/commit/%s/reports/%s"
                .formatted(workspace, repoSlug, commit, reportId);
            System.out.println("Request path: " + path);
            server.stubFor(put(path).willReturn(aResponse().withStatus(200)));

            final MavenProject project = new MavenProject();
            final Path baseDir = tempDir.resolve("baseDir");
            final Path javaMain = baseDir.resolve("src").resolve("main").resolve("java");
            final Path hello = javaMain.resolve("Hello.java");
            final Path world = javaMain.resolve("World.java");

            final BitBucketReport report = new BitBucketReport(project, baseDir,
                inspection("ABC", List.of(
                    issue(hello, 12, "IssueA", "Issue A description", Issue.Severity.HIGHEST),
                    issue(world, 25, "IssueB", "Issue B description", Issue.Severity.HIGH),
                    issue(hello, 7, "IssueC", "Issue C description", Issue.Severity.MEDIUM),
                    issue(world, 38, "IssueD", "Issue D description", Issue.Severity.LOW),
                    issue(hello, 24, "IssueE", "Issue E description", Issue.Severity.LOWEST),
                    issue(world, 15, "IssueF", "Issue F description", Issue.Severity.IGNORE)
                ))
            );

            final BitBucketPipeline pipeline =
                new BitBucketPipeline(workspace, repoSlug, commit, host, null);
            report.publishToBitBucket(pipeline);

            // Verify the request
            System.out.println("Verifying request to BitBucket API...");
            verify(putRequestedFor(urlPathMatching(path))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.title", equalTo("Code Analysis Report")))
                .withRequestBody(matchingJsonPath("$.details", equalTo("Code Analysis found 6 issues.")))
                .withRequestBody(matchingJsonPath("$.report_type", equalTo("BUG")))
                .withRequestBody(matchingJsonPath("$.reporter", equalTo("Code Analysis Maven Plugin")))
                .withRequestBody(matchingJsonPath("$.result", equalTo("FAILED"))));
            System.out.println("Verified request to BitBucket API.");

        } finally {
            server.checkForUnmatchedRequests();
            server.stop();
        }

    }

    private static Inspection inspection(final String tool, final List<? extends Issue> issues) {
        return new StubInspection(tool, issues, new MavenProject());
    }

    private static Issue issue(final Path file, final int line, final String name, final String description,
        final Issue.Severity severity) {
        return new StubIssue(file, line, 0, name, description, severity);
    }

}

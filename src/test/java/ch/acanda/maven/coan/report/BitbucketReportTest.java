package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Inspection;
import ch.acanda.maven.coan.Issue;
import ch.acanda.maven.coan.report.bitbucket.Pipeline;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

/**
 * Test for the BitBucketReport class.
 * This test verifies that the report makes the REST requests to the BitBucket API.
 */
class BitbucketReportTest {

    private static final boolean IGNORE_ARRAY_ORDER = true;
    private static final boolean FAIL_ON_EXTRA_ELEMENTS = false;

    private static final String WORKSPACE = "acanda";
    private static final String REPO_SLUG = "repo";
    private static final String COMMIT = "1234abcd";
    private static final String REPORT_ID = "code-analysis-maven-plugin";
    private static final String REPORTS_PATH =
        "/2.0/repositories/%s/%s/commit/%s/reports/%s".formatted(WORKSPACE, REPO_SLUG, COMMIT, REPORT_ID);
    private static final String ANNOTATIONS_PATH = REPORTS_PATH + "/annotations";

    @Test
    void testPublishToBitbucket(@TempDir final Path tempDir) throws MojoFailureException {
        final WireMockServer server = startMockServer();
        try {

            final Path baseDir = tempDir.resolve("baseDir");
            final Path javaMain = baseDir.resolve("src").resolve("main").resolve("java");
            final Path hello = javaMain.resolve("Hello.java");
            final Path world = javaMain.resolve("World.java");

            final BitBucketReport report = new BitBucketReport(baseDir,
                inspection("ABC", List.of(
                    issue(hello, 12, "IssueA", "Issue A description", Issue.Severity.HIGHEST),
                    issue(world, 25, "IssueB", "Issue B description", Issue.Severity.HIGH),
                    issue(hello, 7, "IssueC", "Issue C description", Issue.Severity.MEDIUM),
                    issue(world, 38, "IssueD", "Issue D description", Issue.Severity.LOW),
                    issue(hello, 24, "IssueE", "Issue E description", Issue.Severity.LOWEST),
                    issue(world, 15, "IssueF", "Issue F description", Issue.Severity.IGNORE)
                ))
            );

            report.publishToBitBucket(createPipeline(server));

            // Verify the report request
            verify(putRequestedFor(urlPathMatching(REPORTS_PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(
                    """
                    {
                      "reporter" : "Code Analysis Maven Plugin",
                      "report_type" : "BUG",
                      "title" : "Code Analysis Report",
                      "details" : "The Code Analysis Maven Plugin found 6 issues.",
                      "result" : "FAILED"
                    }
                    """
                )));

            // Verify the annotations request
            verify(postRequestedFor(urlPathMatching(ANNOTATIONS_PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(
                    """
                    [ {
                      "external_id" : "b10e88c3-8dc0-3fb1-a543-2e58a98ea8ba",
                      "title" : "IssueA",
                      "annotation_type" : "CODE_SMELL",
                      "summary" : "[ABC] Issue A description",
                      "severity" : "CRITICAL",
                      "path" : "src/main/java/Hello.java",
                      "line" : 12
                    }, {
                      "external_id" : "3ceab010-dbc7-3996-bd22-482072ec5b7a",
                      "title" : "IssueB",
                      "annotation_type" : "CODE_SMELL",
                      "summary" : "[ABC] Issue B description",
                      "severity" : "HIGH",
                      "path" : "src/main/java/World.java",
                      "line" : 25
                    }, {
                      "external_id" : "309c8925-3b57-37d2-afb2-cfd47115787e",
                      "title" : "IssueC",
                      "annotation_type" : "CODE_SMELL",
                      "summary" : "[ABC] Issue C description",
                      "severity" : "MEDIUM",
                      "path" : "src/main/java/Hello.java",
                      "line" : 7
                    }, {
                      "external_id" : "ff5d0a21-d405-303f-8c01-25623320146c",
                      "title" : "IssueD",
                      "annotation_type" : "CODE_SMELL",
                      "summary" : "[ABC] Issue D description",
                      "severity" : "LOW",
                      "path" : "src/main/java/World.java",
                      "line" : 38
                    }, {
                      "external_id" : "6ef4efda-1c36-397c-9da3-6e079cc3e9be",
                      "title" : "IssueE",
                      "annotation_type" : "CODE_SMELL",
                      "summary" : "[ABC] Issue E description",
                      "severity" : "LOW",
                      "path" : "src/main/java/Hello.java",
                      "line" : 24
                    }, {
                      "external_id" : "29075bcf-a655-3e93-87b7-1ff48b4f57c0",
                      "title" : "IssueF",
                      "annotation_type" : "CODE_SMELL",
                      "summary" : "[ABC] Issue F description",
                      "severity" : "LOW",
                      "path" : "src/main/java/World.java",
                      "line" : 15
                    } ]
                    """,
                    IGNORE_ARRAY_ORDER,
                    FAIL_ON_EXTRA_ELEMENTS
                )));

            server.checkForUnmatchedRequests();
        } finally {
            server.stop();
        }
    }

    @Test
    void shouldChunkAnnotationsWhenMoreThan100Issues(@TempDir final Path tempDir) throws Exception {
        final WireMockServer server = startMockServer();
        try {
            final List<Issue> issues = IntStream.range(0, 150)
                .mapToObj(i -> issue(tempDir.resolve("A.java"), i, "N", "D", Issue.Severity.MEDIUM))
                .toList();
            final BitBucketReport report = new BitBucketReport(tempDir, inspection("ABC", issues));
            report.publishToBitBucket(createPipeline(server));

            // Verify that createOrUpdateAnnotations() was called twice,
            // once with 100 annotations and once with 50 annotations.
            verify(postRequestedFor(urlPathMatching(ANNOTATIONS_PATH))
                .withRequestBody(matchingJsonPath("$.length()", equalTo("100"))));

            verify(postRequestedFor(urlPathMatching(ANNOTATIONS_PATH))
                .withRequestBody(matchingJsonPath("$.length()", equalTo("50"))));

            server.checkForUnmatchedRequests();
        } finally {
            server.stop();
        }
    }

    private static WireMockServer startMockServer() {
        final WireMockServer server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        server.start();
        WireMock.configureFor("localhost", server.port());
        server.stubFor(put(REPORTS_PATH).willReturn(aResponse().withStatus(200)));
        server.stubFor(post(ANNOTATIONS_PATH).willReturn(aResponse().withStatus(200)));
        return server;
    }

    private static Pipeline createPipeline(final WireMockServer server) {
        return new Pipeline(WORKSPACE, REPO_SLUG, COMMIT, "http://localhost:" + server.port(), null);
    }

    private static Inspection inspection(final String tool, final List<? extends Issue> issues) {
        return new StubInspection(tool, issues, new MavenProject());
    }

    private static Issue issue(final Path file, final int line, final String name, final String description,
        final Issue.Severity severity) {
        return new StubIssue(file, line, 0, name, description, severity);
    }

}

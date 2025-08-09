package ch.acanda.maven.coan;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.soebes.itf.extension.assertj.MavenProjectResultAssert;
import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.extension.SystemProperty;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;

import static ch.acanda.maven.coan.VersionsMojoTest.CHECKSTYLE_VERSION_PATTERN;
import static ch.acanda.maven.coan.VersionsMojoTest.PMD_VERSION_PATTERN;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.soebes.itf.extension.assertj.MavenExecutionResultAssert.assertThat;

@MavenJupiterExtension
@WireMockTest(httpPort = 29_418)
public class AnalyseMojoIT {

    @BeforeEach
    void beforeEach() {
        WireMock.stubFor(any(anyUrl()).willReturn(ok()));
    }

    @MavenTest
    @MavenGoal("verify")
    @SystemProperty(value = "GITHUB_STEP_SUMMARY", content = "target/summary.md")
    @SystemProperty(value = "BITBUCKET_REPO_OWNER", content = "acanda")
    @SystemProperty(value = "BITBUCKET_REPO_SLUG", content = "repo")
    @SystemProperty(value = "BITBUCKET_COMMIT", content = "bfc4fef9")
    public void analyseSucceedsWithoutIssues(final MavenExecutionResult project) {
        final MavenProjectResultAssert target = assertThat(project).isSuccessful().project().hasTarget();
        target.withFile("code-analysis/report.html").exists();
        target.withFile("code-analysis/report.gitlab.json").exists();
        target.withFile("code-analysis/report.github.md").exists();
        target.withFile("summary.md").exists();
        assertThat(project).out().info()
            .describedAs("Contains PMD version matching pattern \"" + PMD_VERSION_PATTERN + "\"")
            .anyMatch(line -> line.matches(PMD_VERSION_PATTERN));
        assertThat(project).out().info()
            .describedAs("Contains Checkstyle version matching pattern \"" + CHECKSTYLE_VERSION_PATTERN + "\"")
            .anyMatch(line -> line.matches(CHECKSTYLE_VERSION_PATTERN));
        assertThat(project).out().info().contains("PMD did not find any issues in analyse.");
        assertThat(project).out().info().contains("Checkstyle did not find any issues in analyse.");
        verify(putRequestedFor(
            urlEqualTo("/2.0/repositories/acanda/repo/commit/bfc4fef9/reports/code-analysis-maven-plugin")
        ));
    }

    @MavenTest
    @MavenGoal("verify")
    @SystemProperty(value = "GITHUB_STEP_SUMMARY", content = "target/summary.md")
    @SystemProperty(value = "BITBUCKET_REPO_OWNER", content = "acanda")
    @SystemProperty(value = "BITBUCKET_REPO_SLUG", content = "repo")
    @SystemProperty(value = "BITBUCKET_COMMIT", content = "bfc4fef9")
    public void analyseFailsWithIssues(final MavenExecutionResult project) {
        final MavenProjectResultAssert target = assertThat(project).isFailure().project().hasTarget();
        target.withFile("code-analysis/report.html").exists();
        target.withFile("code-analysis/report.gitlab.json").exists();
        target.withFile("code-analysis/report.github.md").exists();
        target.withFile("summary.md").exists();
        assertThat(project).out().info()
            .describedAs("Contains PMD version matching pattern \"" + PMD_VERSION_PATTERN + "\"")
            .anyMatch(line -> line.matches(PMD_VERSION_PATTERN));
        assertThat(project).out().info()
            .describedAs("Contains Checkstyle version matching pattern \"" + CHECKSTYLE_VERSION_PATTERN + "\"")
            .anyMatch(line -> line.matches(CHECKSTYLE_VERSION_PATTERN));
        assertThat(project).out().warn().containsSubsequence(
            "PMD found 1 issue in analyse:",
            getPathForJavaSource("Hello.java"),
            " [ExtendsObject] No need to explicitly extend Object. (Hello.java:1)"
        );
        assertThat(project).out().warn().containsSubsequence(
            "Checkstyle found 2 issues in analyse:",
            getPathForJavaSource("Hello.java"),
            " [LineLengthCheck] Line is longer than 35 characters, found 51. (Hello.java:2)",
            " [LineLengthCheck] Line is longer than 35 characters, found 36. (Hello.java:3)"
        );
        verify(putRequestedFor(
            urlEqualTo("/2.0/repositories/acanda/repo/commit/bfc4fef9/reports/code-analysis-maven-plugin")
        ));
        verify(postRequestedFor(
            urlEqualTo("/2.0/repositories/acanda/repo/commit/bfc4fef9/reports/code-analysis-maven-plugin/annotations")
        ));
    }

    private static String getPathForJavaSource(final String filename) {
        return String.join(File.separator, "src", "main", "java", filename);
    }

}

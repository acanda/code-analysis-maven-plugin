package ch.acanda.maven.coan;

import com.soebes.itf.extension.assertj.MavenProjectResultAssert;
import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.extension.SystemProperty;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;

import java.io.File;

import static ch.acanda.maven.coan.VersionsMojoTest.CHECKSTYLE_VERSION_PATTERN;
import static ch.acanda.maven.coan.VersionsMojoTest.PMD_VERSION_PATTERN;
import static com.soebes.itf.extension.assertj.MavenExecutionResultAssert.assertThat;

@MavenJupiterExtension
public class AnalyseMojoIT {

    @MavenTest
    @MavenGoal("verify")
    @SystemProperty(value = "GITHUB_STEP_SUMMARY", content = "target/summary.md")
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
    }

    @MavenTest
    @MavenGoal("verify")
    @SystemProperty(value = "GITHUB_STEP_SUMMARY", content = "target/summary.md")
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
    }

    private static String getPathForJavaSource(final String filename) {
        return String.join(File.separator, "src", "main", "java", filename);
    }

}

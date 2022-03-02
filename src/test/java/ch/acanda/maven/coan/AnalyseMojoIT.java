package ch.acanda.maven.coan;

import com.soebes.itf.extension.assertj.MavenProjectResultAssert;
import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;

import java.io.File;

import static com.soebes.itf.extension.assertj.MavenExecutionResultAssert.assertThat;

@MavenJupiterExtension
public class AnalyseMojoIT {

    @MavenTest
    @MavenGoal("verify")
    public void analyseSucceedsWithoutIssues(final MavenExecutionResult project) {
        final MavenProjectResultAssert target = assertThat(project).isSuccessful().project().hasTarget();
        target.withFile("code-analysis/report.html").exists();
        target.withFile("code-analysis/report.gitlab.json").exists();
        assertThat(project).out().info().anyMatch(line -> line.matches("PMD \\d+\\.\\d+\\.\\d+"));
        assertThat(project).out().info().anyMatch(line -> line.matches("Checkstyle \\d+\\.\\d+(\\.\\d+)?"));
        assertThat(project).out().info().contains("PMD did not find any issues in analyse.");
        assertThat(project).out().info().contains("Checkstyle did not find any issues in analyse.");
    }

    @MavenTest
    @MavenGoal("verify")
    public void analyseFailsWithIssues(final MavenExecutionResult project) {
        final MavenProjectResultAssert target = assertThat(project).isFailure()
            .project().hasTarget();
        target.withFile("code-analysis/report.html").exists();
        target.withFile("code-analysis/report.gitlab.json").exists();
        assertThat(project).out().info().anyMatch(line -> line.matches("PMD \\d+\\.\\d+\\.\\d+"));
        assertThat(project).out().info().anyMatch(line -> line.matches("Checkstyle \\d+\\.\\d+(\\.\\d+)?"));
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

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
        assertThat(project).out().info().contains("PMD did not find any issues.");
        assertThat(project).out().info().contains("Checkstyle did not find any issues.");
    }

    @MavenTest
    @MavenGoal("verify")
    public void analyseFailsWithIssues(final MavenExecutionResult project) {
        final MavenProjectResultAssert target = assertThat(project).isFailure()
                .project().hasTarget();
        target.withFile("code-analysis/report.html").exists();
        assertThat(project).out().warn().containsSubsequence(
                "PMD found 1 issue:",
                "src" + File.separator + "main" + File.separator + "java" + File.separator + "Hello.java",
                " [ExtendsObject] No need to explicitly extend Object. (Hello.java:1)"
        );
        assertThat(project).out().warn().containsSubsequence(
                "Checkstyle found 2 issues:",
                "src" + File.separator + "main" + File.separator + "java" + File.separator + "Hello.java",
                " [LineLengthCheck] Line is longer than 35 characters (found 51). (Hello.java:2)",
                " [LineLengthCheck] Line is longer than 35 characters (found 36). (Hello.java:3)"
        );
    }

}

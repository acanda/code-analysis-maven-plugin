package ch.acanda.maven.coan;

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
        assertThat(project).isSuccessful()
                .project().hasTarget().withFile("code-analysis/pmd-report.html").exists();
        assertThat(project).out().info().contains("PMD did not find any issues.");
    }

    @MavenTest
    @MavenGoal("verify")
    public void analyseFailsWithIssues(final MavenExecutionResult project) {
        assertThat(project).isFailure()
                .project().hasTarget().withFile("code-analysis/pmd-report.html").exists();
        assertThat(project).out().warn().containsSubsequence(
                "PMD found 1 issue:",
                "src" + File.separator + "main" + File.separator + "java" + File.separator + "Hello.java",
                " [ExtendsObject] No need to explicitly extend Object. (Hello.java:1)"
        );
    }

}

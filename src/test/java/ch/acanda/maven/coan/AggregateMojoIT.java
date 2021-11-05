package ch.acanda.maven.coan;

import com.soebes.itf.extension.assertj.MavenProjectResultAssert;
import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.extension.SystemProperty;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;
import com.soebes.itf.jupiter.maven.MavenProjectResult;
import org.assertj.core.api.Condition;

import java.io.File;

import static com.soebes.itf.extension.assertj.MavenExecutionResultAssert.assertThat;

@MavenJupiterExtension
public class AggregateMojoIT {

    @MavenTest
    @MavenGoal("ch.acanda.maven:code-analysis-maven-plugin:aggregate")
    @SystemProperty(value = "coan.report.formats", content = "html,gitlab")
    public void aggregateSucceedsWithoutIssues(final MavenExecutionResult project) {
        final MavenProjectResultAssert target = assertThat(project).isSuccessful().project().hasTarget();
        target.withFile("code-analysis/report.html").exists();
        target.withFile("code-analysis/report.gitlab.json").exists();
        assertThat(project).out().info().anyMatch(line -> line.matches("PMD \\d+\\.\\d+\\.\\d+"));
        assertThat(project).out().info().anyMatch(line -> line.matches("Checkstyle \\d+\\.\\d+(\\.\\d+)?"));
        assertThat(project).out().info().contains("PMD did not find any issues in aggregate.");
        assertThat(project).out().info().contains("Checkstyle did not find any issues in aggregate.");
        assertThat(project).out().info().contains("PMD did not find any issues in aggregate-module-1.");
        assertThat(project).out().info().contains("Checkstyle did not find any issues in aggregate-module-1.");
        assertThat(project).out().info().contains("PMD did not find any issues in aggregate-module-2.");
        assertThat(project).out().info().contains("Checkstyle did not find any issues in aggregate-module-2.");
        assertThat(project).project().withModule("module1").doesNotHave(aTargetDirectory());
        assertThat(project).project().withModule("module2").doesNotHave(aTargetDirectory());
    }

    @MavenTest
    @MavenGoal("ch.acanda.maven:code-analysis-maven-plugin:aggregate")
    @SystemProperty(value = "coan.report.formats", content = "html,gitlab,xyz")
    public void aggregateFailsWithIssues(final MavenExecutionResult project) {
        final MavenProjectResultAssert target = assertThat(project).isFailure()
            .project().hasTarget();
        target.withFile("code-analysis/report.html").exists();
        target.withFile("code-analysis/report.gitlab.json").exists();
        assertThat(project).out().info().anyMatch(line -> line.matches("PMD \\d+\\.\\d+\\.\\d+"));
        assertThat(project).out().info().anyMatch(line -> line.matches("Checkstyle \\d+\\.\\d+(\\.\\d+)?"));
        assertThat(project).out().info().contains("PMD did not find any issues in aggregate.");
        assertThat(project).out().info().contains("Checkstyle did not find any issues in aggregate.");
        assertThat(project).out().warn().containsSubsequence(
            "PMD found 1 issue in aggregate-module-1:",
            getPathForJavaSource("module1", "Hello.java"),
            " [ExtendsObject] No need to explicitly extend Object. (Hello.java:1)"
        );
        assertThat(project).out().warn().containsSubsequence(
            "Checkstyle found 2 issues in aggregate-module-1:",
            getPathForJavaSource("module1", "Hello.java"),
            " [LineLengthCheck] Line is longer than 35 characters (found 51). (Hello.java:2)",
            " [LineLengthCheck] Line is longer than 35 characters (found 36). (Hello.java:3)"
        );
        assertThat(project).out().warn().containsSubsequence(
            "PMD found 1 issue in aggregate-module-2:",
            getPathForJavaSource("module2", "World.java"),
            " [ExtendsObject] No need to explicitly extend Object. (World.java:1)"
        );
        assertThat(project).out().warn().containsSubsequence(
            "Checkstyle found 2 issues in aggregate-module-2:",
            getPathForJavaSource("module2", "World.java"),
            " [LineLengthCheck] Line is longer than 35 characters (found 51). (World.java:2)",
            " [LineLengthCheck] Line is longer than 35 characters (found 36). (World.java:3)"
        );
        assertThat(project).out().warn().contains("The following report formats are invalid and are ignored: xyz");
    }

    private static Condition<MavenProjectResult> aTargetDirectory() {
        return new Condition<>(p -> new File(p.getTargetProjectDirectory(), "target").exists(), "a target");
    }

    private static String getPathForJavaSource(final String module, final String filename) {
        return String.join(File.separator, module, "src", "main", "java", filename);
    }

}

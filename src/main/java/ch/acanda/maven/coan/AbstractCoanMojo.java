package ch.acanda.maven.coan;

import ch.acanda.maven.coan.checkstyle.CheckstyleConfig;
import ch.acanda.maven.coan.pmd.PmdConfig;
import ch.acanda.maven.coan.report.GitHubReport;
import ch.acanda.maven.coan.report.GitLabReport;
import ch.acanda.maven.coan.report.HtmlReport;
import ch.acanda.maven.coan.version.Versions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

abstract class AbstractCoanMojo extends AbstractMojo {

    private static final String REPORT_FORMAT_HTML = "html";
    private static final String REPORT_FORMAT_GITLAB = "gitlab";
    private static final String REPORT_FORMAT_GITHUB = "github";
    private static final String DEFAULT_SKIP = "false";
    private static final String DEFAULT_FAIL_ON_ISSUES = "true";
    private static final String DEFAULT_TARGET_PATH = "${project.build.directory}/code-analysis";
    private static final String DEFAULT_PMD_CONFIG_PATH = "config/pmd.xml";
    private static final String DEFAULT_CHECKSTYLE_CONFIG_PATH = "config/checkstyle.xml";
    private static final String DEFAULT_REPORT_FORMATS = REPORT_FORMAT_HTML;

    @Parameter(defaultValue = "${project}")
    @Getter(AccessLevel.PROTECTED)
    private MavenProject project;

    @Parameter(property = "coan.skip", required = true, defaultValue = DEFAULT_SKIP)
    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private boolean skip;

    @Parameter(property = "coan.failOnIssues", required = true, defaultValue = DEFAULT_FAIL_ON_ISSUES)
    @Getter(AccessLevel.PROTECTED)
    private boolean failOnIssues;

    @Parameter(property = "coan.targetPath", required = true, defaultValue = DEFAULT_TARGET_PATH)
    @Getter(AccessLevel.PROTECTED)
    private String targetPath;

    @Parameter(property = "coan.pmd.configPath", required = true, defaultValue = DEFAULT_PMD_CONFIG_PATH)
    @Getter(AccessLevel.PROTECTED)
    private String pmdConfigPath;

    @Parameter(property = "coan.checkstyle.configPath", required = true, defaultValue = DEFAULT_CHECKSTYLE_CONFIG_PATH)
    @Getter(AccessLevel.PROTECTED)
    private String checkstyleConfigPath;

    @Parameter(property = "coan.report.formats", required = true, defaultValue = DEFAULT_REPORT_FORMATS)
    @Getter(AccessLevel.PROTECTED)
    private Set<String> reportFormats;

    @Override
    public final void execute() throws MojoFailureException {
        if (skip) {
            getLog().info("Skipping code analysis");
            return;
        }
        getLog().info("PMD " + Versions.getPmdVersion());
        getLog().info("Checkstyle " + Versions.getCheckstyleVersion());
        analyseCode();
    }

    protected abstract void analyseCode() throws MojoFailureException;

    protected PmdConfig assemblePmdConfig(final MavenProject project) {
        return new PmdConfig(project, getLog(), getPmdConfigPath(), getTargetPath());
    }

    protected CheckstyleConfig assembleCheckstyleConfig(final MavenProject project) {
        return new CheckstyleConfig(project, getLog(), getCheckstyleConfigPath(), getTargetPath());
    }

    protected void createReports(final Inspection... analyses) throws MojoFailureException {
        final Path baseDir = getProject().getBasedir().toPath();
        final Path targetDir = Paths.get(getTargetPath());
        final String gitHubStepSummary = getGithubStepSummary();
        if (gitHubStepSummary != null) {
            final GitHubReport report = new GitHubReport(getProject(), baseDir, analyses);
            final Path reportFile = Paths.get(gitHubStepSummary);
            report.appendTo(reportFile);
            getLog().info("The GitHub Code Quality report was appended to the GitHub step summary file " + reportFile);
        }
        if (reportFormats.remove(REPORT_FORMAT_HTML)) {
            final HtmlReport report = new HtmlReport(getProject(), baseDir, analyses);
            final Path reportFile = targetDir.resolve("report.html");
            report.writeTo(reportFile);
            getLog().info("The HTML report is available at " + reportFile);
        }
        if (reportFormats.remove(REPORT_FORMAT_GITLAB)) {
            final GitLabReport report = new GitLabReport(baseDir, analyses);
            final Path reportFile = targetDir.resolve("report.gitlab.json");
            report.writeTo(reportFile);
            getLog().info("The GitLab Code Quality report is available at " + reportFile);
        }
        if (reportFormats.remove(REPORT_FORMAT_GITHUB)) {
            final GitHubReport report = new GitHubReport(getProject(), baseDir, analyses);
            final Path reportFile = targetDir.resolve("report.github.md");
            report.writeTo(reportFile);
            getLog().info("The GitHub Code Quality report is available at " + reportFile);
        }
        if (!reportFormats.isEmpty()) {
            final String invalidFormats = String.join(", ", reportFormats);
            getLog().warn("The following report formats are invalid and are ignored: " + invalidFormats);
        }
    }

    private String getGithubStepSummary() {
        final String property = System.getProperty("GITHUB_STEP_SUMMARY");
        return property != null ? property : System.getenv("GITHUB_STEP_SUMMARY");
    }

}

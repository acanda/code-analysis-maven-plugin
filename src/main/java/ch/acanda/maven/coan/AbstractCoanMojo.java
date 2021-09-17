package ch.acanda.maven.coan;

import ch.acanda.maven.coan.checkstyle.CheckstyleConfig;
import ch.acanda.maven.coan.pmd.PmdConfig;
import ch.acanda.maven.coan.report.HtmlReport;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;

abstract class AbstractCoanMojo extends AbstractMojo {

    private static final String DEFAULT_FAIL_ON_ISSUES = "true";
    private static final String DEFAULT_TARGET_PATH = "${project.build.directory}/code-analysis";
    private static final String DEFAULT_PMD_CONFIG_PATH = "config/pmd.xml";
    private static final String DEFAULT_CHECKSTYLE_CONFIG_PATH = "config/checkstyle.xml";

    @Parameter(defaultValue = "${project}")
    @Getter(AccessLevel.PROTECTED)
    private MavenProject project;

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

    protected PmdConfig assemblePmdConfig(final MavenProject project) {
        return PmdConfig.builder()
            .project(project)
            .log(getLog())
            .configPath(getPmdConfigPath())
            .targetPath(getTargetPath())
            .build();
    }

    protected CheckstyleConfig assembleCheckstyleConfig(final MavenProject project) {
        return CheckstyleConfig.builder()
            .project(project)
            .log(getLog())
            .configPath(getCheckstyleConfigPath())
            .targetPath(getTargetPath())
            .build();
    }

    protected void createHtmlReport(final Analysis... analyses) throws MojoFailureException {
        final HtmlReport report =
            new HtmlReport(getProject().getArtifact(), getProject().getBasedir().toPath(), analyses);
        final Path reportFile = Paths.get(getTargetPath()).resolve("report.html");
        report.writeTo(reportFile);
        getLog().info("The HTML report is available at " + reportFile);
    }

}

package ch.acanda.maven.coan;

import ch.acanda.maven.coan.checkstyle.CheckstyleAnalyser;
import ch.acanda.maven.coan.checkstyle.CheckstyleConfig;
import ch.acanda.maven.coan.pmd.PmdAnalyser;
import ch.acanda.maven.coan.pmd.PmdConfig;
import ch.acanda.maven.coan.report.HtmlReport;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.Locale.ENGLISH;

@Mojo(name = "analyse", defaultPhase = LifecyclePhase.VERIFY)
public class AnalyseMojo extends AbstractMojo {

    private static final String DEFAULT_FAIL_ON_ISSUES = "true";
    private static final String DEFAULT_TARGET_PATH = "${project.build.directory}/code-analysis";
    private static final String DEFAULT_PMD_CONFIG_PATH = "config/pmd.xml";
    private static final String DEFAULT_CHECKSTYLE_CONFIG_PATH = "config/checkstyle.xml";

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "coan.failOnIssues", required = true, defaultValue = DEFAULT_FAIL_ON_ISSUES)
    private boolean failOnIssues;

    @Parameter(property = "coan.targetPath", required = true, defaultValue = DEFAULT_TARGET_PATH)
    private String targetPath;

    @Parameter(property = "coan.pmd.configPath", required = true, defaultValue = DEFAULT_PMD_CONFIG_PATH)
    private String pmdConfigPath;

    @Parameter(property = "coan.checkstyle.configPath", required = true, defaultValue = DEFAULT_CHECKSTYLE_CONFIG_PATH)
    private String checkstyleConfigPath;

    @Override
    public void execute() throws MojoFailureException {
        final PmdAnalyser pmdAnalyser = new PmdAnalyser(assemblePmdConfig());
        final Analysis pmdAnalysis = pmdAnalyser.analyse();
        report(pmdAnalysis);
        final CheckstyleAnalyser checkstyleAnalyser = new CheckstyleAnalyser(assembleCheckstyleConfig());
        final Analysis checkstyleAnalysis = checkstyleAnalyser.analyse();
        report(checkstyleAnalysis);
        createHtmlReport(pmdAnalysis, checkstyleAnalysis);
        if (failOnIssues && (pmdAnalysis.foundIssues() || checkstyleAnalysis.foundIssues())) {
            throw new MojoFailureException("Code analysis found " + numberOfToolIssues(pmdAnalysis) + ".");
        }
    }

    private void createHtmlReport(final Analysis... analyses) throws MojoFailureException {
        final HtmlReport report = new HtmlReport(project.getArtifact(), project.getBasedir().toPath(), analyses);
        final Path reportFile = Paths.get(targetPath).resolve("report.html");
        getLog().info("Start writing report to " + reportFile);
        report.writeTo(reportFile);
        getLog().info("Finished writing report to " + reportFile);
    }

    private PmdConfig assemblePmdConfig() {
        return PmdConfig.builder()
                .project(project)
                .log(getLog())
                .configPath(pmdConfigPath)
                .targetPath(targetPath)
                .build();
    }

    private CheckstyleConfig assembleCheckstyleConfig() {
        return CheckstyleConfig.builder()
                .project(project)
                .log(getLog())
                .configPath(checkstyleConfigPath)
                .targetPath(targetPath)
                .build();
    }

    private void report(final Analysis analysis) {
        if (analysis.foundIssues()) {
            final List<? extends Issue> issues = analysis.getIssues();
            final var summaryTemplate = "%s found %s:";
            getLog().warn(String.format(ENGLISH, summaryTemplate, analysis.getToolName(), numberOfIssues(analysis)));
            issues.stream()
                    .collect(Collectors.groupingBy(Issue::getFile))
                    .forEach((file, fileIssues) -> {
                        getLog().warn(project.getBasedir().toPath().relativize(file).toString());
                        fileIssues.stream()
                                .sorted(comparing(Issue::getName).thenComparing(Issue::getLine))
                                .map(this::formatIssue)
                                .forEach(getLog()::warn);
                    });
        } else {
            getLog().info(analysis.getToolName() + " did not find any issues.");
        }
    }

    private String formatIssue(final Issue issue) {
        final var issueTemplate = " [%s] %s (%s:%d)";
        final Path fileName = issue.getFile().getFileName();
        final int line = issue.getLine();
        final String name = issue.getName();
        // Ensure that the description ends in a period. We need the period
        // because IntelliJ only creates links if it finds the pattern
        // ". (fileName:line)" in the console.
        final String description = ensurePeriod(issue.getDescription());
        return String.format(ENGLISH, issueTemplate, name, description, fileName, line);
    }

    private String ensurePeriod(final String s) {
        return s.endsWith(".") ? s : s + ".";
    }

    private String numberOfToolIssues(final Analysis analysis) {
        final int count = analysis.getIssues().size();
        final String noun = count == 1 ? "issue" : "issues";
        return count + " " + analysis.getToolName() + " " + noun;
    }

    private String numberOfIssues(final Analysis analysis) {
        final int count = analysis.getIssues().size();
        final String noun = count == 1 ? " issue" : " issues";
        return count + noun;
    }

}

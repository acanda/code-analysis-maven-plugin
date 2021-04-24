package ch.acanda.maven.coan;

import ch.acanda.maven.coan.pmd.PmdAnalyser;
import ch.acanda.maven.coan.pmd.PmdConfig;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.Locale.ENGLISH;

@Mojo(name = "analyse", defaultPhase = LifecyclePhase.VERIFY)
public class AnalyseMojo extends AbstractMojo {

    private static final String DEFAULT_TARGET_PATH = "${project.build.directory}/code-analysis";
    private static final String DEFAULT_PMD_CONFIG_PATH = "config/pmd.xml";
    private static final String DEFAULT_FAIL_ON_ISSUES = "true";

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "coan.failOnIssues", required = true, defaultValue = DEFAULT_FAIL_ON_ISSUES)
    private boolean failOnIssues;

    @Parameter(property = "coan.pmd.configPath", required = true, defaultValue = DEFAULT_PMD_CONFIG_PATH)
    private String pmdConfigPath;

    @Parameter(property = "coan.targetPath", required = true, defaultValue = DEFAULT_TARGET_PATH)
    private String targetPath;

    @Override
    public void execute() throws MojoFailureException {
        final PmdAnalyser pmdAnalyser = new PmdAnalyser(assemblePmdConfig());
        final Analysis pmdAnalysis = pmdAnalyser.analyse();
        report(pmdAnalysis);
        if (failOnIssues && pmdAnalysis.foundIssues()) {
            throw new MojoFailureException("Code analysis found " + numberOfToolIssues(pmdAnalysis) + ".");
        }
    }

    private PmdConfig assemblePmdConfig() {
        return PmdConfig.builder()
                .project(project)
                .log(getLog())
                .configPath(pmdConfigPath)
                .targetPath(targetPath)
                .build();
    }

    private void report(final Analysis analysis) {
        if (analysis.foundIssues()) {
            final List<Issue> issues = analysis.getIssues();
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

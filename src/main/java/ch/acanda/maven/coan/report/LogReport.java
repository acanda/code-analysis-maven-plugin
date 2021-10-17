package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Analysis;
import ch.acanda.maven.coan.Issue;
import lombok.experimental.UtilityClass;
import org.apache.maven.plugin.logging.Log;

import java.nio.file.Path;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.Locale.ENGLISH;

@UtilityClass
public class LogReport {

    /**
     * Appends the results of an analysis to the Maven log.
     *
     * @param baseDir
     *     The path of files with issues will be shown relative to this directory.
     *     For projects without modules this should be the baseDir of the project.
     *     For multi-module projects this should be the baseDir of the root project.
     */
    public static void report(final Analysis analysis, final Path baseDir, final Log log) {
        final String artifactId = analysis.getProject().getArtifactId();
        if (analysis.foundIssues()) {
            final var summary = "%s found %s in %s:";
            log.warn(format(ENGLISH, summary, analysis.getToolName(), numberOfIssues(analysis), artifactId));
            analysis.getIssues().stream()
                .collect(Collectors.groupingBy(Issue::getFile))
                .forEach((file, fileIssues) -> {
                    log.warn(baseDir.relativize(file).toString());
                    fileIssues.stream()
                        .sorted(comparing(Issue::getName).thenComparing(Issue::getLine))
                        .map(LogReport::formatIssue)
                        .forEach(log::warn);
                });
        } else {
            log.info(analysis.getToolName() + " did not find any issues in " + artifactId + ".");
        }
    }

    private static String formatIssue(final Issue issue) {
        final var issueTemplate = " [%s] %s (%s:%d)";
        final Path fileName = issue.getFile().getFileName();
        final int line = issue.getLine();
        final String name = issue.getName();
        // Ensure that the description ends in a period. We need the period
        // because IntelliJ only creates links if it finds the pattern
        // ". (fileName:line)" in the console.
        final String description = ensurePeriod(issue.getDescription());
        return format(ENGLISH, issueTemplate, name, description, fileName, line);
    }

    private static String ensurePeriod(final String s) {
        return s.endsWith(".") ? s : s + ".";
    }

    private static String numberOfIssues(final Analysis analysis) {
        final int count = analysis.getNumberOfIssues();
        final String noun = count == 1 ? " issue" : " issues";
        return count + noun;
    }

}

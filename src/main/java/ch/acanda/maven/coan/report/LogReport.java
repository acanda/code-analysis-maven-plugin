package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Inspection;
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
    public static void report(final Inspection inspection, final Path baseDir, final Log log) {
        final String artifactId = inspection.project().getArtifactId();
        if (inspection.foundIssues()) {
            final String summary = "%s found %s in %s:";
            log.warn(format(ENGLISH, summary, inspection.toolName(), numberOfIssues(inspection), artifactId));
            inspection.issues().stream()
                .collect(Collectors.groupingBy(Issue::file))
                .forEach((file, fileIssues) -> {
                    log.warn(baseDir.relativize(file).toString());
                    fileIssues.stream()
                        .sorted(comparing(Issue::name).thenComparing(Issue::line))
                        .map(LogReport::formatIssue)
                        .forEach(log::warn);
                });
        } else {
            log.info(inspection.toolName() + " did not find any issues in " + artifactId + ".");
        }
    }

    private static String formatIssue(final Issue issue) {
        final String issueTemplate = " [%s] %s (%s:%d)";
        final Path fileName = issue.file().getFileName();
        final int line = issue.line();
        final String name = issue.name();
        // Ensure that the description ends in a period and does not contain
        // parentheses. We need the period because IntelliJ only creates links
        // if it finds the pattern ". (fileName:line)" in the console.
        final String description = ensurePeriod(issue.description())
            .replaceAll("\\s*+\\(", ", ")
            .replaceAll("\\)", "");
        return format(ENGLISH, issueTemplate, name, description, fileName, line);
    }

    private static String ensurePeriod(final String s) {
        return s.endsWith(".") ? s : s + ".";
    }

    private static String numberOfIssues(final Inspection inspection) {
        final int count = inspection.getNumberOfIssues();
        final String noun = count == 1 ? " issue" : " issues";
        return count + noun;
    }

}

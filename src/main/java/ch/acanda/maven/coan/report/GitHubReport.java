package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Inspection;
import ch.acanda.maven.coan.Issue;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.TreeMap;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summarizingLong;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

@SuppressWarnings("java:S1192" /* duplicated strings: creating constants for html tags makes the code less readable. */)
public class GitHubReport {

    private final MavenProject project;
    private final Path baseDir;
    private final List<Inspection> analyses;

    public GitHubReport(final MavenProject project, final Path baseDir, final Inspection... analyses) {
        this.project = project;
        this.baseDir = baseDir;
        this.analyses = Arrays.asList(analyses);
    }

    public void writeTo(final Path file) throws MojoFailureException {
        try (Writer out = Files.newBufferedWriter(file)) {
            writeTo(out);
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to write GitHub report to file " + file + ".", e);
        }
    }

    public void appendTo(final Path file) throws MojoFailureException {
        try (Writer out = Files.newBufferedWriter(file, CREATE, WRITE, APPEND)) {
            writeTo(out);
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to append GitHub report to file " + file + ".", e);
        }
    }

    private void writeTo(final Writer out) throws IOException {
        final PrintWriter markdown = new PrintWriter(out);
        writeBody(markdown);
        markdown.flush();
        if (markdown.checkError()) {
            throw new IOException("Failed to create HTML report.");
        }
    }

    private void writeBody(final PrintWriter markdown) {
        markdown.print("# Code Analysis for ");
        markdown.print(escapeHtml4(getProjectName(project)));
        final String version = project.getVersion();
        if (version != null) {
            markdown.print(" ");
            markdown.print(version);
        }
        markdown.println();
        markdown.println();
        writeSummary(markdown);
        writeAnalyses(markdown);
    }

    private void writeSummary(final PrintWriter markdown) {
        final boolean foundIssues = analyses.stream().anyMatch(Inspection::foundIssues);
        if (foundIssues) {
            markdown.println("## Summary");
            markdown.println();
            analyses.stream()
                .collect(groupingBy(Inspection::toolName, summarizingLong(Inspection::getNumberOfIssues)))
                .entrySet()
                .stream()
                .map(entry -> escapeHtml4(entry.getKey()) + " found " + numberOfIssues(entry.getValue()) + ".")
                .sorted()
                .forEachOrdered(toolSummary -> {
                    markdown.println(toolSummary);
                    markdown.println();
                });
        } else {
            markdown.println("## Congratulations!");
            markdown.println();
            markdown.println("The code analysers did not find any issues.");
            markdown.println();
        }
    }

    private void writeAnalyses(final PrintWriter markdown) {
        final Map<MavenProject, List<Inspection>> analysesByProject = analyses.stream()
            .filter(Inspection::foundIssues)
            .collect(groupingBy(Inspection::project));
        final boolean includeProjectName = analysesByProject.size() > 1;
        analysesByProject
            .entrySet()
            .stream()
            .sorted(comparing(e -> getProjectName(e.getKey())))
            .forEachOrdered(entry -> writeAnalyses(entry.getKey(), entry.getValue(), includeProjectName, markdown));
    }

    private void writeAnalyses(final MavenProject project, final List<Inspection> analyses,
        final boolean includeProjectName, final PrintWriter markdown) {
        if (includeProjectName) {
            markdown.print("## ");
            markdown.println(escapeHtml4(getProjectName(project)));
            markdown.println();
        }
        analyses.forEach(analysis -> writeAnalysis(analysis, includeProjectName, markdown));
    }

    private void writeAnalysis(final Inspection inspection, final boolean isLevel3, final PrintWriter markdown) {
        markdown.print(isLevel3 ? "### " : "## ");
        markdown.print(escapeHtml4(inspection.toolName()));
        markdown.println(" Report");
        markdown.println();
        inspection.issues()
            .stream()
            .collect(groupingBy(Issue::file, TreeMap::new, toList()))
            .forEach((file, issues) -> writeIssues(file, issues, markdown));
    }

    private void writeIssues(final Path file, final List<? extends Issue> issues, final PrintWriter markdown) {
        markdown.print("- ");
        markdown.println(escapeHtml4(baseDir.relativize(file).toString().replace('\\', '/')));
        issues.stream()
            .sorted(comparing(Issue::severity)
                .thenComparing(Issue::name)
                .thenComparing(Issue::line)
                .thenComparing(Issue::column))
            .forEachOrdered(issue -> writeIssue(issue, markdown));
        markdown.println();
    }

    private static String getProjectName(final MavenProject project) {
        final String name = project.getName();
        return name == null ? project.getArtifactId() : name;
    }

    private static String numberOfIssues(final LongSummaryStatistics summary) {
        final long sum = summary.getSum();
        final String noun = sum == 1 ? " issue" : " issues";
        return sum + noun;
    }

    private static void writeIssue(final Issue issue, final PrintWriter markdown) {
        markdown.print("  - ");
        markdown.print(getEmoji(issue.severity()));
        markdown.print(" ");
        markdown.print(escapeHtml4(issue.name()));
        markdown.print(" ");
        markdown.print(escapeHtml4(issue.description()));
        markdown.print(" (");
        markdown.print(issue.line());
        markdown.print(':');
        markdown.print(issue.column());
        markdown.println(")");
    }

    private static String getEmoji(final Issue.Severity severity) {
        return switch (severity) {
            case HIGHEST -> ":bangbang:";
            case HIGH -> ":exclamation:";
            case MEDIUM -> ":grey_exclamation:";
            case LOW -> ":warning:";
            case LOWEST -> ":speak_no_evil:";
            case IGNORE -> ":information_source:";
        };
    }
}

package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Analysis;
import ch.acanda.maven.coan.Issue;
import org.apache.maven.artifact.Artifact;
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

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summarizingLong;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

@SuppressWarnings("java:S1192" /* duplicated strings: creating constants for html tags makes the code less readable. */)
public class HtmlReport {

    private final Artifact artifact;
    private final Path baseDir;
    private final List<Analysis> analyses;

    public HtmlReport(final Artifact artifact, final Path baseDir, final Analysis... analyses) {
        this.artifact = artifact;
        this.baseDir = baseDir;
        this.analyses = Arrays.asList(analyses);
    }

    public void writeTo(final Path file) throws MojoFailureException {
        try (Writer out = Files.newBufferedWriter(file)) {
            writeTo(out);
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to write HTML report to file " + file + ".", e);
        }
    }

    private void writeTo(final Writer out) throws IOException {
        final PrintWriter html = new PrintWriter(out);
        html.println("<!DOCTYPE html>");
        html.println("<html lang=\"en\">");
        writeHead(html);
        writeBody(html);
        html.println("</html>");
        html.flush();
        if (html.checkError()) {
            throw new IOException("Failed to create HTML report.");
        }
    }

    private void writeBody(final PrintWriter html) {
        html.println("<body>");
        html.print("<h1>Code Analysis for ");
        html.print(escapeHtml4(artifact.getArtifactId()));
        final String version = artifact.getVersion();
        if (version != null) {
            html.print(" ");
            html.print(version);
        }
        html.println("</h1>");
        writeSummary(html);
        writeAnalyses(html);
        html.println("</body>");
    }

    private void writeSummary(final PrintWriter html) {
        final boolean foundIssues = analyses.stream().anyMatch(Analysis::foundIssues);
        html.println("<section>");
        if (foundIssues) {
            html.println("<h2>Summary</h2>");
            analyses.stream()
                .collect(groupingBy(Analysis::getToolName, summarizingLong(a -> a.getIssues().size())))
                .entrySet()
                .stream()
                .map(entry -> escapeHtml4(entry.getKey()) + " found " + numberOfIssues(entry.getValue()) + ".")
                .sorted()
                .forEachOrdered(toolSummary -> {
                    html.print("<p>");
                    html.print(toolSummary);
                    html.println("</p>");
                });
        } else {
            html.println("<h2>Congratulations!</h2>");
            html.println("<p>The code analysers did not find any issues.</p>");
        }
        html.println("</section>");
    }

    private void writeAnalyses(final PrintWriter html) {
        final Map<MavenProject, List<Analysis>> analysesByProject = analyses.stream()
            .filter(Analysis::foundIssues)
            .collect(groupingBy(Analysis::getProject));
        final boolean includeProjectName = analysesByProject.size() > 1;
        analysesByProject
            .entrySet()
            .stream()
            .sorted(comparing(e -> getProjectName(e.getKey())))
            .forEachOrdered(entry -> writeAnalyses(entry.getKey(), entry.getValue(), includeProjectName, html));
    }

    private void writeAnalyses(final MavenProject project, final List<Analysis> analyses,
        final boolean includeProjectName, final PrintWriter html) {
        if (includeProjectName) {
            html.println("<section>");
            html.println("<details open=\"open\">");
            html.print("<summary>");
            html.print(escapeHtml4(getProjectName(project)));
            html.println("</summary>");
        }
        analyses.forEach(analysis -> writeAnalysis(analysis, html));
        if (includeProjectName) {
            html.println("</details>");
            html.println("</section>");
        }
    }

    private void writeAnalysis(final Analysis analysis, final PrintWriter html) {
        html.println("<section>");
        html.print("<h2>");
        html.print(escapeHtml4(analysis.getToolName()));
        html.println(" Report</h2>");
        analysis.getIssues()
            .stream()
            .collect(groupingBy(Issue::getFile))
            .forEach((file, issues) -> writeIssues(file, issues, html));
        html.println("</section>");
    }

    private void writeIssues(final Path file, final List<? extends Issue> issues, final PrintWriter html) {
        html.print("<h3>");
        html.print(escapeHtml4(baseDir.relativize(file).toString().replace('\\', '/')));
        html.println("</h3>");
        html.println("<ul>");
        issues.stream()
            .sorted(comparing(Issue::getSeverity)
                .thenComparing(Issue::getName)
                .thenComparing(Issue::getLine)
                .thenComparing(Issue::getColumn))
            .forEachOrdered(issue -> writeIssue(issue, html));
        html.println("</ul>");
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

    private static void writeHead(final PrintWriter html) {
        html.println("<head>");
        html.println("<meta charset=\"utf-8\">");
        html.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.println("<title>Code Analysis Report</title>");
        html.println("<style type=\"text/css\">");
        html.println("* { font-family: sans-serif; }");
        html.println("html, body { background-color: #f8f8f8; margin: 0; padding: 0; }");
        html.println("h1 { font-size: 1.728em; background-color: white; border-bottom: 0.1em solid #D50000; "
                     + "padding: 0.5em 1em; margin-top: 0; box-shadow: 0 4px 8px 0 rgba(0,0,0,0.2);}");
        html.println("h2 { font-size: 1.44em; }");
        html.println("h3 { font-size: 1.2em; }");
        html.println("section { box-shadow: 0 4px 8px 0 rgba(0,0,0,0.2); border-radius: 0.3em; "
                     + "background-color: white; padding: 0.2em 0.7em; margin: 1em; }");
        html.println("section section { box-shadow: none; }");
        html.println("details { margin: 1em 0; }");
        html.println("summary { font-size: 1.44em; font-weight: bold }");
        html.println("ul { list-style-position: inside; list-style-type: none; padding-left: 0; }");
        html.println("li { margin: 0.5em 0; }");
        html.println(".label { font-size: 0.9em; font-variant: small-caps; "
                     + "border-radius: 0.3em; padding: 0.2em 0.4em; margin-right: 0.5em; }");
        html.println(".label.ignore { background-color: #757575; color: white; }");
        html.println(".label.lowest { background-color: #546E7A; color: white; }");
        html.println(".label.low { background-color: #2962FF; color: white; }");
        html.println(".label.medium { background-color: #FFD600; color: black; }");
        html.println(".label.high { background-color: #FFAB00; color: black; }");
        html.println(".label.highest { background-color: #D50000; color: white; }");
        html.println("</style>");
        html.println("</head>");
    }

    private static void writeIssue(final Issue issue, final PrintWriter html) {
        html.print("<li><span class=\"label ");
        html.print(issue.getSeverity().getName());
        html.print("\">");
        html.print(escapeHtml4(issue.getName()));
        html.print("</span> ");
        html.print(escapeHtml4(issue.getDescription()));
        html.print(" (");
        html.print(issue.getLine());
        html.print(':');
        html.print(issue.getColumn());
        html.println(")</li>");
    }

}

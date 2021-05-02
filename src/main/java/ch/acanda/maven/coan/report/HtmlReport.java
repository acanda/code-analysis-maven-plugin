package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Analysis;
import ch.acanda.maven.coan.Issue;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

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

    private void writeHead(final PrintWriter html) {
        html.println("<head>");
        html.println("<meta charset=\"utf-8\">");
        html.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.println("<title>Checkstyle Report</title>");
        html.println("<style type=\"text/css\">");
        html.println("* { font-family: sans-serif; }");
        html.println("html, body { background-color: #f8f8f8; margin: 0; padding: 0; }");
        html.println("h1 { font-size: 1.728em; background-color: white; border-bottom: 0.1em solid #D50000; "
                + "padding: 0.5em 1em; margin-top: 0; box-shadow: 0 4px 8px 0 rgba(0,0,0,0.2);}");
        html.println("h2 { font-size: 1.44em; }");
        html.println("h3 { font-size: 1.2em; }");
        html.println("section { box-shadow: 0 4px 8px 0 rgba(0,0,0,0.2); border-radius: 0.3em; "
                + "background-color: white; padding: 0.2em 0.7em; margin: 1em; }");
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
        final boolean foundIssues = analyses.stream().allMatch(Analysis::foundIssues);
        html.println("<section>");
        if (foundIssues) {
            html.println("<h2>Summary</h2>");
            analyses.stream()
                    .map(a -> escapeHtml4(a.getToolName()) + " found " + numberOfIssues(a) + ".")
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

    private String numberOfIssues(final Analysis analysis) {
        final int count = analysis.getIssues().size();
        final String noun = count == 1 ? " issue" : " issues";
        return count + noun;
    }

    private void writeAnalyses(final PrintWriter html) {
        analyses.stream()
                .filter(Analysis::foundIssues)
                .forEachOrdered(analysis -> writeAnalysis(analysis, html));
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

    private void writeIssue(final Issue issue, final PrintWriter html) {
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

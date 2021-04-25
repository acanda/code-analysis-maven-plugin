package ch.acanda.maven.coan.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HtmlLogger implements AuditListener {

    private final PrintWriter html;
    private final Path baseDir;
    private final String projectName;

    private boolean currentFileHasErrors;
    private boolean hasErrors;

    public HtmlLogger(final Path htmlFile, final Path baseDir, final String projectName) {
        try {
            html = new PrintWriter(Files.newBufferedWriter(htmlFile, UTF_8));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        this.baseDir = baseDir;
        this.projectName = projectName;
    }

    @Override
    public void auditStarted(final AuditEvent event) {
        html.println("<!DOCTYPE html>");
        html.println("<html lang=\"en\">");
        html.println("<head>");
        html.println("<meta charset=\"utf-8\">");
        html.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.println("<title>Checkstyle Report</title>");
        html.println("<style type=\"text/css\">");
        html.println("* { font-family: sans-serif; }");
        html.println("html, body { background-color: #f8f8f8; margin: 0; padding: 0; }");
        html.println("h1 { font-size: 1.5em; background-color: white; border-bottom: 0.1em solid #D50000; "
                + "padding: 0.5em 1em; margin-top: 0; box-shadow: 0 4px 8px 0 rgba(0,0,0,0.2);}");
        html.println("h2 { font-size: 1.2em; }");
        html.println("section { box-shadow: 0 4px 8px 0 rgba(0,0,0,0.2); background-color: white; "
                + "padding: 0.2em 0.7em; margin: 1em; }");
        html.println("ul { list-style-position: inside; list-style-type: none; padding-left: 0; }");
        html.println("li { margin: 0.5em 0; }");
        html.println(".label { font-size: 0.9em; font-variant: small-caps; "
                + "border-radius: 0.3em; padding: 0.2em 0.4em; margin-right: 0.5em; }");
        html.println(".label.ignore { background-color: #757575; color: white; }");
        html.println(".label.info { background-color: #2962FF; color: white; }");
        html.println(".label.warning { background-color: #FFAB00; color: black; }");
        html.println(".label.error { background-color: #D50000; color: white; }");
        html.println("</style>");
        html.println("</head>");
        html.println("<body>");
        html.print("<h1>Checkstyle Report for ");
        html.print(projectName);
        html.println("</h1>");
    }

    @Override
    public void auditFinished(final AuditEvent event) {
        if (!hasErrors) {
            html.println("<section>");
            html.println("<h2>Congratulations!</h2>");
            html.println("<p>Checkstyle did not find any issues.</p>");
            html.println("</section>");
        }
        html.println("</body>");
        html.println("</html>");
        html.flush();
        html.close();
    }

    @Override
    public void fileStarted(final AuditEvent event) {
        currentFileHasErrors = false;
    }

    @Override
    public void addError(final AuditEvent event) {
        if (!currentFileHasErrors) {
            html.print("<section>");
            html.print("<h2>");
            html.print(baseDir.relativize(Paths.get(event.getFileName())).toString());
            html.println("</h2>");
            html.println("<ul>");
            currentFileHasErrors = true;
        }

        final String sourceName = event.getSourceName();
        final int pos = sourceName.lastIndexOf('.');
        final String name = pos == -1 ? sourceName : sourceName.substring(pos + 1);
        event.getSeverityLevel().getName();
        html.print("<li><span class=\"label ");
        html.print(event.getSeverityLevel().getName());
        html.print("\">");
        html.print(name);
        html.print("</span> ");
        html.print(event.getMessage());
        html.print(" (");
        html.print(event.getLine());
        html.print(':');
        html.print(event.getColumn());
        html.println(")</li>");
    }

    @Override
    public void fileFinished(final AuditEvent event) {
        if (currentFileHasErrors) {
            html.println("</ul>");
            html.print("</section>");
            hasErrors = true;
        }
    }

    @Override
    public void addException(final AuditEvent event, final Throwable throwable) {
        // nothing to do
    }

}

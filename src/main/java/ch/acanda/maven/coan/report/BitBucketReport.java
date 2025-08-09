package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Inspection;
import ch.acanda.maven.coan.Issue;
import ch.acanda.maven.coan.Issue.Severity;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.text.StringEscapeUtils.escapeJson;

/**
 * Creates a code insights report for BitBucket.
 * <p>
 * <a
 * href="https://developer.atlassian.com/bitbucket/api/2/reference/resource/repositories/%7Bworkspace%7D/%7Brepo_slug%7D/commit/%7Bcommit%7D/reports/%7BreportId%7D">
 * BitBucket Code Insights API
 * </a>
 * </p>
 */
public class BitBucketReport {

    private static final Map<Severity, String> SEVERITIES = Map.of(
        Severity.HIGHEST, "HIGH",
        Severity.HIGH, "HIGH",
        Severity.MEDIUM, "MEDIUM",
        Severity.LOW, "LOW",
        Severity.LOWEST, "LOW",
        Severity.IGNORE, "LOW"
    );

    private final MavenProject project;
    private final Path baseDir;
    private final List<Inspection> inspections;

    public BitBucketReport(final MavenProject project, final Path baseDir, final Inspection... inspections) {
        this.project = project;
        this.baseDir = baseDir;
        this.inspections = Arrays.asList(inspections);
    }

    /**
     * Publishes the BitBucket Code Insights report to the BitBucket API if running in a BitBucket pipeline.
     *
     * @throws MojoFailureException
     *     If the report cannot be published
     */
    public void publishToBitBucket(final BitBucketPipeline pipeline) throws MojoFailureException {
        try {
            final String apiUrl = String.format(
                "%s/2.0/repositories/%s/%s/commit/%s/reports/code-analysis-maven-plugin",
                pipeline.apiHost(), pipeline.repoOwner(), pipeline.repoSlug(), pipeline.commit());
            System.out.println("apiUrl = " + apiUrl);
            final InetSocketAddress proxy = new InetSocketAddress("localhost", 29418);
            System.out.println("proxy = " + proxy);

            // Create HTTP client
            final HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(30))
                .proxy(ProxySelector.of(proxy))
                .build();

            // Create HTTP request for report
            final String reportPayload = createReportPayload();
            System.out.println("Report payload = " + reportPayload);
            final HttpRequest reportRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(reportPayload))
                .build();

            final HttpResponse<String> reportResponse =
                client.send(reportRequest, HttpResponse.BodyHandlers.ofString());
            if (reportResponse.statusCode() < 200 || reportResponse.statusCode() >= 300) {
                throw new MojoFailureException("Failed to publish BitBucket Code Insights report. Status code: "
                                               + reportResponse.statusCode() + ", Response: " + reportResponse.body());
            }

            // Create HTTP request for annotations
            final String annotationsPayload = createBulkAnnotationPayload();
            System.out.println("Annotation payload = " + annotationsPayload);
            final HttpRequest annotationsRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/annotations"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(annotationsPayload))
                .build();

            final HttpResponse<String> annotationsResponse =
                client.send(annotationsRequest, HttpResponse.BodyHandlers.ofString());
            if (annotationsResponse.statusCode() < 200 || annotationsResponse.statusCode() >= 300) {
                throw new MojoFailureException("Failed to publish BitBucket Code Insights annotations. Status code: "
                                               + annotationsResponse.statusCode() + ", Response: "
                                               + annotationsResponse.body());
            }
        } catch (final IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new MojoFailureException("Failed to publish BitBucket Code Insights report: " + e.getMessage(), e);
        }
    }

    /**
     * Creates the JSON payload for the BitBucket Code Insights report.
     *
     * @return The JSON payload as a string
     */
    private String createReportPayload() {
        final String details = "Code analysis report for " + project.getName() + ".";
        final StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"title\": \"Code Analysis Report\",\n");
        json.append("  \"details\": \"").append(escapeJson(details)).append("\",\n");
        json.append("  \"report_type\": \"BUG\",\n");
        json.append("  \"reporter\": \"Code Analysis Maven Plugin\",\n");
        json.append("  \"result\": \"").append(hasIssues() ? "FAILED" : "PASSED").append("\"\n");
        json.append("}");
        return json.toString();
    }

    private String createBulkAnnotationPayload() {
        final StringBuilder json = new StringBuilder();
        json.append("[\n");
        json.append(
            inspections.stream()
                .flatMap(inspection ->
                    inspection.issues().stream().map(
                        issue -> createAnnotationPayload(inspection, issue)
                    )
                )
                .collect(joining(",\n"))
        );
        json.append("\n]\n");
        return json.toString();
    }

    private String createAnnotationPayload(final Inspection inspection, final Issue issue) {
        final StringBuilder json = new StringBuilder();
        json.append("  {\n");
        json.append("    \"external_id\": \"").append(UUID.randomUUID()).append("\",\n");
        json.append("    \"title\": \"").append(escapeJson(issue.name())).append("\",\n");
        json.append("    \"annotation_type\": \"CODE_SMELL\",\n");
        json.append("    \"summary\": \"[")
            .append(escapeJson(inspection.toolName()))
            .append("] ")
            .append(escapeJson(issue.description()))
            .append("\",\n");
        json.append("    \"severity\": \"").append(getSeverity(issue.severity())).append("\",\n");
        json.append("    \"path\": \"").append(escapeJson(getPath(issue.file()))).append("\",\n");
        json.append("    \"line\": ").append(issue.line()).append("\n");
        json.append("  }");
        return json.toString();
    }

    private String getSeverity(final Severity severity) {
        return switch (severity) {
            case HIGHEST -> "CRITICAL";
            case HIGH -> "HIGH";
            case MEDIUM -> "MEDIUM";
            case LOW, LOWEST, IGNORE -> "LOW";
        };
    }

    private String getPath(final Path path) {
        return baseDir.relativize(path).toString().replace('\\', '/');
    }

    /**
     * Checks if any of the inspections have issues.
     *
     * @return true if any inspection has issues, false otherwise
     */
    private boolean hasIssues() {
        return inspections.stream().anyMatch(Inspection::foundIssues);
    }

}

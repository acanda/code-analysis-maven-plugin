package ch.acanda.maven.coan.report.bitbucket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.text.StringEscapeUtils.escapeJson;

public class ReportApiClient {

    private final HttpClient client;
    private final String reportUrl;

    public ReportApiClient(final Pipeline pipeline) {
        client = createClient(pipeline);
        reportUrl = "%s/2.0/repositories/%s/%s/commit/%s/reports/code-analysis-maven-plugin"
            .formatted(pipeline.apiHost(), pipeline.repoOwner(), pipeline.repoSlug(), pipeline.commit());
    }

    private static HttpClient createClient(final Pipeline pipeline) {
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(30))
            .proxy(getProxy(pipeline.apiProxy()))
            .build();
    }

    private static ProxySelector getProxy(final String apiProxy) {
        if (apiProxy != null) {
            final String[] proxyParts = apiProxy.split(":");
            if (proxyParts.length == 2) {
                final InetSocketAddress proxy = new InetSocketAddress(proxyParts[0], Integer.parseInt(proxyParts[1]));
                return ProxySelector.of(proxy);
            } else {
                throw new IllegalArgumentException(
                    "Invalid proxy specification, expected \"host:port\" but was \"" + apiProxy + "\""
                );
            }
        }
        return ProxySelector.getDefault();
    }

    public void createOrUpdateReport(final Report report) throws IOException, InterruptedException {
        final String payload =
            """
            {
              "reporter": "Code Analysis Maven Plugin",
              "report_type": "BUG",
              "title": "Code Analysis Report",
              "details": "%s",
              "result": "%s"
            }
            """.formatted(escapeJson(report.details()), report.result());
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(reportUrl))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(payload))
            .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (!isOk(response.statusCode())) {
            throw new IOException(
                "Failed to publish Bitbucket code insights report. Status code: %s, Response: %s"
                    .formatted(response.statusCode(), response.body())
            );
        }
    }

    public void createOrUpdateAnnotations(final Collection<Annotation> annotations)
        throws IOException, InterruptedException {
        final Collection<String> annotationsPayloads = createAnnotationPayloads(annotations);
        final String payload =
            """
            [
            %s
            ]
            """.formatted(String.join(",\n", annotationsPayloads));

        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(reportUrl + "/annotations"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (!isOk(response.statusCode())) {
            throw new IOException(
                "Failed to publish Bitbucket code insights annotations. Status code: %s, Response: %s"
                    .formatted(response.statusCode(), response.body())
            );
        }
    }

   private List<String> createAnnotationPayloads(final Collection<Annotation> annotations) {
        return annotations.stream().map(annotation ->
            """
            {
              "external_id": "%s",
              "title": "%s",
              "annotation_type": "CODE_SMELL",
              "summary": "%s",
              "severity": "%s",
              "path": "%s",
              "line": %d
            }
            """.formatted(
                annotation.externalId(),
                escapeJson(annotation.title()),
                escapeJson(annotation.summary()),
                annotation.severity(),
                escapeJson(annotation.path()),
                annotation.line()
            )
        ).toList();
    }

    private boolean isOk(final int status) {
        return status >= 200 && status < 300;
    }

}

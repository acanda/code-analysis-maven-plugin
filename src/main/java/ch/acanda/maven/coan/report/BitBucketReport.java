package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Inspection;
import ch.acanda.maven.coan.Issue;
import ch.acanda.maven.coan.Issue.Severity;
import ch.acanda.maven.coan.report.bitbucket.BitbucketReportClient;
import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static ch.acanda.maven.coan.report.bitbucket.BitbucketReportClient.Result.FAILED;
import static ch.acanda.maven.coan.report.bitbucket.BitbucketReportClient.Result.PASSED;

/**
 * Creates a code insights report for BitBucket and publishes it to the
 * <a href="https://developer.atlassian.com/cloud/bitbucket/rest/api-group-reports/#api-group-reports">BitBucket Reports API</a>
 */
public class BitBucketReport {

    private final Path baseDir;
    private final List<Inspection> inspections;

    public BitBucketReport(final Path baseDir, final Inspection... inspections) {
        this.baseDir = baseDir;
        this.inspections = Arrays.asList(inspections);
    }

    public void publishToBitBucket(final BitBucketPipeline pipeline) throws MojoFailureException {
        final BitbucketReportClient client = new BitbucketReportClient(pipeline);
        try {
            client.createOrUpdateReport(createReport());
            client.createOrUpdateAnnotations(createAnnotations());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to publish BitBucket Code Insights report", e);
        }
    }

    private BitbucketReportClient.Report createReport() {
        final int numberOfIssues = inspections.stream().mapToInt(Inspection::getNumberOfIssues).sum();
        final String issues = switch (numberOfIssues) {
            case 0 -> "no issues";
            case 1 -> "one issue";
            default -> numberOfIssues + " issues";
        };
        return new BitbucketReportClient.Report(
            "The Code Analysis Maven Plugin found %s.".formatted(issues),
            numberOfIssues == 0 ? PASSED : FAILED
        );
    }

    private List<BitbucketReportClient.Annotation> createAnnotations() {
        return inspections.stream().flatMap(inspection ->
            inspection.issues().stream().map(issue ->
                createAnnotation(inspection, issue)
            )
        ).toList();
    }

    private BitbucketReportClient.Annotation createAnnotation(final Inspection inspection, final Issue issue) {
        final String path = getPath(issue.file());
        final String externalId =
            "%s:%s:%s:%d:%d".formatted(inspection.toolName(), issue.name(), path, issue.line(), issue.column());
        return new BitbucketReportClient.Annotation(
            UUID.nameUUIDFromBytes(externalId.getBytes(StandardCharsets.UTF_8)),
            issue.name(),
            "[%s] %s".formatted(inspection.toolName(), issue.description()),
            getSeverity(issue.severity()),
            path,
            issue.line()
        );
    }

    private BitbucketReportClient.AnnotationSeverity getSeverity(final Severity severity) {
        return switch (severity) {
            case HIGHEST -> BitbucketReportClient.AnnotationSeverity.CRITICAL;
            case HIGH -> BitbucketReportClient.AnnotationSeverity.HIGH;
            case MEDIUM -> BitbucketReportClient.AnnotationSeverity.MEDIUM;
            case LOW, LOWEST, IGNORE -> BitbucketReportClient.AnnotationSeverity.LOW;
        };
    }

    private String getPath(final Path path) {
        return baseDir.relativize(path).toString().replace('\\', '/');
    }

}

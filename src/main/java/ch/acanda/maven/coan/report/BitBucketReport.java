package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Inspection;
import ch.acanda.maven.coan.Issue;
import ch.acanda.maven.coan.Issue.Severity;
import ch.acanda.maven.coan.report.bitbucket.Annotation;
import ch.acanda.maven.coan.report.bitbucket.AnnotationSeverity;
import ch.acanda.maven.coan.report.bitbucket.Pipeline;
import ch.acanda.maven.coan.report.bitbucket.Report;
import ch.acanda.maven.coan.report.bitbucket.ReportApiClient;
import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static ch.acanda.maven.coan.report.bitbucket.Result.FAILED;
import static ch.acanda.maven.coan.report.bitbucket.Result.PASSED;

/**
 * Creates a code insights report for BitBucket and publishes it to the
 * <a href="https://developer.atlassian.com/cloud/bitbucket/rest/api-group-reports/#api-group-reports">BitBucket Reports
 * API</a>
 */
public class BitBucketReport {

    private final Path baseDir;
    private final List<Inspection> inspections;

    public BitBucketReport(final Path baseDir, final Inspection... inspections) {
        this.baseDir = baseDir;
        this.inspections = Arrays.asList(inspections);
    }

    public void publishToBitBucket(final Pipeline pipeline) throws MojoFailureException {
        final ReportApiClient client = new ReportApiClient(pipeline);
        try {
            client.createOrUpdateReport(createReport());
            for (final List<Annotation> annotations : createPartitionedAnnotations()) {
                client.createOrUpdateAnnotations(annotations);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoFailureException("Publishing the BitBucket Code Insights report was interrupted", e);
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to publish the BitBucket Code Insights report", e);
        }
    }

    private Report createReport() {
        final int numberOfIssues = inspections.stream().mapToInt(Inspection::getNumberOfIssues).sum();
        final String issues = switch (numberOfIssues) {
            case 0 -> "no issues";
            case 1 -> "one issue";
            default -> numberOfIssues + " issues";
        };
        return new Report(
            "The Code Analysis Maven Plugin found %s.".formatted(issues),
            numberOfIssues == 0 ? PASSED : FAILED
        );
    }

    /**
     * The BitBucket API only allows a maximum of 100 annotations per request,
     * so we need to partition the annotations into smaller lists.
     */
    private List<List<Annotation>> createPartitionedAnnotations() {
        final List<Annotation> allAnnotations = inspections.stream().flatMap(inspection ->
            inspection.issues().stream().map(issue ->
                createAnnotation(inspection, issue)
            )
        ).toList();
        return partition(allAnnotations, 100);
    }

    private List<List<Annotation>> partition(final List<Annotation> list, final int size) {
        final int listSize = list.size();
        return IntStream.range(0, (listSize + size - 1) / size)
            .mapToObj(i ->
                list.subList(
                    i * size,
                    Math.min(listSize, (i + 1) * size)
                )
            )
            .toList();
    }

    private Annotation createAnnotation(final Inspection inspection, final Issue issue) {
        final String path = getPath(issue.file());
        final String externalId =
            "%s:%s:%s:%d:%d".formatted(inspection.toolName(), issue.name(), path, issue.line(), issue.column());
        return new Annotation(
            UUID.nameUUIDFromBytes(externalId.getBytes(StandardCharsets.UTF_8)),
            issue.name(),
            "[%s] %s".formatted(inspection.toolName(), issue.description()),
            getSeverity(issue.severity()),
            path,
            issue.line()
        );
    }

    private AnnotationSeverity getSeverity(final Severity severity) {
        return switch (severity) {
            case HIGHEST -> AnnotationSeverity.CRITICAL;
            case HIGH -> AnnotationSeverity.HIGH;
            case MEDIUM -> AnnotationSeverity.MEDIUM;
            case LOW, LOWEST, IGNORE -> AnnotationSeverity.LOW;
        };
    }

    private String getPath(final Path path) {
        return baseDir.relativize(path).toString().replace('\\', '/');
    }

}

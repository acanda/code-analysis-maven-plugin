package ch.acanda.maven.coan.report.bitbucket;

import java.util.UUID;

public record Annotation(
    UUID externalId,
    String title,
    String summary,
    AnnotationSeverity severity,
    String path,
    int line
) {
}

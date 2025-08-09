package ch.acanda.maven.coan.report;

public record BitBucketPipeline(
    String repoOwner,
    String repoSlug,
    String commit,
    String apiHost,
    String apiProxy
) {
}

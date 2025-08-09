package ch.acanda.maven.coan.report.bitbucket;

public record Pipeline(
    String repoOwner,
    String repoSlug,
    String commit,
    String apiHost,
    String apiProxy
) {
}

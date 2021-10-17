package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Analysis;
import ch.acanda.maven.coan.Issue;
import ch.acanda.maven.coan.Issue.Severity;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONComposer;
import com.fasterxml.jackson.jr.ob.comp.ArrayComposer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;

/**
 * Creates a code quality report for GitLab.
 * <p>
 * <a href="https://docs.gitlab.com/ee/user/project/merge_requests/code_quality.html#implementing-a-custom-tool">
 * GitLab code quality report format
 * </a>
 * </p>
 */
public class GitLabReport {

    private static final Map<Severity, String> SEVERITIES = Map.of(
        Severity.HIGHEST, "blocker",
        Severity.HIGH, "critical",
        Severity.MEDIUM, "major",
        Severity.LOW, "minor",
        Severity.LOWEST, "minor",
        Severity.IGNORE, "info"
    );

    private final Path baseDir;
    private final List<Analysis> analyses;

    public GitLabReport(final Path baseDir, final Analysis... analyses) {
        this.baseDir = baseDir;
        this.analyses = Arrays.asList(analyses);
    }


    public void writeTo(final Path file) throws MojoFailureException {
        try {
            final ArrayComposer<JSONComposer<OutputStream>> json =
                JSON.std
                    .with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                    .composeTo(file.toFile())
                    .startArray();

            for (final Analysis analysis : analyses) {
                for (final Issue issue : analysis.getIssues()) {
                    composeIssue(json, analysis, issue);
                }
            }

            json.end().finish();
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to write Gitlab Code Quality report to file " + file + ".", e);
        }

    }

    private void composeIssue(final ArrayComposer<JSONComposer<OutputStream>> json, final Analysis analysis,
        final Issue issue) throws IOException {
        final String description =
            format(Locale.ENGLISH, "%s [%s]: %s", analysis.getToolName(), issue.getName(), issue.getDescription());
        final String path = baseDir.relativize(issue.getFile()).toString().replace('\\', '/');
        final String fingerprint = createFingerprint(issue.getName(), path, issue.getLine(), issue.getColumn());
        json.startObject()
            .put("description", description)
            .put("fingerprint", fingerprint)
            .put("severity", SEVERITIES.get(issue.getSeverity()))
            .startObjectField("location")
            .put("path", path)
            .startObjectField("lines")
            .put("begin", issue.getLine())
            .end()
            .end()
            .end();
    }

    private static String createFingerprint(final String name, final String path, final int line, final int column) {
        final byte[] bytes = ArrayUtils.addAll(ArrayUtils.addAll(
                name.getBytes(StandardCharsets.UTF_8),
                path.getBytes(StandardCharsets.UTF_8)),
            ByteBuffer.allocate(Integer.BYTES * 2).putInt(line).putInt(column).array());
        return UUID.nameUUIDFromBytes(bytes).toString();
    }

}

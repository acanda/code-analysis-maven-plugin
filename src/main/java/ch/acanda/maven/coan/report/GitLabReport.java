package ch.acanda.maven.coan.report;

import ch.acanda.maven.coan.Inspection;
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
 * <a href="https://docs.gitlab.com/ci/testing/code_quality/#code-quality-report-format">
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
    private final List<Inspection> inspections;

    public GitLabReport(final Path baseDir, final Inspection... inspections) {
        this.baseDir = baseDir;
        this.inspections = Arrays.asList(inspections);
    }


    public void writeTo(final Path file) throws MojoFailureException {
        try {
            final ArrayComposer<JSONComposer<OutputStream>> json =
                JSON.std
                    .with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                    .composeTo(file.toFile())
                    .startArray();

            for (final Inspection inspection : inspections) {
                for (final Issue issue : inspection.issues()) {
                    composeIssue(json, inspection, issue);
                }
            }

            json.end().finish().close();
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to write Gitlab Code Quality report to file " + file + ".", e);
        }

    }

    private void composeIssue(final ArrayComposer<JSONComposer<OutputStream>> json, final Inspection inspection,
        final Issue issue) throws IOException {
        final String description =
            format(Locale.ENGLISH, "%s [%s]: %s", inspection.toolName(), issue.name(), issue.description());
        final String path = baseDir.relativize(issue.file()).toString().replace('\\', '/');
        final String fingerprint = createFingerprint(issue.name(), path, issue.line(), issue.column());
        json.startObject()
            .put("description", description)
            .put("fingerprint", fingerprint)
            .put("severity", SEVERITIES.get(issue.severity()))
            .startObjectField("location")
            .put("path", path)
            .startObjectField("lines")
            .put("begin", issue.line())
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

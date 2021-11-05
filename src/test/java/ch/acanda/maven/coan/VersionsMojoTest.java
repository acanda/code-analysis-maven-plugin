package ch.acanda.maven.coan;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionsMojoTest {

    @Test
    void versionsShouldOutputToolVersions() throws MojoFailureException {
        final VersionsMojo mojo = new VersionsMojo();
        final RecordingLog log = new RecordingLog();
        mojo.setLog(log);

        mojo.execute();

        assertThat(log.getLogAsString()).matches(
            "\\[info] code-analysis-maven-plugin \\d+\\.\\d+\\.\\d+\n"
            + "\\[info] PMD \\d+\\.\\d+\\.\\d+\n"
            + "\\[info] Checkstyle \\d+\\.\\d+(\\.\\d+)?"
        );
    }

}

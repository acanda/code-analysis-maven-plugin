package ch.acanda.maven.coan;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionsMojoTest {

    static final String PMD_VERSION_PATTERN = "PMD \\d+\\.\\d+\\.\\d+(-rc\\d+)?";
    static final String CHECKSTYLE_VERSION_PATTERN = "Checkstyle \\d+\\.\\d+(\\.\\d+)?";


    @Test
    void versionsShouldOutputToolVersions() throws MojoFailureException {
        final VersionsMojo mojo = new VersionsMojo();
        final RecordingLog log = new RecordingLog();
        mojo.setLog(log);

        mojo.execute();

        assertThat(log.getLogAsString()).matches(
            "\\[info] code-analysis-maven-plugin \\d+\\.\\d+\\.\\d+\n"
            + "\\[info] " + PMD_VERSION_PATTERN + "\n"
            + "\\[info] " + CHECKSTYLE_VERSION_PATTERN
        );
    }

}

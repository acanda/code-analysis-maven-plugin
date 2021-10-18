package ch.acanda.maven.coan;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyseMojoTest {

    @Test
    void skip() throws MojoFailureException {
        final AnalyseMojo mojo = new AnalyseMojo();
        mojo.setSkip(true);
        final RecordingLog log = new RecordingLog();
        mojo.setLog(log);

        mojo.execute();

        assertThat(log.getLogAsString()).isEqualTo("[info] Skipping code analysis");
    }

}

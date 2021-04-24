package ch.acanda.maven.coan.pmd;

import lombok.Builder;
import lombok.Data;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

@Data
@Builder
public class PmdConfig {
    private final MavenProject project;
    private final Log log;
    private final String configPath;
    private final String targetPath;
}

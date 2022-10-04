package ch.acanda.maven.coan.pmd;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public record PmdConfig(
    MavenProject project,
    Log log,
    String configPath,
    String targetPath
) {
}

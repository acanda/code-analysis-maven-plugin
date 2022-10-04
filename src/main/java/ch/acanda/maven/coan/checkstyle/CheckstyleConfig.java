package ch.acanda.maven.coan.checkstyle;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public record CheckstyleConfig(
    MavenProject project,
    Log log,
    String configPath,
    String targetPath
) {
}

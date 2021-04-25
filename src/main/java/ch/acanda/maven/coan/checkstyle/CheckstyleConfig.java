package ch.acanda.maven.coan.checkstyle;

import lombok.Builder;
import lombok.Data;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

@Data
@Builder
public class CheckstyleConfig {
    private final MavenProject project;
    private final Log log;
    private final String configPath;
    private final String targetPath;
}

package ch.acanda.maven.coan;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigsTest {

    @Test
    void configInParentProject(@TempDir final Path tempDir) throws MojoFailureException, IOException {
        final RecordingLog log = new RecordingLog();
        final Path parentBaseDir = tempDir.resolve("parent");
        final MavenProject parent = createProject(parentBaseDir);
        final Path configPath = createConfigFile(parentBaseDir, "config", "abc.json");
        final MavenProject project = createProject(tempDir.resolve("project"));
        project.setParent(parent);
        final Path path = Configs.resolve("ABC", "config/abc.json", project, log);
        assertThat(path).isReadable().isAbsolute().isEqualTo(configPath.toAbsolutePath());
    }

    @Test
    void configInProject(@TempDir final Path tempDir) throws MojoFailureException, IOException {
        final RecordingLog log = new RecordingLog();
        final MavenProject parent = createProject(tempDir.resolve("parent"));
        final Path projectBaseDir = tempDir.resolve("project");
        final MavenProject project = createProject(tempDir.resolve("project"));
        final Path configPath = createConfigFile(projectBaseDir, "config", "abc.json");
        project.setParent(parent);
        final Path path = Configs.resolve("ABC", "config/abc.json", project, log);
        assertThat(path).isReadable().isAbsolute().isEqualTo(configPath.toAbsolutePath());
    }

    private static MavenProject createProject(final Path baseDir) throws IOException {
        Files.createDirectory(baseDir);
        final MavenProject project = new MavenProject();
        project.setFile(baseDir.resolve("pom.xml").toFile());
        project.setArtifactId(baseDir.getFileName().toString());
        return project;
    }

    private static Path createConfigFile(final Path path, final String... segments) throws IOException {
        final Path configPath = Paths.get(path.toString(), segments);
        Files.createDirectories(configPath.getParent());
        Files.createFile(configPath);
        return configPath;
    }

}

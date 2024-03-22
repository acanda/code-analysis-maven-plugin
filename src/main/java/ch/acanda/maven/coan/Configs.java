package ch.acanda.maven.coan;

import lombok.experimental.UtilityClass;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.joining;

@UtilityClass
public class Configs {

    public static Path resolve(final String tool, final String configPath, final MavenProject project, final Log log)
        throws MojoFailureException {
        final List<Path> failed = new ArrayList<>();
        final Optional<Path> path = resolveRecursively(configPath, project, log, failed);
        path.ifPresent(p -> log.debug("Found " + tool + " configPath at " + p + "."));
        return path.orElseThrow(() -> {
            final String paths = failed.stream().map(Path::toString).collect(joining("\n  "));
            final String msg = "Unable to find %s configuration at the following paths:\n  %s";
            return new MojoFailureException(String.format(msg, tool, paths));
        });
    }

    private static Optional<Path> resolveRecursively(final String configPath, final MavenProject project,
        final Log log, final List<Path> failed) {
        final Path baseDir = project.getBasedir().toPath();
        final Path config = baseDir.resolve(configPath);
        if (Files.exists(config)) {
            if (Files.isReadable(config)) {
                return Optional.of(config);
            } else {
                log.warn(config + " exists but is not readable.");
            }
        }
        failed.add(config);
        if (project.hasParent()) {
            return resolveRecursively(configPath, project.getParent(), log, failed);
        }
        return Optional.empty();
    }

}

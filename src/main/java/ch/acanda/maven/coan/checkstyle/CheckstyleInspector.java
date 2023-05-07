package ch.acanda.maven.coan.checkstyle;

import ch.acanda.maven.coan.Configs;
import ch.acanda.maven.coan.Inspection;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.ModuleFactory;
import com.puppycrawl.tools.checkstyle.PackageObjectFactory;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.ThreadModeSettings;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.RootModule;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class CheckstyleInspector {

    private final CheckstyleConfig config;
    private final Log log;

    public CheckstyleInspector(final CheckstyleConfig config) {
        this.config = config;
        log = config.log();
    }

    public Inspection inspect() throws MojoFailureException {
        try {
            final Path configPath = Configs.resolve("Checkstyle", config.configPath(), config.project(), config.log());

            final Configuration configuration = ConfigurationLoader.loadConfiguration(
                configPath.toString(),
                new PropertiesExpander(new Properties()),
                ConfigurationLoader.IgnoredModulesOptions.OMIT,
                new ThreadModeSettings(1, 1));

            final ModuleFactory factory = new PackageObjectFactory(
                Checker.class.getPackage().getName(), Checker.class.getClassLoader());
            final RootModule rootModule = (RootModule) factory.createModule(configuration.getName());
            rootModule.setModuleClassLoader(Checker.class.getClassLoader());
            rootModule.configure(configuration);
            final IssueCollector issueCollector = new IssueCollector();
            rootModule.addListener(issueCollector);

            final List<File> files = getFiles();
            if (log.isDebugEnabled()) {
                final String names = files.stream().map(File::getAbsolutePath).collect(joining("\n  "));
                log.debug("Source files:\n  " + names);
            }
            rootModule.process(files);

            return new CheckstyleInspection(config.project(), issueCollector.getIssues());

        } catch (final CheckstyleException e) {
            throw new MojoFailureException("Failed to run Checkstyle.", e);
        }
    }

    private List<File> getFiles() {
        final Build build = config.project().getBuild();
        final Path sources = Paths.get(build.getSourceDirectory());
        final Path testSources = Paths.get(build.getTestSourceDirectory());
        return Stream.of(sources, testSources)
            .filter(Files::exists)
            .flatMap(CheckstyleInspector::getFiles)
            .filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(".java"))
            .map(Path::toFile)
            .collect(toList());
    }

    private static Stream<Path> getFiles(final Path directory) {
        try {
            return Files.walk(directory);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to collect source files.", e);
        }
    }

}

package ch.acanda.maven.coan.pmd;

import ch.acanda.maven.coan.Analysis;
import ch.acanda.maven.coan.Configs;
import ch.acanda.maven.coan.PmdIssue;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetLoader;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.FileDataSource;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class PmdAnalyser {

    private final PmdConfig config;
    private final Log log;

    public PmdAnalyser(final PmdConfig config) {
        this.config = config;
        log = config.getLog();
    }

    @SuppressWarnings("java:S4792" /* False positive */)
    public Analysis analyse() throws MojoFailureException {
        // This triggers a false positive in Sonar (java:S4792).
        // We disable the logger for PMD because we log the PMD issues in a
        // different format that is consistent across all analysers.
        Logger.getLogger("net.sourceforge.pmd").setLevel(Level.OFF);

        final Path configPath = Configs.resolve("PMD", config.getConfigPath(), config.getProject(), config.getLog());

        final List<RuleSet> ruleSets = loadRuleSets(configPath);
        if (log.isDebugEnabled()) {
            log.debug("Active rules: " + getRules(ruleSets).collect(joining(", ", "", ".")));
        }
        final Path targetPath = Paths.get(config.getTargetPath());
        final PMDConfiguration configuration = createPmdConfiguration(targetPath);
        final List<DataSource> files = getFiles();
        if (log.isDebugEnabled()) {
            final String names = files.stream().map(ds -> ds.getNiceFileName(false, null)).collect(joining("\n  "));
            log.debug("Source files:\n  " + names);
        }
        final Report report = PMD.processFiles(configuration, ruleSets, files, List.of());
        final List<RuleViolation> violations = report.getViolations();
        return new PmdAnalysis(config.getProject(), violations.stream().map(PmdIssue::new).collect(toList()));
    }

    private List<DataSource> getFiles() {
        final Build build = config.getProject().getBuild();
        final Path sources = Paths.get(build.getSourceDirectory());
        final Path testSources = Paths.get(build.getTestSourceDirectory());
        return Stream.of(sources, testSources)
            .filter(Files::exists)
            .flatMap(PmdAnalyser::getFiles)
            .filter(Files::isRegularFile)
            .map(path -> new FileDataSource(path.toFile()))
            .collect(toList());
    }

    private static Stream<Path> getFiles(final Path directory) {
        try {
            return Files.walk(directory);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to collect source files.", e);
        }
    }

    private static List<RuleSet> loadRuleSets(final Path config) {
        final RuleSetLoader loader = new RuleSetLoader();
        loader.enableCompatibility(false);
        return List.of(loader.loadFromResource(config.toString()));
    }

    private static Stream<String> getRules(final List<RuleSet> ruleSets) {
        return ruleSets.stream().flatMap(rs -> rs.getRules().stream()).map(Rule::getName).sorted();
    }

    private static PMDConfiguration createPmdConfiguration(final Path targetPath) {
        final PMDConfiguration configuration = new PMDConfiguration();
        configuration.setAnalysisCacheLocation(targetPath.resolve("pmd.cache").toString());
        return configuration;
    }

}

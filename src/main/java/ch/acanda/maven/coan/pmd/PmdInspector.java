package ch.acanda.maven.coan.pmd;

import ch.acanda.maven.coan.Configs;
import ch.acanda.maven.coan.Inspection;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.lang.rule.RuleSetLoader;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
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

public class PmdInspector {

    private final PmdConfig config;
    private final Log log;

    public PmdInspector(final PmdConfig config) {
        this.config = config;
        log = config.log();
    }

    @SuppressWarnings("java:S4792" /* False positive */)
    public Inspection inspect() throws MojoFailureException {
        // This triggers a false positive in Sonar (java:S4792).
        // We disable the logger for PMD because we log the PMD issues in a
        // different format that is consistent across all inspectors.
        Logger.getLogger("net.sourceforge.pmd").setLevel(Level.OFF);

        final Path configPath = Configs.resolve("PMD", config.configPath(), config.project(), config.log());

        final PMDConfiguration configuration = createPmdConfiguration(config);
        final List<RuleSet> ruleSets = loadRuleSets(configPath);
        if (log.isDebugEnabled()) {
            log.debug("Active rules: " + getRules(ruleSets).collect(joining(", ", "", ".")));
        }
        final List<Path> files = getFiles();
        if (log.isDebugEnabled()) {
            final String names = files.stream().map(Path::toString).collect(joining("\n  "));
            log.debug("Source files:\n  " + names);
        }
        configuration.setInputPathList(files);

        try (PmdAnalysis analysis = PmdAnalysis.create(configuration)) {
            analysis.addRuleSets(ruleSets);
            final Report report = analysis.performAnalysisAndCollectReport();
            final List<RuleViolation> violations = report.getViolations();
            return new PmdInspection(config.project(), violations.stream().map(PmdIssue::new).collect(toList()));
        }

    }

    private List<Path> getFiles() {
        final Build build = config.project().getBuild();
        final Path sources = Paths.get(build.getSourceDirectory());
        final Path testSources = Paths.get(build.getTestSourceDirectory());
        return Stream.of(sources, testSources)
            .filter(Files::exists)
            .flatMap(PmdInspector::getFiles)
            .filter(Files::isRegularFile)
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
        return List.of(new RuleSetLoader().loadFromResource(config.toString()));
    }

    private static Stream<String> getRules(final List<RuleSet> ruleSets) {
        return ruleSets.stream().flatMap(rs -> rs.getRules().stream()).map(Rule::getName).sorted();
    }

    private static PMDConfiguration createPmdConfiguration(final PmdConfig config) {
        final PMDConfiguration configuration = new PMDConfiguration();
        final Path targetPath = Paths.get(config.targetPath());
        configuration.setAnalysisCacheLocation(targetPath.resolve("pmd.cache").toString());
        configuration.addInputPath(Paths.get(config.project().getBuild().getSourceDirectory()));
        configuration.addInputPath(Paths.get(config.project().getBuild().getTestSourceDirectory()));
        configuration.prependAuxClasspath(targetPath.resolve("classes").toString());
        return configuration;
    }

}

package ch.acanda.maven.coan.pmd;

import ch.acanda.maven.coan.Analysis;
import ch.acanda.maven.coan.PmdIssue;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetLoader;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.SummaryHTMLRenderer;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.FileDataSource;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("PMD.ExcessiveImports")
public class PmdAnalyser {

    private final PmdConfig config;
    private final Log log;

    public PmdAnalyser(final PmdConfig config) {
        this.config = config;
        log = config.getLog();
    }

    public Analysis analyse() throws MojoFailureException {
        Logger.getLogger("net.sourceforge.pmd").setLevel(Level.OFF);

        final Path configPath = getConfig(config.getProject(), new ArrayList<>());
        log.debug("Found PMD configPath at " + configPath + ".");

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
        try {
            final Renderer htmlRenderer = createHtmlRenderer(targetPath);
            htmlRenderer.start();
            final Report report = PMD.processFiles(configuration, ruleSets, files, List.of(htmlRenderer));
            htmlRenderer.end();
            htmlRenderer.flush();
            final List<RuleViolation> violations = report.getViolations();
            return new PmdAnalysis(violations.stream().map(PmdIssue::new).collect(toList()));
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to write report.", e);
        }
    }

    private List<DataSource> getFiles() {
        final Build build = config.getProject().getBuild();
        final Path sources = Paths.get(build.getSourceDirectory());
        final Path testSources = Paths.get(build.getTestSourceDirectory());
        return Stream.of(sources, testSources)
                .filter(Files::exists)
                .flatMap(this::getFiles)
                .filter(Files::isRegularFile)
                .map(path -> new FileDataSource(path.toFile()))
                .collect(toList());
    }

    private Stream<Path> getFiles(final Path directory) {
        try {
            return Files.walk(directory);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to collect source files.", e);
        }
    }

    private Path getConfig(final MavenProject project, final List<Path> failed) throws MojoFailureException {
        final Path baseDir = project.getBasedir().toPath();
        final Path config = baseDir.resolve(this.config.getConfigPath());
        if (Files.exists(config)) {
            if (Files.isReadable(config)) {
                return config;
            } else {
                log.warn(config + " exists but is not readable.");
            }
        }
        failed.add(config);
        if (project.hasParent()) {
            return getConfig(project.getParent(), failed);
        }
        throw new MojoFailureException("Unable to find PMD configuration.");
    }

    private List<RuleSet> loadRuleSets(final Path config) {
        final RuleSetLoader loader = new RuleSetLoader();
        loader.enableCompatibility(false);
        return List.of(loader.loadFromResource(config.toString()));
    }

    private Stream<String> getRules(final List<RuleSet> ruleSets) {
        return ruleSets.stream().flatMap(rs -> rs.getRules().stream()).map(Rule::getName).sorted();
    }

    private PMDConfiguration createPmdConfiguration(final Path targetPath) {
        final PMDConfiguration configuration = new PMDConfiguration();
        configuration.setAnalysisCacheLocation(targetPath.resolve("pmd.cache").toString());
        return configuration;
    }

    private Renderer createHtmlRenderer(final Path buildDir) throws MojoFailureException {
        try {
            Files.createDirectories(buildDir);
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to create directory " + buildDir + ".", e);
        }
        try {
            final SummaryHTMLRenderer renderer = new SummaryHTMLRenderer();
            renderer.setWriter(Files.newBufferedWriter(buildDir.resolve("pmd-report.html"), StandardCharsets.UTF_8));
            return renderer;
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to create report.", e);
        }
    }

}

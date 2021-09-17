package ch.acanda.maven.coan.pmd;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetLoader;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.XMLRenderer;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.FileDataSource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Mojo(name = "pmd-report", defaultPhase = LifecyclePhase.VERIFY)
public class PmdReportMojo extends AbstractMojo {

    private static final String CONFIG_DEFAULT_VALUE = "config/pmd.xml";

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "coan.pmd.config", required = true, defaultValue = CONFIG_DEFAULT_VALUE)
    private String config;

    @Override
    public void execute() throws MojoFailureException {
        final Path config = getConfig(project, new ArrayList<>());
        getLog().debug("Found PMD config at " + config + ".");
        final List<RuleSet> ruleSets = loadRuleSets(config);
        if (getLog().isDebugEnabled()) {
            getLog().debug("Active rules: " + getRules(ruleSets).collect(joining(", ", "", ".")));
        }
        final Path buildDir = Paths.get(project.getBuild().getDirectory()).resolve("code-analysis-maven-plugin");
        final PMDConfiguration configuration = createPmdConfiguration(buildDir);
        final List<DataSource> files = getFiles();
        if (getLog().isDebugEnabled()) {
            final String names = files.stream().map(ds -> ds.getNiceFileName(false, null)).collect(joining("\n  "));
            getLog().debug("Source files:\n  " + names);
        }
        try {
            final Renderer htmlRenderer = createHtmlRenderer(buildDir);
            htmlRenderer.start();
            final List<Renderer> renderers = List.of(htmlRenderer, new DebugRenderer(getLog()));
            final Report report = PMD.processFiles(configuration, ruleSets, files, renderers);
            htmlRenderer.end();
            htmlRenderer.flush();
            final List<RuleViolation> violations = report.getViolations();
            if (violations.isEmpty()) {
                getLog().info("You have 0 PMD violations.");
            } else {
                getLog().warn("You have " + violations.size() + " PMD violations.");
            }
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to write report.", e);
        }
    }

    // False positive: the streams created by Files.walk(...) are closed by the stream of Stream.concat(...).
    @SuppressWarnings("java:S2095")
    private List<DataSource> getFiles() throws MojoFailureException {
        final Path sources = Paths.get(project.getBuild().getSourceDirectory());
        final Path testSources = Paths.get(project.getBuild().getTestSourceDirectory());
        try {
            return Stream.concat(Files.walk(sources), Files.walk(testSources))
                .filter(Files::isRegularFile)
                .map(path -> new FileDataSource(path.toFile()))
                .collect(Collectors.toList());
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to collect source files.", e);
        }
    }

    private Path getConfig(final MavenProject project, final List<Path> failed) throws MojoFailureException {
        final Path baseDir = project.getBasedir().toPath();
        final Path config = baseDir.resolve(this.config);
        if (Files.exists(config)) {
            if (Files.isReadable(config)) {
                return config;
            } else {
                getLog().warn(config + " exists but is not readable.");
            }
        }
        failed.add(config);
        if (project.hasParent()) {
            return getConfig(project.getParent(), failed);
        }
        throw new MojoFailureException("Unable to find PMD configuration.");
    }

    private static List<RuleSet> loadRuleSets(final Path config) {
        final RuleSetLoader loader = new RuleSetLoader();
        loader.enableCompatibility(false);
        return List.of(loader.loadFromResource(config.toString()));
    }

    private static Stream<String> getRules(final List<RuleSet> ruleSets) {
        return ruleSets.stream().flatMap(rs -> rs.getRules().stream()).map(Rule::getName).sorted();
    }

    private static PMDConfiguration createPmdConfiguration(final Path buildDir) throws MojoFailureException {
        final PMDConfiguration configuration = new PMDConfiguration();
        configuration.setAnalysisCacheLocation(buildDir.resolve("pmd.cache").toString());
        return configuration;
    }

    private static Renderer createHtmlRenderer(final Path buildDir) throws MojoFailureException {
        try {
            Files.createDirectories(buildDir);
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to create directory " + buildDir + ".", e);
        }

        try {
            final XMLRenderer renderer = new XMLRenderer();
            renderer.setWriter(Files.newBufferedWriter(buildDir.resolve("pmd-report.xml"), StandardCharsets.UTF_8));
            return renderer;
        } catch (final IOException e) {
            throw new MojoFailureException("Failed to create report.", e);
        }
    }

}

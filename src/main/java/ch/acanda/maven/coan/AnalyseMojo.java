package ch.acanda.maven.coan;

import ch.acanda.maven.coan.checkstyle.CheckstyleInspector;
import ch.acanda.maven.coan.pmd.PmdInspector;
import ch.acanda.maven.coan.report.LogReport;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Mojo(name = "analyse", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class AnalyseMojo extends AbstractCoanMojo {

    @Override
    protected void analyseCode() throws MojoFailureException {
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            final MavenProject project = getProject();
            final PmdInspector pmdInspector = new PmdInspector(assemblePmdConfig(project));
            final CheckstyleInspector checkstyleInspector = new CheckstyleInspector(assembleCheckstyleConfig(project));

            final Future<Inspection> pmdFuture = executorService.submit(pmdInspector::inspect);
            final Future<Inspection> checkstyleFuture = executorService.submit(checkstyleInspector::inspect);

            final Inspection pmdInspection = pmdFuture.get();
            final Inspection checkstyleInspection = checkstyleFuture.get();
            executorService.shutdown();

            LogReport.report(pmdInspection, project.getBasedir().toPath(), getLog());
            LogReport.report(checkstyleInspection, project.getBasedir().toPath(), getLog());
            createReports(pmdInspection, checkstyleInspection);
            failOnIssues(pmdInspection, checkstyleInspection);

        } catch (final ExecutionException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private void failOnIssues(final Inspection pmdInspection, final Inspection checkstyleInspection)
        throws MojoFailureException {
        if (isFailOnIssues() && (pmdInspection.foundIssues() || checkstyleInspection.foundIssues())) {
            final String numberOfToolIssues = Stream.of(pmdInspection, checkstyleInspection)
                .filter(Inspection::foundIssues)
                .map(AnalyseMojo::numberOfToolIssues)
                .collect(joining(" and "));
            throw new MojoFailureException("Code analysis found " + numberOfToolIssues + ".");
        }
    }

    private static String numberOfToolIssues(final Inspection inspection) {
        final int count = inspection.getNumberOfIssues();
        final String noun = count == 1 ? "issue" : "issues";
        return count + " " + inspection.toolName() + " " + noun;
    }

}

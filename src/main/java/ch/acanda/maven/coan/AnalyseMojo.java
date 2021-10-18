package ch.acanda.maven.coan;

import ch.acanda.maven.coan.checkstyle.CheckstyleAnalyser;
import ch.acanda.maven.coan.pmd.PmdAnalyser;
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

@Mojo(name = "analyse", defaultPhase = LifecyclePhase.VERIFY)
public class AnalyseMojo extends AbstractCoanMojo {

    @Override
    protected void analyseCode() throws MojoFailureException {
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            final MavenProject project = getProject();
            final PmdAnalyser pmdAnalyser = new PmdAnalyser(assemblePmdConfig(project));
            final CheckstyleAnalyser checkstyleAnalyser = new CheckstyleAnalyser(assembleCheckstyleConfig(project));

            final Future<Analysis> pmdFuture = executorService.submit(pmdAnalyser::analyse);
            final Future<Analysis> checkstyleFuture = executorService.submit(checkstyleAnalyser::analyse);

            final Analysis pmdAnalysis = pmdFuture.get();
            final Analysis checkstyleAnalysis = checkstyleFuture.get();
            executorService.shutdown();

            LogReport.report(pmdAnalysis, project.getBasedir().toPath(), getLog());
            LogReport.report(checkstyleAnalysis, project.getBasedir().toPath(), getLog());
            createReports(pmdAnalysis, checkstyleAnalysis);
            failOnIssues(pmdAnalysis, checkstyleAnalysis);

        } catch (final ExecutionException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private void failOnIssues(final Analysis pmdAnalysis, final Analysis checkstyleAnalysis)
        throws MojoFailureException {
        if (isFailOnIssues() && (pmdAnalysis.foundIssues() || checkstyleAnalysis.foundIssues())) {
            final String numberOfToolIssues = Stream.of(pmdAnalysis, checkstyleAnalysis)
                .filter(Analysis::foundIssues)
                .map(AnalyseMojo::numberOfToolIssues)
                .collect(joining(" and "));
            throw new MojoFailureException("Code analysis found " + numberOfToolIssues + ".");
        }
    }

    private static String numberOfToolIssues(final Analysis analysis) {
        final int count = analysis.getNumberOfIssues();
        final String noun = count == 1 ? "issue" : "issues";
        return count + " " + analysis.getToolName() + " " + noun;
    }

}

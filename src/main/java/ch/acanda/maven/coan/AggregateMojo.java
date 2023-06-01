package ch.acanda.maven.coan;

import ch.acanda.maven.coan.checkstyle.CheckstyleInspector;
import ch.acanda.maven.coan.pmd.PmdInspector;
import ch.acanda.maven.coan.report.LogReport;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Mojo(name = "aggregate", aggregator = true, threadSafe = true)
public class AggregateMojo extends AbstractCoanMojo {

    @Parameter(property = "reactorProjects", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @Override
    protected void analyseCode() throws MojoFailureException {

        logReactorProjects();

        final List<Callable<Inspection>> analysers = reactorProjects.stream()
            .flatMap(reactorProject -> Stream.<Callable<Inspection>>of(
                () -> new PmdInspector(assemblePmdConfig(reactorProject)).inspect(),
                () -> new CheckstyleInspector(assembleCheckstyleConfig(reactorProject)).inspect()
            ))
            .collect(toList());

        final ExecutorService executorService = Executors.newFixedThreadPool(analysers.size());
        try {
            final List<Future<Inspection>> runningAnalyses = executorService.invokeAll(analysers, 1, TimeUnit.HOURS);
            final List<Inspection> analyses = runningAnalyses.stream()
                .map(AggregateMojo::waitUntilFinished)
                .collect(toList());

            analyses.forEach(analysis -> LogReport.report(analysis, getProject().getBasedir().toPath(), getLog()));
            createReports(analyses.toArray(Inspection[]::new));
            failOnIssues(analyses);

        } catch (final RejectedExecutionException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoFailureException(e.getMessage(), e);
        } catch (final AnalysisExecutionException e) {
            final Throwable cause = e.getCause();
            throw new MojoFailureException(cause.getMessage(), cause); //NOPMD
        } finally {
            executorService.shutdown();
        }

    }

    private void failOnIssues(final List<Inspection> analyses) throws MojoFailureException {
        final boolean foundIssues = analyses.stream().anyMatch(Inspection::foundIssues);
        if (isFailOnIssues() && foundIssues) {
            final long sum = analyses.stream().mapToInt(Inspection::getNumberOfIssues).sum();
            final String issues = sum == 1 ? " issue" : " issues";
            throw new MojoFailureException("Code analysis found " + sum + issues + ".");
        }
    }

    private void logReactorProjects() {
        if (getLog().isDebugEnabled()) {
            final String projects = reactorProjects.stream()
                .map(MavenProject::toString)
                .collect(joining("\n  ", "Projects:\n  ", ""));
            getLog().debug(projects);
        }
    }

    private static Inspection waitUntilFinished(final Future<Inspection> runningAnalysis) {
        try {
            return runningAnalysis.get(1, TimeUnit.HOURS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AnalysisExecutionException(e);
        } catch (final ExecutionException | TimeoutException e) {
            throw new AnalysisExecutionException(e);
        }
    }

}

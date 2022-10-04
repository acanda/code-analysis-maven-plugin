package ch.acanda.maven.coan.pmd;

import ch.acanda.maven.coan.Issue;
import net.sourceforge.pmd.RuleViolation;

import java.nio.file.Path;
import java.nio.file.Paths;

public record PmdIssue(RuleViolation violation) implements Issue {

    @Override
    public Path file() {
        return Paths.get(violation.getFilename());
    }

    @Override
    public int line() {
        return violation.getBeginLine();
    }

    @Override
    public int column() {
        return violation.getBeginColumn();
    }

    @Override
    public String name() {
        return violation.getRule().getName();
    }

    @Override
    public String description() {
        return violation.getDescription();
    }

    @Override
    public Severity severity() {
        return switch (violation.getRule().getPriority()) {
            case HIGH -> Severity.HIGHEST;
            case MEDIUM_HIGH -> Severity.HIGH;
            case MEDIUM -> Severity.MEDIUM;
            case MEDIUM_LOW -> Severity.LOW;
            case LOW -> Severity.LOWEST;
        };
    }

}

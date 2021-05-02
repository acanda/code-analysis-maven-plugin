package ch.acanda.maven.coan;

import lombok.RequiredArgsConstructor;
import net.sourceforge.pmd.RuleViolation;

import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor
public class PmdIssue implements Issue {

    private final RuleViolation violation;

    @Override
    public Path getFile() {
        return Paths.get(violation.getFilename());
    }

    @Override
    public int getLine() {
        return violation.getBeginLine();
    }

    @Override
    public int getColumn() {
        return violation.getBeginColumn();
    }

    @Override
    public String getName() {
        return violation.getRule().getName();
    }

    @Override
    public String getDescription() {
        return violation.getDescription();
    }

    @Override
    public Severity getSeverity() {
        switch (violation.getRule().getPriority()) {
            case HIGH:
                return Severity.HIGHEST;
            case MEDIUM_HIGH:
                return Severity.HIGH;
            case MEDIUM:
                return Severity.MEDIUM;
            case MEDIUM_LOW:
                return Severity.LOW;
            case LOW:
                return Severity.LOWEST;
            default:
                throw new IllegalStateException("Unexpected PMD rule priority: " + violation.getRule().getPriority());
        }
    }

}

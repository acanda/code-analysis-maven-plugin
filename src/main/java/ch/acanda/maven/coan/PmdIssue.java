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
    public String getName() {
        return violation.getRule().getName();
    }

    @Override
    public String getDescription() {
        return violation.getDescription();
    }

}

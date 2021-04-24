package ch.acanda.maven.coan.pmd;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;
import org.apache.maven.plugin.logging.Log;

import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.Iterator;

public class DebugRenderer extends AbstractIncrementingRenderer {

    private final Log log;

    public DebugRenderer(final Log log) {
        super("MavenDebugRenderer", "Renders violations to the Maven log.");
        this.log = log;
        setWriter(new OutputStreamWriter(System.out));
    }

    @Override
    public void renderFileViolations(final Iterator<RuleViolation> violations) {
        violations.forEachRemaining(violation -> {
            final String template = "%s [%s] %s: %s (%s:%d)";
            final Rule rule = violation.getRule();
            final String name = Paths.get(violation.getFilename()).getFileName().toString();
            final String content = String.format(template, violation.getFilename(), rule.getRuleSetName(),
                    rule.getName(), rule.getMessage(), name, violation.getBeginLine());
            log.error(content);
        });
    }

    @Override
    public String defaultFileExtension() {
        return null;
    }

}

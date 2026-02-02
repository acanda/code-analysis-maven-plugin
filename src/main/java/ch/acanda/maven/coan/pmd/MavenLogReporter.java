package ch.acanda.maven.coan.pmd;

import net.sourceforge.pmd.util.log.PmdReporter;
import org.apache.maven.plugin.logging.Log;
import org.slf4j.event.Level;

import java.text.MessageFormat;

class MavenLogReporter implements PmdReporter {

    private final Log log;
    private int errors;

    MavenLogReporter(final Log log) {
        this.log = log;
    }

    @Override
    public boolean isLoggable(final Level level) {
        return switch (level) {
            case ERROR -> log.isErrorEnabled();
            case WARN -> log.isWarnEnabled();
            case INFO -> log.isInfoEnabled();
            case DEBUG, TRACE -> log.isDebugEnabled();
        };
    }

    @Override
    public void logEx(final Level level, final String template, final Object[] formatArgs, final Throwable error) {
        if (template != null) {
            final String message = MessageFormat.format(template, formatArgs);
            switch (level) {
                case ERROR:
                    errors++;
                    log.error(message);
                    break;
                case WARN:
                    log.warn(message);
                    break;
                case INFO:
                    log.info(message);
                    break;
                default:
                    log.debug(message);
                    break;
            }
        }
    }

    @Override
    public int numErrors() {
        return errors;
    }

}

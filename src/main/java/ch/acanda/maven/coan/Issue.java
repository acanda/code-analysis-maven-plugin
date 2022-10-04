package ch.acanda.maven.coan;

import java.nio.file.Path;
import java.util.Locale;

public interface Issue {

    Path file();

    int line();

    int column();

    String name();

    String description();

    Severity severity();

    enum Severity {
        HIGHEST, HIGH, MEDIUM, LOW, LOWEST, IGNORE;

        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}

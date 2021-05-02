package ch.acanda.maven.coan;

import java.nio.file.Path;
import java.util.Locale;

public interface Issue {

    Path getFile();

    int getLine();

    int getColumn();

    String getName();

    String getDescription();

    Severity getSeverity();

    enum Severity {
        HIGHEST, HIGH, MEDIUM, LOW, LOWEST, IGNORE;

        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}

package ch.acanda.maven.coan;

import java.nio.file.Path;

public interface Issue {

    Path getFile();

    int getLine();

    String getName();

    String getDescription();

}

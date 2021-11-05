package ch.acanda.maven.coan;

import ch.acanda.maven.coan.version.Versions;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * The "versions" goal outputs the versions of the different tools.
 */
@Mojo(name = "versions", requiresProject = false, threadSafe = true)
public class VersionsMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoFailureException {
        getLog().info(Versions.PLUGIN_ARTIFACT_ID + " " + Versions.getPluginVersion());
        getLog().info("PMD " + Versions.getPmdVersion());
        getLog().info("Checkstyle " + Versions.getCheckstyleVersion());
    }

}

package ch.acanda.maven.coan;

import net.sourceforge.pmd.PMD;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The "versions" goal outputs the versions of the different tools.
 */
@Mojo(name = "versions", requiresProject = false, threadSafe = true)
public class VersionsMojo extends AbstractMojo {

    public static final String PLUGIN_GROUP_ID = "ch.acanda.maven";
    public static final String PLUGIN_ARTIFACT_ID = "code-analysis-maven-plugin";
    public static final String PMD_GROUP_ID = "net.sourceforge.pmd";
    public static final String PMD_ARTIFACT_ID = "pmd-core";

    @Override
    public void execute() throws MojoFailureException {
        getLog().info(PLUGIN_ARTIFACT_ID + " " + getVersion(VersionsMojo.class, PLUGIN_GROUP_ID, PLUGIN_ARTIFACT_ID));
        getLog().info("PMD " + getVersion(PMD.class, PMD_GROUP_ID, PMD_ARTIFACT_ID));
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private String getVersion(final Class<?> cls, final String groupId, final String artifactId)
            throws MojoFailureException {
        final String pomProperties = "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        try (InputStream in = cls.getResourceAsStream(pomProperties)) {
            final Properties props = new Properties();
            props.load(in);
            return props.getProperty("version");
        } catch (final IOException | NullPointerException e) {
            throw new MojoFailureException("Failed to read version for " + artifactId, e);
        }
    }

}

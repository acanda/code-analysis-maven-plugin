package ch.acanda.maven.coan.version;

import ch.acanda.maven.coan.VersionsMojo;
import com.puppycrawl.tools.checkstyle.Main;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.sourceforge.pmd.PmdAnalysis;
import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Versions {

    public static final String PLUGIN_ARTIFACT_ID = "code-analysis-maven-plugin";
    private static final String PLUGIN_GROUP_ID = "ch.acanda.maven";
    private static final String PMD_GROUP_ID = "net.sourceforge.pmd";
    private static final String PMD_ARTIFACT_ID = "pmd-core";
    private static final String CHECKSTYLE_GROUP_ID = "com.puppycrawl.tools";
    private static final String CHECKSTYLE_ARTIFACT_ID = "checkstyle";

    public static String getPluginVersion() throws MojoFailureException {
        return getVersion(VersionsMojo.class, PLUGIN_GROUP_ID, PLUGIN_ARTIFACT_ID);
    }

    public static String getPmdVersion() throws MojoFailureException {
        return getVersion(PmdAnalysis.class, PMD_GROUP_ID, PMD_ARTIFACT_ID);
    }

    public static String getCheckstyleVersion() throws MojoFailureException {
        return getVersion(Main.class, CHECKSTYLE_GROUP_ID, CHECKSTYLE_ARTIFACT_ID);
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private static String getVersion(final Class<?> cls, final String groupId, final String artifactId)
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

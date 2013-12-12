package org.mosaic.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mosaic.launcher.SystemError.bootstrapError;

/**
 * @author arik
 */
final class SystemPackages
{
    private static final Logger LOG = LoggerFactory.getLogger( SystemPackages.class );

    private static final Pattern ARTIFACT_VERSION_PATTERN = Pattern.compile( "\\$\\{(.+):(.+)\\}" );

    private static final String extraSystemPackages;

    static
    {
        Map<String, String> packages = new LinkedHashMap<>();
        readPackages( packages, "extra.systempackages.properties" );
        extraSystemPackages = createSpecForPackages( packages, true );
        LOG.trace( "Extra system packages: {}", SystemPackages.extraSystemPackages );
    }

    public static String getExtraSystemPackages()
    {
        return SystemPackages.extraSystemPackages;
    }

    private static void readPackages( @Nonnull Map<String, String> properties, @Nonnull String path )
    {
        // extra packages exported by boot delegation & the system bundle
        URL sysPkgResource = SystemPackages.class.getResource( path );
        if( sysPkgResource == null )
        {
            throw bootstrapError( "Could not find system packages file at '{}' - cannot boot server", path );
        }

        Properties tempProperties = new Properties();
        try( InputStream is = sysPkgResource.openStream() )
        {
            tempProperties.load( is );
            for( String packageName : tempProperties.stringPropertyNames() )
            {
                properties.put( packageName, tempProperties.getProperty( packageName ) );
            }
        }
        catch( IOException e )
        {
            throw bootstrapError( "Cannot read system packages from '{}': {}", sysPkgResource, e.getMessage(), e );
        }
    }

    private static String createSpecForPackages( @Nonnull Map<String, String> packages, boolean withVersions )
    {
        StringBuilder buffer = new StringBuilder( 2000 );
        for( Map.Entry<String, String> entry : packages.entrySet() )
        {
            String packageName = entry.getKey().trim();

            String version = packageName.startsWith( "javax.annotation" ) ? "2.1.0" : entry.getValue();
            if( entry.getValue() == null )
            {
                LOG.warn( "Null version for package '{}' detected", packageName );
                continue;
            }
            version = version.trim();

            Matcher matcher = ARTIFACT_VERSION_PATTERN.matcher( version );
            if( matcher.matches() )
            {
                version = getArtifactVersion( matcher.group( 1 ), matcher.group( 2 ) );
            }

            if( buffer.length() > 0 )
            {
                buffer.append( "," );
            }

            if( withVersions )
            {
                buffer.append( packageName ).append( ";version=" ).append( version );
            }
            else
            {
                buffer.append( packageName );
            }
        }
        return buffer.toString();
    }

    @Nonnull
    private static String getArtifactVersion( @Nonnull String groupId, @Nonnull String artifactId )
    {
        String path = "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        URL resource = SystemPackages.class.getResource( path );
        if( resource == null )
        {
            throw bootstrapError( "could not find Maven properties file for '" + groupId + ":" + artifactId + "' in classpath under '" + path + "'" );
        }

        Properties properties = new Properties();
        try( InputStream inputStream = resource.openStream() )
        {
            properties.load( inputStream );
        }
        catch( IOException e )
        {
            throw bootstrapError( "could not read Maven properties file for '" + groupId + ":" + artifactId + "' in classpath under '" + path + "'", e );
        }

        String version = properties.getProperty( "version" );
        if( version == null || version.isEmpty() )
        {
            throw bootstrapError( "empty version in properties file for " + groupId + ":" + artifactId + "' in classpath under '" + path + "'" );
        }

        return version;
    }

    private SystemPackages()
    {
    }
}

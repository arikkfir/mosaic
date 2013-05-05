package org.mosaic.launcher.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.launcher.MosaicBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.nio.file.Files.*;
import static org.mosaic.launcher.util.SystemError.bootstrapError;

/**
 * @author arik
 */
public final class Utils
{
    private static final Logger LOG = LoggerFactory.getLogger( Utils.class );

    private static final String XX_USE_SPLIT_VERIFIER = "-XX:-UseSplitVerifier";

    @Nonnull
    public static String getArtifactVersion( @Nonnull String groupId,
                                             @Nonnull String artifactId,
                                             @Nonnull String defaultVersion )
    {
        String version = getArtifactVersion( groupId, artifactId );
        return version == null ? defaultVersion : version;
    }

    @Nullable
    public static String getArtifactVersion( @Nonnull String groupId, @Nonnull String artifactId )
    {
        URL propertiesUrl = Utils.class.getResource( String.format( "/META-INF/maven/%s/%s/pom.properties", groupId, artifactId ) );
        if( propertiesUrl == null )
        {
            return null;
        }

        Properties properties = new Properties();
        try( InputStream is = propertiesUrl.openStream() )
        {
            properties.load( is );
            return properties.getProperty( "version" );
        }
        catch( IOException e )
        {
            LOG.warn( "Could not discover version of artifact '{}:{}': {}", groupId, artifactId, e.getMessage(), e );
            return null;
        }
    }

    @Nonnull
    public static URL requireClasspathResource( @Nonnull Properties properties,
                                                @Nonnull String propertyOverrideName,
                                                @Nonnull String path )
    {
        String pathOverride = properties.getProperty( propertyOverrideName );
        if( pathOverride != null )
        {
            Path filePath = Paths.get( pathOverride );
            if( !Files.exists( filePath ) )
            {
                throw bootstrapError( "Path '%s' does not exist", pathOverride );
            }
            else if( !Files.isRegularFile( filePath ) )
            {
                throw bootstrapError( "Path '%s' is not a file", pathOverride );
            }
            else if( !Files.isReadable( filePath ) )
            {
                throw bootstrapError( "Path '%s' is not readable", pathOverride );
            }
            else
            {
                try
                {
                    return filePath.toUri().toURL();
                }
                catch( MalformedURLException e )
                {
                    throw bootstrapError( "Could not convert path '%s' to a URL: %s", e, filePath, e.getMessage() );
                }
            }
        }
        else
        {
            URL url = MosaicBuilder.class.getResource( path );
            if( url == null )
            {
                throw bootstrapError( "Could not find classpath resource '%s'", path );
            }
            else
            {
                return url;
            }
        }
    }

    public static void assertJvmSplitVerifierIsUsed()
    {
        LOG.debug( "Verifying JVM split-verifier is used (required for bytecode weaving)" );
        for( String arg : getRuntimeMXBean().getInputArguments() )
        {
            if( arg.contains( XX_USE_SPLIT_VERIFIER ) )
            {
                return;
            }
        }
        throw bootstrapError(
                "The JVM split verifier argument has not been specified.\n" +
                "The JVM split verifier is required to enable bytecode \n" +
                "weaving by the Mosaic server.\n" +
                "Please provide the argument to the JVM command line:\n" +
                "    java ... %s ...",
                XX_USE_SPLIT_VERIFIER
        );
    }

    @Nonnull
    public static String discoverMosaicVersion()
    {
        LOG.debug( "Discovering Mosaic version" );
        URL mosaicPropertiesResource = MosaicBuilder.class.getResource( "/mosaic.properties" );
        if( mosaicPropertiesResource == null )
        {
            throw bootstrapError( "Incomplete Mosaic installation - could not find 'mosaic.properties' file." );
        }

        Properties mosaicVersionProperties = new Properties();
        try( InputStream input = mosaicPropertiesResource.openStream() )
        {
            mosaicVersionProperties.load( input );
            return mosaicVersionProperties.getProperty( "mosaic.version" );
        }
        catch( IOException e )
        {
            throw bootstrapError( "Could not read from '%s': %s", e, mosaicPropertiesResource, e.getMessage() );
        }
    }

    public static void deleteContents( @Nonnull Path path )
    {
        try( DirectoryStream<Path> stream = newDirectoryStream( path ) )
        {
            for( Path child : stream )
            {
                deletePath( child );
            }
        }
        catch( IOException e )
        {
            throw bootstrapError( "Could not delete contents of directory at '" + path + "': " + e.getMessage(), e );
        }
    }

    public static void deletePath( @Nonnull Path path ) throws IOException
    {
        if( Files.isDirectory( path ) )
        {
            try( DirectoryStream<Path> stream = Files.newDirectoryStream( path ) )
            {
                for( Path child : stream )
                {
                    deletePath( child );
                }
            }
        }
        Files.delete( path );
    }

    @Nonnull
    public static Path resolveDirectoryInHome( @Nonnull Properties properties,
                                               @Nonnull Path home,
                                               @Nonnull String name )
    {
        Path dir;

        String customLocation = properties.getProperty( "mosaic.home." + name );
        dir = home.resolve( customLocation == null ? name : customLocation ).normalize();

        if( !exists( dir ) )
        {
            try
            {
                dir = Files.createDirectories( dir );
            }
            catch( IOException e )
            {
                throw bootstrapError( "Could not create directory at '%s': %s", e, dir, e.getMessage() );
            }
        }
        else if( !isDirectory( dir ) )
        {
            throw bootstrapError( "Path at '%s' is not a directory", dir );
        }

        properties.setProperty( "mosaic.home." + name, dir.toString() );
        return dir;
    }
}

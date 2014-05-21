package org.mosaic.launcher;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;
import static org.mosaic.launcher.SystemError.bootstrapError;

/**
 * @author arik
 */
final class SystemPackages
{
    private static final Logger LOG = LoggerFactory.getLogger( SystemPackages.class );

    private static final Pattern VERSION = Pattern.compile( "version=\"?(.+)\"?" );

    private static final String extraSystemPackages;

    private static final List<String> BOOT_PACKAGES = asList(
            "com.sun.jdi",
            "com.sun.jdi.connect",
            "com.sun.jdi.event",
            "com.sun.jdi.request",
            "sun.reflect"
    );

    static
    {
        URL exportedPackagesFile = SystemPackages.class.getResource( "/exported-packages.txt" );
        if( exportedPackagesFile == null )
        {
            throw new IllegalStateException( "could not find /exported-packages.txt file in classpath" );
        }

        try( BufferedReader reader = new BufferedReader( new InputStreamReader( exportedPackagesFile.openStream() ) ) )
        {
            Map<String, String> packages = new LinkedHashMap<>();
            for( String packageName : BOOT_PACKAGES )
            {
                packages.put( packageName, "0.0.0" );
            }

            String packageName;
            while( ( packageName = reader.readLine() ) != null )
            {
                packages.put( packageName, getVersionForPackage( packageName.trim() ) );
            }

            extraSystemPackages = createSpecForPackages( packages, true );
        }
        catch( IOException e )
        {
            throw bootstrapError( "could not read system packages from '{}': {}", exportedPackagesFile, e.getMessage(), e );
        }
    }

    public static String getExtraSystemPackages()
    {
        return SystemPackages.extraSystemPackages;
    }

    private static String getVersionForPackage( String packageName ) throws IOException
    {
        for( String token : ManagementFactory.getRuntimeMXBean().getClassPath().split( Pattern.quote( File.pathSeparator ) ) )
        {
            Path path = Paths.get( token );
            if( Files.isDirectory( path ) )
            {
                String version = getVersionForPackageInPath( path, packageName );
                if( version != null )
                {
                    return version;
                }
            }
            else
            {
                URI uri = URI.create( "jar:file:" + path.toUri().getPath() );
                try( FileSystem fileSystem = FileSystems.newFileSystem( uri, Collections.<String, Object>emptyMap() ) )
                {
                    Path root = fileSystem.getRootDirectories().iterator().next();
                    String version = getVersionForPackageInPath( root, packageName );
                    if( version != null )
                    {
                        return version;
                    }
                }
            }
        }

        throw new IllegalStateException( "could not find version of package '" + packageName + "'" );
    }

    private static String getVersionForPackageInPath( Path root, String packageName ) throws IOException
    {
        Path packageDir = root.resolve( packageName.replace( '.', '/' ) );
        if( Files.isDirectory( packageDir ) )
        {
            Path manifestFile = root.resolve( "META-INF/MANIFEST.MF" );
            if( Files.isRegularFile( manifestFile ) )
            {
                try( InputStream inputStream = Files.newInputStream( manifestFile ) )
                {
                    Manifest manifest = new Manifest( inputStream );
                    String exportPackageDirective = manifest.getMainAttributes().getValue( "Export-Package" );
                    if( exportPackageDirective != null )
                    {
                        for( String exportedPackage : exportPackageDirective.split( "," ) )
                        {
                            int semiColonIndex = exportedPackage.indexOf( ';' );
                            if( semiColonIndex > 0 )
                            {
                                String directives = exportedPackage.substring( semiColonIndex + 1 );
                                for( String directive : directives.split( ";" ) )
                                {
                                    Matcher matcher = VERSION.matcher( directive );
                                    if( matcher.matches() )
                                    {
                                        return matcher.group( 1 );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String createSpecForPackages( Map<String, String> packages, boolean withVersions )
    {
        StringBuilder buffer = new StringBuilder( 2000 );
        for( Map.Entry<String, String> entry : packages.entrySet() )
        {
            String packageName = entry.getKey().trim();

            String version = entry.getValue();
            if( version == null )
            {
                LOG.warn( "Null version for package '{}' detected", packageName );
                continue;
            }
            version = version.trim();

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

    private SystemPackages()
    {
    }
}

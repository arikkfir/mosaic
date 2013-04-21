package org.mosaic.launcher.home;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nonnull;

import static org.mosaic.launcher.SystemError.bootstrapError;

/**
 * @author arik
 */
public class HomeResolver
{
    @Nonnull
    public static Path home;

    @Nonnull
    public static Path apps;

    @Nonnull
    public static Path boot;

    @Nonnull
    public static Path etc;

    @Nonnull
    public static Path lib;

    @Nonnull
    public static Path logs;

    @Nonnull
    public static Path work;

    public static void initServerHome()
    {
        home = resolveServerHome();
        apps = resolveHomeDirectory( home, "apps" );
        boot = resolveHomeDirectory( home, "boot" );
        etc = resolveHomeDirectory( home, "etc" );
        lib = resolveHomeDirectory( home, "lib" );
        logs = resolveHomeDirectory( home, "logs" );
        work = resolveHomeDirectory( home, "work" );

        if( !Boolean.getBoolean( "dev" ) )
        {
            // when in dev mode, both the stdout AND the logging output go to the console, so avoid printing this twice
            System.out.println();
            for( String line : getWelcomeLines() )
            {
                System.out.println( line );
            }
            System.out.println();
        }
    }

    @Nonnull
    public static List<String> getWelcomeLines()
    {
        List<String> logoLines = printMosaicLogo();
        List<String> infoLines = Arrays.asList(
                "",
                String.format( "Mosaic server, version %s:", resolveServerVersion() ),
                "---------------------------------------------",
                "",
                String.format( "Home..................%s", home ),
                String.format( "Apps..................%s", apps ),
                String.format( "Boot..................%s", boot ),
                String.format( "Configurations (etc)..%s", etc ),
                String.format( "Bundles (lib).........%s", lib ),
                String.format( "Logs..................%s", logs ),
                String.format( "--------------------------------------------------" ),
                String.format( "Launch time...........%s", new Date() ),
                ""
        );
        List<String> lines = new LinkedList<>();
        lines.add( "**********************************************************************************************************************" );

        int i = 0;
        while( i < logoLines.size() || i < infoLines.size() )
        {
            String logoLine = i < logoLines.size() ? logoLines.get( i ) : "";
            while( logoLine.length() < 34 )
            {
                logoLine += " ";
            }

            String infoLine = i < infoLines.size() ? infoLines.get( i ) : "";
            lines.add( logoLine + infoLine );
            i++;
        }

        lines.add( "**********************************************************************************************************************" );
        return lines;
    }

    @Nonnull
    private static Path resolveServerHome()
    {
        Path home;

        String homePath = System.getProperty( "mosaic.home" );
        if( homePath != null )
        {
            home = FileSystems.getDefault().getPath( homePath );
            if( !home.isAbsolute() )
            {
                Path userHome = FileSystems.getDefault().getPath( System.getProperty( "user.dir" ) );
                home = userHome.resolve( home );
            }
        }
        else
        {
            home = FileSystems.getDefault().getPath( System.getProperty( "user.dir" ) );
        }
        home = home.normalize();

        if( !Files.exists( home ) )
        {
            throw bootstrapError( "Mosaic home directory at '%s' does not exist.", home );
        }
        else if( !Files.isDirectory( home ) )
        {
            throw bootstrapError( "Path for Mosaic home directory at '%s' is not a directory.", home );
        }

        System.setProperty( "mosaic.home", home.toString() );
        return home;
    }

    @Nonnull
    private static Path resolveHomeDirectory( @Nonnull Path parent, @Nonnull String name )
    {
        Path dir;

        String customLocation = System.getProperty( "mosaic.home." + name );
        dir = parent.resolve( customLocation == null ? name : customLocation ).normalize();

        if( !Files.exists( dir ) )
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
        else if( !Files.isDirectory( dir ) )
        {
            throw bootstrapError( "Path at '%s' is not a directory", dir );
        }

        System.setProperty( "mosaic.home." + name, dir.toString() );
        return dir;
    }

    @Nonnull
    private static String resolveServerVersion()
    {
        //mosaic.version
        String version = System.getProperty( "mosaic.version" );
        if( version != null )
        {
            return version;
        }

        URL mosaicPropertiesResource = HomeResolver.class.getResource( "/mosaic.properties" );
        if( mosaicPropertiesResource == null )
        {
            throw bootstrapError( "Incomplete Mosaic installation - could not find 'mosaic.properties' file." );
        }

        Properties properties = new Properties();
        try( InputStream input = mosaicPropertiesResource.openStream() )
        {
            properties.load( input );
            version = properties.getProperty( "mosaic.version" );
            System.setProperty( "mosaic.version", version );
            return version;
        }
        catch( IOException e )
        {
            throw bootstrapError( "Could not read from '%s': %s", e, mosaicPropertiesResource, e.getMessage() );
        }
    }

    @Nonnull
    private static List<String> printMosaicLogo()
    {
        URL mosaicLogoResource = HomeResolver.class.getResource( "/logo.txt" );
        if( mosaicLogoResource != null )
        {
            try( BufferedReader reader = new BufferedReader( new InputStreamReader( mosaicLogoResource.openStream() ) ) )
            {
                List<String> lines = new ArrayList<>();
                String line;
                while( ( line = reader.readLine() ) != null )
                {
                    lines.add( line );
                }
                return lines;
            }
            catch( IOException e )
            {
                throw bootstrapError( "Incomplete Mosaic installation - could not read from 'logo.txt' file." );
            }
        }
        return Collections.emptyList();
    }
}

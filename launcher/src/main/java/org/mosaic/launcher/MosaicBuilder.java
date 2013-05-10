package org.mosaic.launcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javax.annotation.Nonnull;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static org.mosaic.launcher.util.SystemError.bootstrapError;
import static org.mosaic.launcher.util.Utils.assertJvmSplitVerifierIsUsed;
import static org.mosaic.launcher.util.Utils.discoverMosaicVersion;

/**
 * @author arik
 */
public class MosaicBuilder
{
    private static final String VERSION;

    static
    {
        // split-verifier JVM flag is required for Mosaic
        assertJvmSplitVerifierIsUsed();

        //mosaic.version
        VERSION = discoverMosaicVersion();
    }

    @Nonnull
    private final Properties properties;

    public MosaicBuilder( @Nonnull Properties properties )
    {
        this.properties = new Properties( properties );

        String userHome = this.properties.getProperty( "user.home" );
        if( userHome == null )
        {
            userHome = System.getProperty( "user.home" );
            this.properties.setProperty( "user.home", userHome );
        }

        String userDir = this.properties.getProperty( "user.dir" );
        if( userDir == null )
        {
            userDir = System.getProperty( "user.dir" );
            this.properties.setProperty( "user.dir", userDir );
        }

        this.properties.setProperty( "user.home", userHome );
        this.properties.setProperty( "user.dir", userDir );
        this.properties.setProperty( "mosaic.version", VERSION );
        this.properties.setProperty( "mosaic.home", resolveMosaicHome().toString() );
    }

    public MosaicBuilder setApps( @Nonnull Path apps )
    {
        this.properties.setProperty( "mosaic.home.apps", apps.toString() );
        return this;
    }

    public MosaicBuilder setEtc( @Nonnull Path etc )
    {
        this.properties.setProperty( "mosaic.home.etc", etc.toString() );
        return this;
    }

    public MosaicBuilder setLib( @Nonnull Path lib )
    {
        this.properties.setProperty( "mosaic.home.lib", lib.toString() );
        return this;
    }

    public MosaicBuilder setLogs( @Nonnull Path logs )
    {
        this.properties.setProperty( "mosaic.home.logs", logs.toString() );
        return this;
    }

    public MosaicBuilder setWork( @Nonnull Path work )
    {
        this.properties.setProperty( "mosaic.home.work", work.toString() );
        return this;
    }

    public MosaicInstance create()
    {
        return new MosaicInstance( this.properties );
    }

    private Path resolveMosaicHome()
    {
        Path home;
        if( this.properties.containsKey( "mosaic.home" ) )
        {
            home = Paths.get( this.properties.getProperty( "mosaic.home" ) );
            if( !home.isAbsolute() )
            {
                home = Paths.get( this.properties.getProperty( "user.dir" ) ).resolve( home );
            }
            home = home.normalize().toAbsolutePath();
        }
        else
        {
            home = Paths.get( this.properties.getProperty( "user.dir" ) ).normalize().toAbsolutePath();
        }
        if( !exists( home ) )
        {
            throw bootstrapError( "Mosaic home directory at '%s' does not exist.", home );
        }
        else if( !isDirectory( home ) )
        {
            throw bootstrapError( "Path for Mosaic home directory at '%s' is not a directory.", home );
        }
        return home;
    }
}

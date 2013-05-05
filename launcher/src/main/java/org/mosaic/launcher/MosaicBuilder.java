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
        this.properties.setProperty( "user.home", this.properties.getProperty( "user.home" ) );
        this.properties.setProperty( "user.dir", this.properties.getProperty( "user.dir" ) );
        this.properties.setProperty( "mosaic.version", VERSION );
        this.properties.setProperty( "mosaic.home", resolveMosaicHome( this.properties ).toString() );
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

    private Path resolveMosaicHome( Properties properties )
    {
        Path home;
        if( properties.containsKey( "mosaic.home" ) )
        {
            home = Paths.get( properties.getProperty( "mosaic.home" ) );
            if( !home.isAbsolute() )
            {
                home = Paths.get( properties.getProperty( "user.dir" ) ).resolve( home );
            }
            home = home.normalize().toAbsolutePath();
        }
        else
        {
            home = Paths.get( properties.getProperty( "user.dir" ) ).normalize().toAbsolutePath();
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

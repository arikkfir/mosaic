package org.mosaic.launcher;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javax.annotation.Nonnull;

import static org.mosaic.launcher.SystemError.bootstrapError;

/**
 * @author arik
 */
public final class MosaicConfigurationBuilder
{
    private static Path guessHome()
    {
        String mosaicHome = System.getProperty( "mosaic.home" );
        if( mosaicHome == null || mosaicHome.trim().isEmpty() )
        {
            throw new IllegalStateException( "Could not find system property 'mosaic.home'" );
        }
        return Paths.get( mosaicHome );
    }

    @Nonnull
    private final String version;

    private boolean devMode = Boolean.getBoolean( "devMode" ) || Boolean.getBoolean( "dev" );

    @Nonnull
    private Path home;

    @Nonnull
    private Path apps;

    @Nonnull
    private Path boot;

    @Nonnull
    private Path etc;

    @Nonnull
    private Path lib;

    @Nonnull
    private Path logs;

    @Nonnull
    private Path work;

    public MosaicConfigurationBuilder()
    {
        this( guessHome() );
    }

    public MosaicConfigurationBuilder( @Nonnull Path home )
    {
        URL mosaicPropertiesResource = MosaicConfigurationBuilder.class.getResource( "mosaic.properties" );
        if( mosaicPropertiesResource == null )
        {
            throw bootstrapError( "Incomplete Mosaic installation - could not find 'mosaic.properties' file." );
        }

        Properties mosaicVersionProperties = new Properties();
        try( InputStream input = mosaicPropertiesResource.openStream() )
        {
            mosaicVersionProperties.load( input );
            this.version = mosaicVersionProperties.getProperty( "mosaic.version" );
        }
        catch( Exception e )
        {
            throw bootstrapError( "Could not read from '{}': {}", mosaicPropertiesResource, e.getMessage(), e );
        }

        this.home = home;
        this.apps = this.home.resolve( "apps" );
        this.boot = this.home.resolve( "boot" );
        this.etc = this.home.resolve( "etc" );
        this.lib = this.home.resolve( "lib" );
        this.logs = this.home.resolve( "logs" );
        this.work = this.home.resolve( "work" );
    }

    @Override
    public MosaicConfigurationBuilder clone() throws CloneNotSupportedException
    {
        return ( MosaicConfigurationBuilder ) super.clone();
    }

    @Nonnull
    public MosaicConfigurationBuilder devMode()
    {
        this.devMode = true;
        return this;
    }

    @Nonnull
    public MosaicConfigurationBuilder apps( @Nonnull Path apps )
    {
        this.apps = apps;
        return this;
    }

    @Nonnull
    public MosaicConfigurationBuilder boot( @Nonnull Path boot )
    {
        this.boot = boot;
        return this;
    }

    @Nonnull
    public MosaicConfigurationBuilder etc( @Nonnull Path etc )
    {
        this.etc = etc;
        return this;
    }

    @Nonnull
    public MosaicConfigurationBuilder lib( @Nonnull Path lib )
    {
        this.lib = lib;
        return this;
    }

    @Nonnull
    public MosaicConfigurationBuilder logs( @Nonnull Path logs )
    {
        this.logs = logs;
        return this;
    }

    @Nonnull
    public MosaicConfigurationBuilder work( @Nonnull Path work )
    {
        this.work = work;
        return this;
    }

    public MosaicConfiguration build()
    {
        return new MosaicConfigurationImpl();
    }

    private class MosaicConfigurationImpl implements MosaicConfiguration
    {
        @Nonnull
        private final String version;

        private final boolean devMode;

        @Nonnull
        private final Path home;

        @Nonnull
        private final Path apps;

        @Nonnull
        private final Path boot;

        @Nonnull
        private final Path etc;

        @Nonnull
        private final Path lib;

        @Nonnull
        private final Path logs;

        @Nonnull
        private final Path work;

        public MosaicConfigurationImpl()
        {
            this.version = MosaicConfigurationBuilder.this.version;
            this.devMode = MosaicConfigurationBuilder.this.devMode;
            this.home = MosaicConfigurationBuilder.this.home;
            this.apps = MosaicConfigurationBuilder.this.apps;
            this.boot = MosaicConfigurationBuilder.this.boot;
            this.etc = MosaicConfigurationBuilder.this.etc;
            this.lib = MosaicConfigurationBuilder.this.lib;
            this.logs = MosaicConfigurationBuilder.this.logs;
            this.work = MosaicConfigurationBuilder.this.work;
        }

        @Nonnull
        @Override
        public String getVersion()
        {
            return this.version;
        }

        @Override
        public boolean isDevMode()
        {
            return this.devMode;
        }

        @Nonnull
        @Override
        public Path getHome()
        {
            return this.home;
        }

        @Nonnull
        @Override
        public Path getApps()
        {
            return this.apps;
        }

        @Nonnull
        @Override
        public Path getBoot()
        {
            return this.boot;
        }

        @Nonnull
        @Override
        public Path getEtc()
        {
            return this.etc;
        }

        @Nonnull
        @Override
        public Path getLib()
        {
            return this.lib;
        }

        @Nonnull
        @Override
        public Path getLogs()
        {
            return this.logs;
        }

        @Nonnull
        @Override
        public Path getWork()
        {
            return this.work;
        }
    }
}

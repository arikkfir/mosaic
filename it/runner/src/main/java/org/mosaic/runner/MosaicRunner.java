package org.mosaic.runner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.launcher.MosaicBuilder;
import org.mosaic.launcher.MosaicInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.createTempDirectory;

/**
 * @author arik
 */
public class MosaicRunner
{
    private static final Logger LOG = LoggerFactory.getLogger( MosaicRunner.class );

    private static final Charset UTF_8 = Charset.forName( "UTF-8" );

    private static final String INIT_SQL;

    static
    {
        URL sql01Url = Resources.getResource( MosaicRunner.class, "/sql/mosaic-init.sql" );
        if( sql01Url == null )
        {
            throw new IllegalStateException( "Could not locate SQL script '/sql/mosaic-init.sql'" );
        }
        try
        {
            INIT_SQL = Resources.toString( sql01Url, Charset.forName( "UTF-8" ) );
        }
        catch( IOException e )
        {
            throw new IllegalStateException( "Could not read SQL script '" + sql01Url + "': " + e.getMessage(), e );
        }
    }

    @Nonnull
    private final Path runnerDirectory;

    @Nonnull
    private final Path mosaicHome;

    @Nonnull
    private final Properties properties = new Properties();

    @Nullable
    private MosaicInstance mosaic;

    public MosaicRunner() throws IOException
    {
        this.runnerDirectory = createTempDirectory( Paths.get( System.getProperty( "user.dir" ) ), "mosaic" );
        this.mosaicHome = this.runnerDirectory.resolve( "server" );
    }

    public MosaicRunner( @Nonnull Properties properties ) throws IOException
    {
        this.runnerDirectory = createTempDirectory( Paths.get( System.getProperty( "user.dir" ) ), "mosaic" );
        this.mosaicHome = this.runnerDirectory.resolve( "server" );
        this.properties.putAll( properties );
    }

    public void addProperty( @Nonnull String name, @Nullable String value )
    {
        this.properties.setProperty( name, value );
    }

    public void start() throws IOException
    {
        LOG.info( "" );
        LOG.info( "" );
        LOG.info( "======================================================================================" );
        LOG.info( "Starting Mosaic at: {}", this.mosaicHome );

        Files.createDirectories( this.mosaicHome.resolve( "apps" ) );
        Files.createDirectories( this.mosaicHome.resolve( "etc" ) );
        Files.createDirectories( this.mosaicHome.resolve( "lib" ) );

        MosaicBuilder builder = new MosaicBuilder( this.properties, this.mosaicHome );
        this.mosaic = builder.create();
        this.mosaic.start();
    }

    public void stop()
    {
        if( this.mosaic != null )
        {
            this.mosaic.stop();
            this.mosaic = null;
        }
    }
}

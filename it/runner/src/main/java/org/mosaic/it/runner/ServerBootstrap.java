package org.mosaic.it.runner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.*;
import static org.apache.commons.io.FileUtils.deleteDirectory;

/**
 * @author arik
 */
public class ServerBootstrap
{
    private static final Logger LOG = LoggerFactory.getLogger( ServerBootstrap.class );

    @Nonnull
    private final Path home;

    public ServerBootstrap() throws IOException
    {
        URL distZipUrl = getClass().getResource( "/dist-bin.zip" );
        if( distZipUrl == null )
        {
            throw new IOException( "Mosaic distribution not found in integration tests runner JAR file" );
        }

        // clean our temp directory just in case
        Path zipDestDir = Files.createTempDirectory( "mosaic" );
        File zipDestDirIoFile = zipDestDir.toFile();
        zipDestDirIoFile.deleteOnExit();
        deleteDirectory( zipDestDirIoFile );
        createDirectories( zipDestDir );

        // extract the mosaic distribution
        try( ZipInputStream zin = new ZipInputStream( distZipUrl.openStream() ) )
        {
            ZipEntry zipEntry = zin.getNextEntry();
            while( zipEntry != null )
            {
                if( !zipEntry.isDirectory() )
                {
                    Path dest = zipDestDir.resolve( zipEntry.getName() );
                    createDirectories( dest.getParent() );
                    copy( zin, dest );
                }
                zipEntry = zin.getNextEntry();
            }
        }

        // find internal extracted dir
        try( DirectoryStream<Path> stream = Files.newDirectoryStream( zipDestDir, "mosaic-*" ) )
        {
            Path home = null;
            for( Path path : stream )
            {
                if( isDirectory( path ) )
                {
                    Path versionFile = path.resolve( "version" );
                    if( exists( versionFile ) && isRegularFile( versionFile ) && isReadable( versionFile ) )
                    {
                        home = path;
                        break;
                    }
                }
            }

            if( home != null )
            {
                this.home = home;
            }
            else
            {
                throw new IOException( "Could not find Mosaic directory in bundled Mosaic distribution zip file" );
            }
        }
        LOG.info( "Extracted Mosaic server instance to: {}", this.home );
    }

    @Nonnull
    public ServerBootstrap start() throws IOException, InterruptedException
    {
        LOG.info( "Starting Mosaic server at: {}", this.home );

        // start server process
        ProcessBuilder builder = new ProcessBuilder( "/bin/sh", this.home.resolve( "bin/mosaic.sh" ).toString(), "start" );
        builder.directory( this.home.toFile() );
        builder.environment().put( "MOSAIC_PID_FILE", this.home.resolve( "mosaic.pid" ).toString() );
        builder.environment().put( "MOSAIC_JAVA_OPTS", "-Xms1g -Xmx1g" );
        builder.start();

        // wait until it fully starts or aborts
        long start = System.currentTimeMillis();
        while( start + ( 1000 * 30 ) > System.currentTimeMillis() )
        {
            try
            {
                Thread.sleep( 1000 );
            }
            catch( InterruptedException e )
            {
                try
                {
                    shutdown();
                }
                catch( Exception ignore )
                {
                }
                throw e;
            }

            Path globalLogFile = this.home.resolve( "logs/global.log" );
            if( exists( globalLogFile ) )
            {
                String contents = new String( readAllBytes( globalLogFile ), "UTF-8" );
                if( contents.contains( "Mosaic server is running " ) )
                {
                    return this;
                }
            }
        }

        // failed to start?
        try
        {
            shutdown();
        }
        catch( Exception ignore )
        {
        }
        throw new IllegalStateException( "Mosaic server did not seem to start! (log file did not contain the line 'Mosaic server is running')" );
    }

    @Nonnull
    public ServerBootstrap shutdown() throws InterruptedException, IOException
    {
        LOG.info( "Stopping Mosaic server..." );

        // start server process
        ProcessBuilder builder = new ProcessBuilder( "/bin/sh", this.home.resolve( "bin/mosaic.sh" ).toString(), "stop" );
        builder.directory( this.home.toFile() );
        builder.environment().put( "MOSAIC_PID_FILE", this.home.resolve( "mosaic.pid" ).toString() );
        builder.environment().put( "MOSAIC_JAVA_OPTS", "-Xms1g -Xmx1g" );
        builder.start().waitFor();
        return this;
    }

    @Nonnull
    public ServerBootstrap deploy( @Nonnull Path jar ) throws IOException
    {
        LOG.info( "Deploying module at: {}", jar );
        copy( jar, this.home.resolve( "lib" ).resolve( jar.getFileName() ) );
        return this;
    }
}

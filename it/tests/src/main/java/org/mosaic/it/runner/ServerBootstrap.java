package org.mosaic.it.runner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerAcl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.*;
import static org.apache.commons.io.FileUtils.deleteDirectory;

/**
 * @author arik
 */
public class ServerBootstrap
{
    private static final Logger LOG = LoggerFactory.getLogger( ServerBootstrap.class );

    private static final Charset UTF_8 = Charset.forName( "UTF-8" );

    private static final String SQL_INIT_01;

    static
    {
        URL sql01Url = ServerBootstrap.class.getResource( "/sql/01.sql" );
        if( sql01Url == null )
        {
            throw new IllegalStateException( "Could not locate SQL script '01.sql'" );
        }
        try
        {
            SQL_INIT_01 = IOUtils.toString( sql01Url, "UTF-8" );
        }
        catch( IOException e )
        {
            throw new IllegalStateException( "Could not read SQL script '" + sql01Url + "': " + e.getMessage(), e );
        }
    }

    @Nonnull
    private final Path workDirectory;

    @Nonnull
    private final Path globalDatabaseDirectory;

    @Nonnull
    private final Path serverDirectory;

    @Nullable
    private Server databaseServer;

    public ServerBootstrap() throws IOException, SQLException
    {
        // create a temporary directory to extract the server in
        this.workDirectory = Files.createTempDirectory( "mosaic" );
        if( !Boolean.getBoolean( "preserveDatabase" ) )
        {
            this.workDirectory.toFile().deleteOnExit();
            System.out.println( "Mosaic runner work directory at '" + this.workDirectory + "' will be DELETED" );
        }
        else
        {
            System.out.println( "Mosaic runner work directory at '" + this.workDirectory + "' will be PRESERVED" );
        }
        deleteDirectory( this.workDirectory.toFile() );
        createDirectories( this.workDirectory );

        // initialize database directory
        this.globalDatabaseDirectory = this.workDirectory.resolve( "databases/main" );

        // extract the mosaic distribution
        this.serverDirectory = extractServer();

        // tells HSQLDB *not* to reconfigure log4j etc
        System.setProperty( "hsqldb.reconfig_logging", "false" );
    }

    @Nonnull
    public ServerBootstrap start() throws IOException, InterruptedException, SQLException
    {
        startHsqlDatabase();
        initializeHsqlDatabase();
        startMosaicServer();
        return this;
    }

    @Nonnull
    public ServerBootstrap connectToDatabase() throws IOException
    {
        // add database connectivity to 'etc' dir of server
        Path dbConfigFile = this.serverDirectory.resolve( "etc/database.properties" );
        Properties databaseProperties = new Properties();
        databaseProperties.setProperty( "main.initialSize", "1" );
        databaseProperties.setProperty( "main.password", "" );
        databaseProperties.setProperty( "main.url", "jdbc:hsqldb:hsql://localhost/main" );
        databaseProperties.setProperty( "main.username", "SA" );
        try( Writer writer = newBufferedWriter( dbConfigFile, UTF_8, CREATE, WRITE, TRUNCATE_EXISTING ) )
        {
            databaseProperties.store( writer, "Created automatically by Mosaic runner module for integration tests" );
        }
        return this;
    }

    @Nonnull
    public ServerBootstrap shutdown() throws InterruptedException, IOException
    {
        LOG.info( "Stopping Mosaic server..." );
        ProcessBuilder builder = new ProcessBuilder( "/bin/sh", this.serverDirectory.resolve( "bin/mosaic.sh" ).toString(), "stop" );
        builder.directory( this.serverDirectory.toFile() );
        builder.environment().put( "MOSAIC_PID_FILE", this.serverDirectory.resolve( "mosaic.pid" ).toString() );
        builder.environment().put( "MOSAIC_JAVA_OPTS", "-Xms1g -Xmx1g" );
        builder.start().waitFor();

        LOG.info( "Stopping database server..." );
        if( this.databaseServer != null )
        {
            this.databaseServer.shutdown();
        }
        this.databaseServer = null;

        return this;
    }

    @Nonnull
    public ServerBootstrap deploy( @Nonnull Path jar ) throws IOException
    {
        LOG.info( "Deploying module at: {}", jar );
        copy( jar, this.serverDirectory.resolve( "lib" ).resolve( jar.getFileName() ) );
        return this;
    }

    @Nonnull
    public ServerBootstrap deployTestModule( @Nonnull String index ) throws IOException
    {
        String testModuleFileName = "test-module-" + index + ".jar";
        URL testModuleUrl = ServerBootstrap.class.getResource( "/" + testModuleFileName );
        if( testModuleUrl == null )
        {
            throw new IllegalStateException( "Could not locate test module '" + testModuleFileName + "'" );
        }

        Path dest = this.serverDirectory.resolve( "lib" ).resolve( testModuleFileName );
        LOG.info( "Deploying test module '{}' to: {}", testModuleFileName, dest );
        try( InputStream in = testModuleUrl.openStream();
             OutputStream out = newOutputStream( dest, CREATE, WRITE, TRUNCATE_EXISTING ) )
        {
            IOUtils.copy( in, out );
        }

        return this;
    }

    public boolean checkFileExists( @Nonnull String path )
    {
        Path file = this.serverDirectory.resolve( path );
        return Files.exists( file );
    }

    public boolean checkFileContents( @Nonnull String path, @Nonnull String content ) throws IOException
    {
        Path file = this.serverDirectory.resolve( path );
        return Files.exists( file ) && new String( Files.readAllBytes( file ), "UTF-8" ).equals( content );
    }

    private void startHsqlDatabase() throws IOException
    {
        LOG.info( "Starting HSQL database server..." );
        HsqlProperties p = new HsqlProperties();
        p.setProperty( "server.database.0", "file:" + this.globalDatabaseDirectory );
        p.setProperty( "server.dbname.0", "main" );
        p.setProperty( "server.silent", "false" );
        p.setProperty( "server.remote_open", "true" );
        p.setProperty( "server.daemon", "true" );
        p.setProperty( "server.port", "9009" );
        p.setProperty( "sql.enforce_names", "true" );
        p.setProperty( "sql.enforce_refs", "true" );
        p.setProperty( "sql.enforce_size", "true" );
        p.setProperty( "sql.enforce_types", "true" );
        p.setProperty( "hsqldb.sqllog", "3" );// 3 means log everything
        Server server = new Server();
        try
        {
            server.setProperties( p );
        }
        catch( ServerAcl.AclFormatException e )
        {
            throw new IllegalStateException( "Could not initialize/create HSQL database: " + e.getMessage(), e );
        }
        server.setLogWriter( null ); // can use custom writer
        server.setErrWriter( null ); // can use custom writer
        server.start();
        this.databaseServer = server;
    }

    private void initializeHsqlDatabase() throws SQLException
    {
        LOG.info( "Initializing HSQL contents..." );
        try( Connection c = DriverManager.getConnection( "jdbc:hsqldb:hsql://localhost:9009/main", "SA", "" ) )
        {
            try( Statement stmt = c.createStatement() )
            {
                stmt.execute( SQL_INIT_01 );
            }
        }
    }

    private ServerBootstrap startMosaicServer() throws IOException, InterruptedException
    {
        LOG.info( "Starting Mosaic server at: {}", this.serverDirectory );
        ProcessBuilder builder = new ProcessBuilder( "/bin/sh", this.serverDirectory.resolve( "bin/mosaic.sh" ).toString(), "start" );
        builder.directory( this.serverDirectory.toFile() );
        builder.environment().put( "MOSAIC_PID_FILE", this.serverDirectory.resolve( "mosaic.pid" ).toString() );
        builder.environment().put( "MOSAIC_JAVA_OPTS", "-Xms1g -Xmx1g" );
        builder.start();

        // wait until it fully starts or aborts
        long start = System.currentTimeMillis();
        while( start + ( 1000 * 30 ) > System.currentTimeMillis() )
        {
            Thread.sleep( 1000 );

            Path globalLogFile = this.serverDirectory.resolve( "logs/global.log" );
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
        throw new IllegalStateException( "Mosaic server did not seem to start! (log file did not contain the line 'Mosaic server is running')" );
    }

    private Path extractServer() throws IOException
    {
        // server distribution is embedded in our JAR
        URL distZipUrl = getClass().getResource( "/dist-bin.zip" );
        if( distZipUrl == null )
        {
            throw new IOException( "Mosaic distribution not found in integration tests runner JAR file" );
        }

        // extract the server distribution zip file
        try( ZipInputStream zin = new ZipInputStream( distZipUrl.openStream() ) )
        {
            ZipEntry zipEntry = zin.getNextEntry();
            while( zipEntry != null )
            {
                if( !zipEntry.isDirectory() )
                {
                    Path dest = this.workDirectory.resolve( zipEntry.getName() );
                    createDirectories( dest.getParent() );
                    copy( zin, dest );
                }
                zipEntry = zin.getNextEntry();
            }
        }

        // find the created extracted dir in the temp directory
        try( DirectoryStream<Path> stream = Files.newDirectoryStream( this.workDirectory, "mosaic-*" ) )
        {
            for( Path path : stream )
            {
                if( isDirectory( path ) )
                {
                    Path versionFile = path.resolve( "version" );
                    if( exists( versionFile ) && isRegularFile( versionFile ) && isReadable( versionFile ) )
                    {
                        LOG.info( "Extracted Mosaic server instance to: {}", this.serverDirectory );
                        return path;
                    }
                }
            }
        }
        throw new IOException( "Could not find Mosaic directory in bundled Mosaic distribution zip file" );
    }
}

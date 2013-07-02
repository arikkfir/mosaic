package org.mosaic.it.runner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.method.AuthNone;
import org.mosaic.launcher.MosaicBuilder;
import org.mosaic.launcher.MosaicInstance;
import org.mosaic.launcher.util.SystemError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import static java.nio.file.Files.*;
import static java.nio.file.StandardCopyOption.*;

/**
 * @author arik
 */
public class MosaicRunner
{
    private static final Logger LOG = LoggerFactory.getLogger( MosaicRunner.class );

    @Nonnull
    protected final ModulesExtractor modulesExtractor = new ModulesExtractor();

    @Nonnull
    private final Path runnerDirectory;

    @Nonnull
    private final Path mosaicDirectory;

    @Nonnull
    private final Properties properties = new Properties();

    @Nullable
    private MosaicInstance mosaic;

    public MosaicRunner() throws IOException
    {
        this( new Properties() );
    }

    public MosaicRunner( @Nonnull Properties properties ) throws IOException
    {
        this.runnerDirectory = createTempDirectory( "mosaicIntegrationTestRunner" );
        this.mosaicDirectory = this.runnerDirectory.resolve( "server" );
        this.properties.setProperty( "mosaic.home", mosaicDirectory.toString() );
        this.properties.putAll( properties );
    }

    public MosaicRunner onDevelopmentMode()
    {
        addProperty( "dev", "true" );
        addProperty( "showErrors", "true" );
        return this;
    }

    public void addProperty( @Nonnull String name, @Nullable String value )
    {
        this.properties.setProperty( name, value );
    }

    @Nonnull
    public MosaicInstance start() throws IOException
    {
        LOG.info( "Starting Mosaic at work dir: {}", this.runnerDirectory );
        this.mosaic = new MosaicBuilder( this.properties ).create();
        try
        {
            this.mosaic.start();
        }
        catch( SystemError.BootstrapException e )
        {
            FormattingTuple tuple = MessageFormatter.arrayFormat( e.getMessage(), e.getArguments() );
            throw new IllegalStateException( tuple.getMessage(), e );
        }
        return this.mosaic;
    }

    public void stop()
    {
        if( this.mosaic != null )
        {
            this.mosaic.stop();
        }
        this.mosaic = null;
    }

    public CommandResult runCommand( @Nonnull String commandLine ) throws IOException
    {
        try( SSHClient ssh = new SSHClient() )
        {
            ssh.addHostKeyVerifier( new PromiscuousVerifier() );
            ssh.connect( "localhost", 7553 );
            ssh.auth( "admin", new AuthNone() );
            try( Session session = ssh.startSession() )
            {
                LOG.info( "Sending SSH command '{}' to Mosaic server...", commandLine );
                final Session.Command cmd = session.exec( commandLine );

                String out = net.schmizz.sshj.common.IOUtils.readFully( cmd.getInputStream() ).toString();
                cmd.join( 120, TimeUnit.SECONDS );
                return new CommandResult( cmd.getExitStatus(), commandLine, out );
            }
        }
    }

    public CommandResult runSingleCommand( @Nonnull final String commandLine ) throws Exception
    {
        return runOnServer( new CallableWithMosaic<CommandResult>()
        {
            @Override
            public CommandResult run( @Nonnull MosaicRunner runner ) throws Exception
            {
                return runCommand( commandLine );
            }
        } );
    }

    public void deployModule( @Nonnull String name ) throws IOException
    {
        runCommand( "install-module file:" + this.modulesExtractor.getModule( name ) ).assertSuccess();
    }

    public void deployModuleFile( @Nonnull String name ) throws IOException
    {
        Path src = this.modulesExtractor.getModule( name );
        Path target = this.mosaicDirectory.resolve( "lib" ).resolve( src.getFileName() );
        copy( src, target, ATOMIC_MOVE, REPLACE_EXISTING, COPY_ATTRIBUTES );
    }

    public Path deployWebApplication( @Nonnull String name ) throws IOException, InterruptedException
    {
        {
            String webContentDescName = name + "-content.xml";
            URL webContentDescUrl = getClass().getResource( "/org/mosaic/it/apps/content/" + webContentDescName );
            if( webContentDescUrl != null )
            {
                try( InputStream is = webContentDescUrl.openStream() )
                {
                    Path webContentDescPath;
                    if( this.mosaic == null )
                    {
                        webContentDescPath = this.mosaicDirectory.resolve( "apps" ).resolve( "content" ).resolve( webContentDescName );
                    }
                    else
                    {
                        createDirectories( this.mosaic.getApps().resolve( "content" ) );
                        webContentDescPath = this.mosaic.getApps().resolve( "content" ).resolve( webContentDescName );
                    }

                    LOG.info( "Deploying content of web application '{}' to '{}'", name, webContentDescPath );
                    Files.copy( is, webContentDescPath );
                }
            }
        }

        {
            String webAppDescName = name + ".xml";
            URL webAppDescUrl = getClass().getResource( "/org/mosaic/it/apps/" + webAppDescName );
            if( webAppDescUrl == null )
            {
                throw new IllegalStateException( "Could not find testing web application '" + name + "' file" );
            }

            try( InputStream is = webAppDescUrl.openStream() )
            {
                Path webAppDescPath;
                if( this.mosaic == null )
                {
                    webAppDescPath = this.mosaicDirectory.resolve( "apps" ).resolve( webAppDescName );
                }
                else
                {
                    webAppDescPath = this.mosaic.getApps().resolve( webAppDescName );
                }

                LOG.info( "Deploying web application '{}' to '{}'", name, webAppDescPath );
                Files.copy( is, webAppDescPath );
                Thread.sleep( 3000 );
                return webAppDescPath;
            }
        }
    }

    public <T> T runOnServer( @Nonnull CallableWithMosaic<T> callable ) throws Exception
    {
        //noinspection UnusedDeclaration
        try( MosaicInstance mosaic = start() )
        {
            return callable.run( this );
        }
    }

    @Nullable
    public MosaicInstance getInstance()
    {
        return mosaic;
    }
}

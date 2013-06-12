package org.mosaic.launcher;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.felix.framework.Felix;
import org.mosaic.launcher.logging.ServerLoggingConfigurator;
import org.mosaic.launcher.logging.SimpleLoggingConfigurator;
import org.mosaic.launcher.osgi.BootBundlesWatcher;
import org.mosaic.launcher.osgi.MosaicFelix;
import org.mosaic.launcher.util.SystemError;
import org.mosaic.launcher.util.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static org.mosaic.launcher.logging.EventsLogger.printEmphasizedWarnMessage;
import static org.mosaic.launcher.util.Header.printHeader;
import static org.mosaic.launcher.util.SystemError.bootstrapError;
import static org.mosaic.launcher.util.Utils.resolveDirectoryInHome;
import static org.osgi.framework.FrameworkEvent.STOPPED;

/**
 * @author arik
 */
public class MosaicInstance implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger( MosaicInstance.class );

    /**
     * The port on which the shutdown and restart requests are sent. If changing this value, remember to change it also
     * in ServerImpl.
     */
    private static final int SHUTDOWN_PORT = 38631;

    @Nonnull
    private final Properties properties;

    @Nonnull
    private final String version;

    private final boolean devMode;

    @Nonnull
    private final Path home;

    @Nonnull
    private final Path apps;

    @Nonnull
    private final Path etc;

    @Nonnull
    private final Path lib;

    @Nonnull
    private final Path logs;

    @Nonnull
    private final Path work;

    @Nonnull
    private final Path felixWork;

    @Nonnull
    private final MosaicInstance.MosaicShutdownHook mosaicShutdownHook = new MosaicShutdownHook();

    @Nonnull
    private final Deque<BootTask> bootTasks = new ConcurrentLinkedDeque<>();

    private long initializationStartTime;

    private long initializationFinishTime;

    @Nullable
    private Felix felix;

    @Nullable
    private BootBundlesWatcher bootBundlesWatcher;

    MosaicInstance( @Nonnull Properties properties )
    {
        this.properties = properties;
        this.version = properties.getProperty( "mosaic.version" );
        this.devMode = "true".equalsIgnoreCase( properties.getProperty( "dev" ) );
        this.home = Paths.get( this.properties.getProperty( "mosaic.home" ) );
        this.apps = resolveDirectoryInHome( this.properties, this.home, "apps" );
        this.etc = resolveDirectoryInHome( this.properties, this.home, "etc" );
        this.lib = resolveDirectoryInHome( this.properties, this.home, "lib" );
        this.logs = resolveDirectoryInHome( this.properties, this.home, "logs" );
        this.work = resolveDirectoryInHome( this.properties, this.home, "work" );
        this.felixWork = this.work.resolve( "felix" );

        this.bootTasks.addAll( Arrays.asList(
                new BootTask( "home" )
                {
                    @Override
                    protected void executeInternal() throws IOException
                    {
                        if( MosaicInstance.this.devMode )
                        {
                            LOG.warn( "Cleaning logs directory..." );
                            if( exists( MosaicInstance.this.logs ) )
                            {
                                Utils.deleteContents( MosaicInstance.this.logs );
                            }

                            LOG.warn( "Cleaning work directory..." );
                            if( exists( MosaicInstance.this.work ) )
                            {
                                Utils.deleteContents( MosaicInstance.this.work );
                            }
                        }
                        else if( exists( MosaicInstance.this.felixWork ) )
                        {
                            LOG.warn( "Cleaning OSGi cache..." );
                            Utils.deletePath( MosaicInstance.this.felixWork );
                        }
                        createDirectories( apps );
                        createDirectories( etc );
                        createDirectories( lib );
                        createDirectories( logs );
                        createDirectories( work );
                    }
                },
                new BootTask( "logging" )
                {
                    @Override
                    protected void executeInternal() throws Exception
                    {
                        new ServerLoggingConfigurator( MosaicInstance.this.properties, MosaicInstance.this.etc ).initializeLogging();
                    }

                    @Override
                    protected void revertInternal() throws Exception
                    {
                        new SimpleLoggingConfigurator( MosaicInstance.this.properties ).initializeLogging();
                    }
                },
                new BootTask( "osgi-framework" )
                {
                    @Override
                    protected void executeInternal() throws BundleException
                    {
                        Felix felix = new MosaicFelix( MosaicInstance.this );
                        felix.start();
                        MosaicInstance.this.felix = felix;
                    }

                    @Override
                    protected void revertInternal() throws BundleException, InterruptedException
                    {
                        Felix felix = MosaicInstance.this.felix;
                        if( felix != null && felix.getState() == Bundle.ACTIVE )
                        {
                            felix.stop();
                            while( true )
                            {
                                if( felix.waitForStop( 1000 ).getType() == STOPPED )
                                {
                                    break;
                                }
                            }
                        }
                        MosaicInstance.this.felix = null;
                    }
                },
                new BootTask( "jvm-shutdown-hook" )
                {
                    @Override
                    protected void executeInternal()
                    {
                        Runtime.getRuntime().addShutdownHook( MosaicInstance.this.mosaicShutdownHook );
                    }

                    @Override
                    protected void revertInternal()
                    {
                        Runtime.getRuntime().removeShutdownHook( MosaicInstance.this.mosaicShutdownHook );
                    }
                },
                new BootTask( "boot-bundles" )
                {
                    @Override
                    protected void executeInternal()
                    {
                        Felix felix = MosaicInstance.this.felix;
                        if( felix != null )
                        {
                            MosaicInstance.this.bootBundlesWatcher = new BootBundlesWatcher( MosaicInstance.this, felix );
                        }
                        else
                        {
                            throw new IllegalStateException( "Felix has not been initialized" );
                        }
                    }

                    @Override
                    protected void revertInternal() throws Exception
                    {
                        BootBundlesWatcher bootBundlesWatcher = MosaicInstance.this.bootBundlesWatcher;
                        if( bootBundlesWatcher != null )
                        {
                            bootBundlesWatcher.stop();
                        }
                        else
                        {
                            throw new IllegalStateException( "Boot bundles watcher has not been initialized" );
                        }
                        MosaicInstance.this.bootBundlesWatcher = null;
                    }
                },
                new BootTask( "mosaic-shutdown-listener" )
                {
                    private ServerSocket serverSocket;

                    @Override
                    protected void executeInternal() throws IOException
                    {
                        this.serverSocket = new ServerSocket( SHUTDOWN_PORT, 5, InetAddress.getLoopbackAddress() );
                        new Thread( "MosaicShutdownListener" )
                        {
                            @Override
                            public void run()
                            {
                                while( true )
                                {
                                    final Socket clientSocket;
                                    try
                                    {
                                        clientSocket = serverSocket.accept();
                                    }
                                    catch( SocketException e )
                                    {
                                        LOG.info( "Mosaic shutdown listener closed" );
                                        break;
                                    }
                                    catch( IOException e )
                                    {
                                        LOG.error( "Could not listen for shutdown requests on port {}: {}", SHUTDOWN_PORT, e.getMessage(), e );
                                        break;
                                    }

                                    new Thread( "ShutdownRequestHandler" )
                                    {
                                        @Override
                                        public void run()
                                        {
                                            try( BufferedReader reader = new BufferedReader( new InputStreamReader( clientSocket.getInputStream(), "UTF-8" ) ) )
                                            {
                                                String instruction = reader.readLine();
                                                switch( instruction )
                                                {
                                                    case "SHUTDOWN":
                                                        MosaicInstance.this.stop();
                                                        break;
                                                    case "RESTART":
                                                        MosaicInstance.this.stop();
                                                        MosaicInstance.this.start();
                                                        break;
                                                }
                                            }
                                            catch( IOException e )
                                            {
                                                LOG.error( "Error while processing request to shutdown listener: {}", e.getMessage(), e );
                                            }
                                        }
                                    }.start();
                                }
                            }
                        }.start();
                    }

                    @Override
                    protected void revertInternal() throws IOException
                    {
                        this.serverSocket.close();
                    }
                }
        ) );
    }

    @Nonnull
    public String getVersion()
    {
        return version;
    }

    @Nonnull
    public Properties getProperties()
    {
        return properties;
    }

    public boolean isDevMode()
    {
        return devMode;
    }

    @Nonnull
    public Path getHome()
    {
        return home;
    }

    @Nonnull
    public Path getApps()
    {
        return apps;
    }

    @Nonnull
    public Path getEtc()
    {
        return etc;
    }

    @Nonnull
    public Path getLib()
    {
        return lib;
    }

    @Nonnull
    public Path getLogs()
    {
        return logs;
    }

    @Nonnull
    public Path getWork()
    {
        return work;
    }

    public long getInitializationStartTime()
    {
        return initializationStartTime;
    }

    @Nonnull
    public String getInitializationTime()
    {
        long seconds = ( this.initializationFinishTime - this.initializationStartTime ) / 1000;
        return seconds + " seconds";
    }

    @Nonnull
    public String getUpTime()
    {
        long seconds = ( currentTimeMillis() - this.initializationFinishTime ) / 1000;
        return seconds + " seconds";
    }

    public synchronized void start()
    {
        if( MosaicInstance.this.felix != null )
        {
            throw bootstrapError( "Mosaic already started!" );
        }

        MosaicInstance.this.initializationStartTime = System.currentTimeMillis();
        MosaicInstance.this.initializationFinishTime = -1;
        printHeader( MosaicInstance.this );

        Deque<BootTask> executedTasks = new LinkedList<>();
        for( BootTask task : this.bootTasks )
        {
            if( task.execute() )
            {
                executedTasks.push( task );
            }
            else
            {
                while( !executedTasks.isEmpty() )
                {
                    executedTasks.pop().revert();
                }
                throw SystemError.bootstrapError( "Could not start Mosaic server: boot phase {} failed", task.getName() );
            }
        }

        MosaicInstance.this.initializationFinishTime = currentTimeMillis();
        printEmphasizedWarnMessage( "Mosaic server is running (initialized in {})", getInitializationTime() );
    }

    @Override
    public void close() throws IOException
    {
        stop();
    }

    public synchronized void stop()
    {
        if( this.felix != null )
        {
            printEmphasizedWarnMessage( "Mosaic server is stopping..." );

            List<BootTask> reversedTasks = new ArrayList<>( this.bootTasks );
            Collections.reverse( reversedTasks );
            for( BootTask task : reversedTasks )
            {
                task.revert();
            }

            printEmphasizedWarnMessage( "Mosaic system has been stopped (up-time was {})", getUpTime() );
            MosaicInstance.this.initializationFinishTime = -1;
            MosaicInstance.this.initializationStartTime = -1;
        }
    }

    private class MosaicShutdownHook extends Thread
    {
        public MosaicShutdownHook()
        {
            super( "Mosaic Shutdown Hook" );
        }

        @Override
        public void run()
        {
            try
            {
                MosaicInstance.this.stop();
            }
            catch( Exception e )
            {
                LOG.warn( e.getMessage(), e );
            }
        }
    }
}

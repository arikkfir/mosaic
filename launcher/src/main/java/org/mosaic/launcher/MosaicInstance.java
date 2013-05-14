package org.mosaic.launcher;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.felix.framework.Felix;
import org.mosaic.launcher.logging.LoggingConfigurator;
import org.mosaic.launcher.osgi.BootBundlesWatcher;
import org.mosaic.launcher.osgi.MosaicFelix;
import org.mosaic.launcher.util.Utils;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static org.mosaic.launcher.logging.EventsLogger.printEmphasizedWarnMessage;
import static org.mosaic.launcher.util.Header.printHeader;
import static org.mosaic.launcher.util.SystemError.BootstrapException;
import static org.mosaic.launcher.util.SystemError.bootstrapError;
import static org.mosaic.launcher.util.Utils.resolveDirectoryInHome;

/**
 * @author arik
 */
public class MosaicInstance implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger( MosaicInstance.class );

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
    private final LoggingConfigurator loggingConfigurator;

    @Nonnull
    private final MosaicInstance.MosaicShutdownHook mosaicShutdownHook = new MosaicShutdownHook();

    /**
     * The exact time initialization started.
     */
    private long initializationStartTime;

    /**
     * The exact time initialization finished.
     */
    private long initializationFinishTime;

    /**
     * The OSGi container instance.
     */
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
        this.loggingConfigurator = new LoggingConfigurator( this );
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
        this.initializationStartTime = System.currentTimeMillis();
        this.initializationFinishTime = -1;

        printHeader( this );
        prepareHome();
        this.loggingConfigurator.initializeLogging();
        startFelix();
        try
        {
            //noinspection ConstantConditions
            this.bootBundlesWatcher = new BootBundlesWatcher( this, this.felix );
        }
        catch( Exception e )
        {
            stop();
            throw e;
        }

        // done!
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
        if( this.bootBundlesWatcher != null )
        {
            try
            {
                this.bootBundlesWatcher.stop();
            }
            catch( InterruptedException ignore )
            {
            }
            this.bootBundlesWatcher = null;
        }

        if( this.felix != null )
        {
            try
            {
                this.felix.stop();
                this.felix.waitForStop( 30000 );
            }
            catch( BundleException e )
            {
                throw new IllegalStateException( "Could not stop OSGi container: " + e.getMessage(), e );
            }
            catch( InterruptedException e )
            {
                throw new IllegalStateException( "Timed-out while waiting for OSGi container to stop.", e );
            }
            catch( Exception e )
            {
                throw new IllegalStateException( "Unknown error occurred while stopping Mosaic: " + e.getMessage(), e );
            }
            finally
            {
                this.felix = null;
                try
                {
                    Runtime.getRuntime().removeShutdownHook( this.mosaicShutdownHook );
                }
                catch( Exception ignore )
                {
                }
            }
        }
    }

    private void prepareHome()
    {
        try
        {
            // if dev mode, clean logs and all work dirs
            if( this.devMode )
            {
                LOG.warn( "Cleaning logs directory..." );
                if( exists( this.logs ) )
                {
                    Utils.deleteContents( this.logs );
                }

                LOG.warn( "Cleaning work directory..." );
                if( exists( this.work ) )
                {
                    Utils.deleteContents( this.work );
                }
            }
            else
            {
                // not dev mode - just clean felix storage directory
                if( exists( this.felixWork ) )
                {
                    Utils.deletePath( this.felixWork );
                }
            }
        }
        catch( BootstrapException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw bootstrapError( "Could not clean temporary work directories: {}", e.getMessage(), e );
        }

        try
        {
            createDirectories( this.apps );
            createDirectories( this.etc );
            createDirectories( this.lib );
            createDirectories( this.logs );
            createDirectories( this.work );
        }
        catch( IOException e )
        {
            throw bootstrapError( "Could not create server home directories: {}", e.getMessage(), e );
        }
    }

    private void startFelix()
    {
        // create felix configuration
        try
        {
            // create & start felix
            Felix felix = new MosaicFelix( this );
            felix.start();
            this.felix = felix;

            // install a shutdown hook to ensure we close the server when the JVM process dies
            Runtime.getRuntime().addShutdownHook( this.mosaicShutdownHook );

            // start a thread that monitors felix
            new FrameworkStateMonitor().start();
        }
        catch( BootstrapException e )
        {
            stop();
            throw e;
        }
        catch( Exception e )
        {
            stop();
            throw bootstrapError( "Could not initialize OSGi container: {}", e.getMessage(), e );
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

    private class FrameworkStateMonitor extends Thread
    {
        private FrameworkStateMonitor()
        {
            setName( "FelixMonitor" );
            setDaemon( false );
            setPriority( Thread.MIN_PRIORITY );
        }

        @Override
        public void run()
        {
            Felix felix = MosaicInstance.this.felix;
            while( felix != null )
            {
                try
                {
                    int event = felix.waitForStop( 1000l ).getType();
                    if( event == FrameworkEvent.STOPPED )
                    {
                        // clear Felix reference
                        MosaicInstance.this.felix = null;

                        printEmphasizedWarnMessage( "Mosaic system has been stopped (up-time was {})", getUpTime() );

                        // if restarting - start a new thread which will call Main again
                        if( Boolean.getBoolean( "mosaic.restarting" ) )
                        {
                            // reset the 'restarting' flag
                            System.setProperty( "mosaic.restarting", "false" );

                            // wait a little for things to calm down
                            Thread.sleep( 2000 );

                            // start a new thread which will start the server
                            new StartRunnable().start();
                        }
                        return;
                    }
                }
                catch( InterruptedException e )
                {
                    break;
                }
                felix = MosaicInstance.this.felix;
            }
        }
    }

    private class StartRunnable extends Thread
    {
        private StartRunnable()
        {
            setName( "MosaicRunner" );
            setDaemon( false );
            setPriority( Thread.MAX_PRIORITY );
        }

        @Override
        public void run()
        {
            MosaicInstance.this.start();
        }
    }
}

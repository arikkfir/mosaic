package org.mosaic.launcher.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.mosaic.launcher.Main;
import org.mosaic.launcher.home.HomeResolver;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.slf4j.LoggerFactory;

import static org.apache.felix.framework.cache.BundleCache.CACHE_BUFSIZE_PROP;
import static org.apache.felix.framework.util.FelixConstants.LOG_LEVEL_PROP;
import static org.mosaic.launcher.Main.getUpTime;
import static org.mosaic.launcher.SystemError.bootstrapError;
import static org.mosaic.launcher.logging.EventsLogger.printEmphasizedErrorMessage;
import static org.mosaic.launcher.logging.EventsLogger.printEmphasizedWarnMessage;
import static org.osgi.framework.Constants.*;

/**
 * @author arik
 */
public class FelixResolver
{
    private static final String INITIAL_BUNDLE_START_LEVEL = FelixConstants.BUNDLE_STARTLEVEL_PROP;

    private static final String INITIAL_FRAMEWORK_START_LEVEL = FRAMEWORK_BEGINNING_STARTLEVEL;

    @Nullable
    public static Felix felix;

    @Nullable
    public static FileVisitorsManager fileVisitorsManager;

    public static void startOsgiContainer() throws InterruptedException
    {
        Map<String, Object> felixConfig = new HashMap<>();

        // storage properties
        felixConfig.put( FRAMEWORK_STORAGE, getAndCleanFelixWorkDirectory().toAbsolutePath().toString() );
        felixConfig.put( CACHE_BUFSIZE_PROP, ( 1024 * 64 ) + "" );

        // disable Felix logging output (we'll only log OSGi events)
        felixConfig.put( LOG_LEVEL_PROP, "0" );

        // start-level
        felixConfig.put( INITIAL_FRAMEWORK_START_LEVEL, "1" );
        felixConfig.put( INITIAL_BUNDLE_START_LEVEL, "1" );

        // build spec for extra packages exported by boot delegation & the system bundle
        Properties systemPackages = readSystemPackagesSpecs();
        StringBuilder specWithVersions = new StringBuilder( 2000 );
        StringBuilder specWithoutVersions = new StringBuilder( 2000 );
        for( String packageName : systemPackages.stringPropertyNames() )
        {
            if( specWithoutVersions.length() > 0 )
            {
                specWithoutVersions.append( "," );
            }
            specWithoutVersions.append( packageName );

            if( specWithVersions.length() > 0 )
            {
                specWithVersions.append( "," );
            }
            specWithVersions.append( packageName ).append( ";version=" ).append( systemPackages.getProperty( packageName ) );
        }
        felixConfig.put( FRAMEWORK_BOOTDELEGATION, specWithoutVersions.toString() );
        felixConfig.put( FRAMEWORK_SYSTEMPACKAGES_EXTRA, specWithVersions.toString() );

        // bootstrap!
        try
        {
            // initialize felix
            Felix felix = new Felix( felixConfig );
            felix.init();

            // start felix
            felix.start();
            FelixResolver.felix = felix;

            // add a framework listener which logs framework lifecycle events
            // MUST be added *after* the framework has started to ignore the initial STARTED event which means nothing..
            Thread.sleep( 500 );
            felix.getBundleContext().addFrameworkListener( new LoggingFrameworkListener() );

            // install a shutdown hook to ensure we close the server when the JVM process dies
            installShutdownHook();

            // install a thread that monitors felix
            Thread monitorThread = new Thread( new FrameworkStateMonitor(), "FelixMonitor" );
            monitorThread.setDaemon( false );
            monitorThread.setPriority( Thread.MIN_PRIORITY );
            monitorThread.start();

            // start the file-visitors manager
            FileVisitorsManager fileVisitorsManager = new FileVisitorsManager();
            FelixResolver.fileVisitorsManager = fileVisitorsManager;
            fileVisitorsManager.start( felix.getBundleContext() );
        }
        catch( BundleException e )
        {
            throw bootstrapError( "Could not initialize OSGi container: " + e.getMessage(), e );
        }
    }

    private static Properties readSystemPackagesSpecs()
    {
        // extra packages exported by boot delegation & the system bundle
        URL sysPkgResource = FelixResolver.class.getResource( "/system-packages.properties" );
        if( sysPkgResource == null )
        {
            throw bootstrapError( "Could not find system packages file at '/system-packages.properties' - cannot boot server" );
        }

        Properties properties = new Properties();
        try( InputStream is = sysPkgResource.openStream() )
        {
            properties.load( is );
            return properties;
        }
        catch( IOException e )
        {
            throw bootstrapError( "Cannot read system packages from '" + sysPkgResource + "': " + e.getMessage(), e );
        }
    }

    private static void installShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook( new MosaicShutdownHook() );
    }

    @Nonnull
    private static Path getAndCleanFelixWorkDirectory()
    {
        Path felixDir = HomeResolver.work.resolve( "felix" );
        deletePath( felixDir );

        try
        {
            Files.createDirectories( felixDir );
        }
        catch( IOException e )
        {
            throw bootstrapError( "Could not create Apache Felix directory at '%s': %s", e, felixDir, e.getMessage() );
        }

        return felixDir;
    }

    private static void deletePath( @Nonnull Path path )
    {
        if( Files.exists( path ) )
        {
            if( Files.isDirectory( path ) )
            {
                try( DirectoryStream<Path> directoryStream = Files.newDirectoryStream( path ) )
                {
                    for( Path child : directoryStream )
                    {
                        deletePath( child );
                    }
                }
                catch( IOException e )
                {
                    throw bootstrapError( "Could not delete '%s': %s", e, path, e.getMessage() );
                }
            }

            try
            {
                Files.delete( path );
            }
            catch( IOException e )
            {
                throw bootstrapError( "Could not delete '%s': %s", e, path, e.getMessage() );
            }
        }
    }

    private static class FrameworkStateMonitor implements Runnable
    {
        @Override
        public void run()
        {
            Felix felix = FelixResolver.felix;
            while( felix != null )
            {
                try
                {
                    switch( felix.waitForStop( 1000l ).getType() )
                    {
                        case FrameworkEvent.ERROR:
                            printEmphasizedWarnMessage( "Mosaic has been stopped due to an error." );
                            break;

                        case FrameworkEvent.STOPPED_UPDATE:
                        case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
                            printEmphasizedErrorMessage( "OSGi container has been updated - this is not a supported scenario in Mosaic servers. Please use standard restart facilities (see Server class)" );
                            break;

                        case FrameworkEvent.STOPPED:

                            // stop file-visitors manager
                            FileVisitorsManager fileVisitorsManager = FelixResolver.fileVisitorsManager;
                            if( fileVisitorsManager != null )
                            {
                                fileVisitorsManager.stop();
                                FelixResolver.fileVisitorsManager = null;
                            }

                            printEmphasizedWarnMessage( "Mosaic system has been stopped (up-time was {})", getUpTime() );

                            // if restarting - start a new thread which will call Main again
                            if( Boolean.getBoolean( "mosaic.restarting" ) )
                            {
                                Thread restartThread = new Thread( new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        try
                                        {
                                            Thread.sleep( 2000 );
                                            System.setProperty( "mosaic.restarting", "false" );
                                            Main.main( new String[ 0 ] );
                                        }
                                        catch( InterruptedException e )
                                        {
                                            printEmphasizedWarnMessage( "Restart skipped because restart thread has been interrupted" );
                                        }
                                    }
                                }, "RestartThread" );
                                restartThread.setDaemon( false );
                                restartThread.start();
                            }
                            else
                            {
                                FelixResolver.felix = null;
                            }
                            return;
                    }
                    felix = FelixResolver.felix;
                }
                catch( InterruptedException e )
                {
                    break;
                }
            }
        }
    }

    private static class MosaicShutdownHook extends Thread
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
                Felix felix = FelixResolver.felix;
                if( felix != null )
                {
                    felix.stop();
                    felix.waitForStop( 30000 );
                }
            }
            catch( BundleException e )
            {
                LoggerFactory.getLogger( "org.osgi.framework" ).warn( "Could not stop OSGi container: {}", e.getMessage(), e );
            }
            catch( InterruptedException e )
            {
                LoggerFactory.getLogger( "org.osgi.framework" ).warn( "Timed-out while waiting for OSGi container to stop.", e );
            }
        }
    }
}

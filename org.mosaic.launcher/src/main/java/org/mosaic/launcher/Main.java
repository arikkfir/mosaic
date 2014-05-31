package org.mosaic.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.felix.framework.Felix;
import org.osgi.framework.*;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static java.lang.Thread.setDefaultUncaughtExceptionHandler;
import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.notExists;
import static org.apache.felix.framework.cache.BundleCache.CACHE_BUFSIZE_PROP;
import static org.apache.felix.framework.util.FelixConstants.LOG_LEVEL_PROP;
import static org.mosaic.launcher.SystemError.bootstrapError;
import static org.mosaic.launcher.SystemPackages.getExtraSystemPackages;
import static org.osgi.framework.Constants.*;

/**
 * @author arik
 */
public class Main
{
    private static final String XX_USE_SPLIT_VERIFIER = "-XX:-UseSplitVerifier";

    private static final int FELIX_CACHE_BUFSIZE = 1024 * 64;

    public static void main( String[] args ) throws IOException, BundleException
    {
        //////////////////////////////////////////////////////
        // setup
        //////////////////////////////////////////////////////

        // install a shutdown hook to stop Felix
        Runtime.getRuntime().addShutdownHook( new Thread( new Shutdown(), "Mosaic-Shutdown-Hook" ) );

        // mark launch time
        System.setProperty( "org.mosaic.start.time", System.currentTimeMillis() + "" );

        // ensure split verifier is not used
        assertJvmSplitVerifierIsDisabled();

        // connect SLF4J to java.util.logging
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // install an exception handler for all threads that don't have an exception handler, that simply logs the exception
        setDefaultUncaughtExceptionHandler( new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException( Thread t, Throwable e )
            {
                EventsLogger.printEmphasizedErrorMessage( e.getMessage(), e );
            }
        } );

        //////////////////////////////////////////////////////
        // configuration
        //////////////////////////////////////////////////////
        Map<String, Object> properties = new LinkedHashMap<>();

        // read mosaic.properties & initialize version
        URL mosaicPropertiesResource = Main.class.getResource( "/mosaic.properties" );
        if( mosaicPropertiesResource == null )
        {
            throw bootstrapError( "Incomplete Mosaic installation - could not find 'mosaic.properties' file." );
        }
        Properties mosaicVersionProperties = new Properties();
        try( InputStream input = mosaicPropertiesResource.openStream() )
        {
            mosaicVersionProperties.load( input );
            properties.put( "org.mosaic.version", mosaicVersionProperties.getProperty( "org.mosaic.version" ) );
        }
        catch( Exception e )
        {
            throw bootstrapError( "Could not read from '{}': {}", mosaicPropertiesResource, e.getMessage(), e );
        }

        // initialize home directory locations
        Path home = Paths.get( System.getProperty( "org.mosaic.home", System.getProperty( "user.dir" ) ) );
        Path bin = Paths.get( System.getProperty( "org.mosaic.home.bin", home.resolve( "bin" ).toString() ) );
        Path apps = Paths.get( System.getProperty( "org.mosaic.home.apps", home.resolve( "apps" ).toString() ) );
        Path etc = Paths.get( System.getProperty( "org.mosaic.home.etc", home.resolve( "etc" ).toString() ) );
        Path lib = Paths.get( System.getProperty( "org.mosaic.home.lib", home.resolve( "lib" ).toString() ) );
        Path logs = Paths.get( System.getProperty( "org.mosaic.home.logs", home.resolve( "logs" ).toString() ) );
        Path schemas = Paths.get( System.getProperty( "org.mosaic.home.schemas", home.resolve( "schemas" ).toString() ) );
        Path work = Paths.get( System.getProperty( "org.mosaic.home.work", home.resolve( "work" ).toString() ) );
        properties.put( "org.mosaic.home", home.toString() );
        properties.put( "org.mosaic.home.apps", apps.toString() );
        properties.put( "org.mosaic.home.bin", bin.toString() );
        properties.put( "org.mosaic.home.etc", etc.toString() );
        properties.put( "org.mosaic.home.lib", lib.toString() );
        properties.put( "org.mosaic.home.logs", logs.toString() );
        properties.put( "org.mosaic.home.schemas", schemas.toString() );
        properties.put( "org.mosaic.home.work", work.toString() );

        //////////////////////////////////////////////////////
        // bootstrap
        //////////////////////////////////////////////////////

        // create home directory structure (if not existing)
        createDirectories( home );
        createDirectories( apps );
        createDirectories( etc );
        createDirectories( lib );
        createDirectories( logs );
        createDirectories( work );

        // show header
        Header.printHeader( properties );

        // add Felix configuration
        properties.put( Constants.FRAMEWORK_STORAGE, work.resolve( "felix" ).toString() );  // specify work location for felix
        properties.put( CACHE_BUFSIZE_PROP, FELIX_CACHE_BUFSIZE + "" );                     // buffer size for reading from storage
        properties.put( LOG_LEVEL_PROP, "0" );                                              // disable Felix logging output (we'll only log OSGi events)
        properties.put( FRAMEWORK_BUNDLE_PARENT, FRAMEWORK_BUNDLE_PARENT_EXT );             // parent class-loader of all bundles
        //        properties.put( FRAMEWORK_BEGINNING_STARTLEVEL, "1" );                              // start at 1, we'll increase the start level manually
        properties.put( FRAMEWORK_SYSTEMPACKAGES_EXTRA, getExtraSystemPackages() );         // extra packages exported by system bundle
        properties.put( FRAMEWORK_BOOTDELEGATION, "sun.*" );                                // extra packages available via classloader delegation (ie. not "Import-Package" necessary)

        // create Felix, bringing it to STARTING state
        // this means that it loads bundles from previous runs, but DOES NOT start them
        // this will give us a chance to remove bundles that no longer exist in the filesystem, and to update
        // those whose files have been updated
        Felix felix = new Felix( properties );
        felix.init();

        // get felix bundle context
        BundleContext felixBundleContext = felix.getBundleContext();
        if( felixBundleContext == null )
        {
            throw bootstrapError( "Felix has no bundle context!" );
        }

        // add OSGi listeners that emit log statements on OSGi events
        felixBundleContext.addFrameworkListener( new FrameworkListener()
        {
            @Override
            public void frameworkEvent( FrameworkEvent frameworkEvent )
            {
                Bundle bundle = frameworkEvent.getBundle();

                @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
                Throwable throwable = frameworkEvent.getThrowable();

                switch( frameworkEvent.getType() )
                {
                    case FrameworkEvent.ERROR:
                        EventsLogger.printEmphasizedErrorMessage( throwable.getMessage(), throwable );
                        break;

                    case FrameworkEvent.PACKAGES_REFRESHED:
                        EventsLogger.printEmphasizedInfoMessage( "OSGi packages have been refreshed" );
                        break;

                    case FrameworkEvent.STARTED:
                        EventsLogger.printEmphasizedInfoMessage( "OSGi framework has been started" );
                        break;
                }
            }
        } );

        // ensure our core bundle is up to date
        Path coreBundlePath = Paths.get( System.getProperty( "coreBundle", home.resolve( "bin" ).resolve( "org.mosaic.core.jar" ).toString() ) );
        Bundle coreBundle = felixBundleContext.getBundle( "file:" + coreBundlePath );
        if( coreBundle == null )
        {
            //noinspection UnusedAssignment
            coreBundle = felixBundleContext.installBundle( "file:" + coreBundlePath );
        }
        else
        {
            coreBundle.update();
        }
        coreBundle.start();

        // update bundles
        for( Bundle bundle : felix.getBundleContext().getBundles() )
        {
            String location = bundle.getLocation();
            if( location.startsWith( "file:" ) )
            {
                Path path = Paths.get( location.substring( "file:".length() ) );
                if( notExists( path ) )
                {
                    bundle.uninstall();
                }
                else if( getLastModifiedTime( path ).toMillis() > bundle.getLastModified() )
                {
                    bundle.update();
                    bundle.start();  // FIXME: we should only start it if it was active in the first place
                }
            }
        }

        // now start Felix
        felix.start();
        Shutdown.setFelix( felix );
    }

    private static void assertJvmSplitVerifierIsDisabled()
    {
        for( String arg : getRuntimeMXBean().getInputArguments() )
        {
            if( arg.contains( XX_USE_SPLIT_VERIFIER ) )
            {
                return;
            }
        }
        throw bootstrapError(
                "The JVM split verifier argument has not been disabled.\n" +
                "The JVM split verifier conflicts with Mosaic's bytecode \n" +
                "weaving.\n" +
                "Please provide the argument to the JVM command line:\n" +
                "    java ... {} ...",
                XX_USE_SPLIT_VERIFIER
        );
    }

    private static void createDirectories( Path... paths ) throws IOException
    {
        for( Path path : paths )
        {
            if( Files.isSymbolicLink( path ) )
            {
                createDirectories( Files.readSymbolicLink( path ) );
            }
            else if( Files.exists( path ) )
            {
                if( Files.isRegularFile( path ) )
                {
                    throw new IOException( "path '" + path + "' is a file and not a directory" );
                }
            }
            else
            {
                Files.createDirectories( path );
            }
        }
    }
}

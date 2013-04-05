package org.mosaic.launcher;

import java.lang.management.ManagementFactory;
import javax.annotation.Nonnull;
import org.apache.felix.framework.Felix;
import org.mosaic.launcher.home.HomeResolver;
import org.mosaic.launcher.osgi.FelixResolver;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.FrameworkStartLevel;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;
import static org.mosaic.launcher.SystemError.bootstrapError;
import static org.mosaic.launcher.home.HomeResolver.initServerHome;
import static org.mosaic.launcher.logging.EventsLogger.printEmphasizedErrorMessage;
import static org.mosaic.launcher.logging.EventsLogger.printEmphasizedWarnMessage;
import static org.mosaic.launcher.logging.LoggingResolver.initServerLogging;
import static org.mosaic.launcher.osgi.BundlesInstaller.watchDirectory;
import static org.mosaic.launcher.osgi.FelixResolver.startOsgiContainer;

/**
 * @author arik
 */
public class Main
{
    private static long initializationStartTime;

    private static long initializationFinishTime;

    public static void main( @Nonnull String[] args )
    {
        setInitializationStartTime( currentTimeMillis() );
        try
        {
            // split verifier is required for AOP weaving by our WeavingHook implementations (i.e. by Javassist)
            verifySplitVerifierIsUsed();

            // init server home structure
            initServerHome();

            // init logging
            initServerLogging();

            // init and start the OSGi container
            startOsgiContainer();

            // install and start boot libraries
            watchDirectory( HomeResolver.boot.resolve( "libs" ) );
            if( !verifyAllBundlesAreActive() )
            {
                return;
            }

            // install and start boot mosaic libraries
            setDefaultBundleStartLevel( 5 );
            watchDirectory( HomeResolver.boot.resolve( "mosaic" ) );
            setFrameworkStartLevel( 5, new FrameworkListener()
            {
                @Override
                public void frameworkEvent( FrameworkEvent event )
                {
                    // verify all boot bundles have been started properly
                    if( !verifyAllBundlesAreActive() )
                    {
                        // TODO arik: does not detect boot bundles that are STARTED but not ACTIVE
                        return;
                    }

                    // install and start bundles from the lib directory
                    watchDirectory( HomeResolver.lib );

                    // we're running!
                    setInitializationFinishTime( currentTimeMillis() );
                    printEmphasizedWarnMessage( "Mosaic server is running (initialized in {})", getInitializationTime() );
                }
            } );
        }
        catch( Exception e )
        {
            // handle all errors here
            SystemError.handle( e );
            System.exit( 1 );
        }
    }

    @Nonnull
    public static String getUpTime()
    {
        return formatDurationWords( currentTimeMillis() - initializationFinishTime, true, true );
    }

    @Nonnull
    public static String getInitializationTime()
    {
        return formatDurationWords( initializationFinishTime - initializationStartTime, true, true );
    }

    public static void setInitializationStartTime( long initializationStartTime )
    {
        Main.initializationStartTime = initializationStartTime;
        Main.initializationFinishTime = -1;
    }

    public static void setInitializationFinishTime( long initializationFinishTime )
    {
        Main.initializationFinishTime = initializationFinishTime;
    }

    private static void setFrameworkStartLevel( int startlevel, @Nonnull FrameworkListener... frameworkListeners )
    {
        Felix felix = FelixResolver.felix;
        if( felix != null )
        {
            felix.getBundle().adapt( FrameworkStartLevel.class ).setStartLevel( startlevel, frameworkListeners );
        }
    }

    private static void setDefaultBundleStartLevel( int startlevel )
    {
        Felix felix = FelixResolver.felix;
        if( felix != null )
        {
            felix.getBundle().adapt( FrameworkStartLevel.class ).setInitialBundleStartLevel( startlevel );
        }
    }

    private static void verifySplitVerifierIsUsed()
    {
        final String XX_USE_SPLIT_VERIFIER = "-XX:-UseSplitVerifier";

        boolean splitVerifierMissing = true;
        for( String arg : ManagementFactory.getRuntimeMXBean().getInputArguments() )
        {
            if( arg.contains( XX_USE_SPLIT_VERIFIER ) )
            {
                splitVerifierMissing = false;
                break;
            }
        }

        if( splitVerifierMissing )
        {
            throw bootstrapError(
                    "The JVM split verifier argument has not been specified.\n" +
                    "The JVM split verifier is required to enable bytecode \n" +
                    "weaving by the Mosaic server.\n" +
                    "Please provide the argument to the JVM command line:\n" +
                    "    java ... %s ...",
                    XX_USE_SPLIT_VERIFIER
            );
        }
    }

    private static boolean verifyAllBundlesAreActive()
    {
        boolean ok = true;

        Felix felix = FelixResolver.felix;
        if( felix != null )
        {
            BundleContext bundleContext = felix.getBundleContext();
            for( Bundle bundle : bundleContext.getBundles() )
            {
                if( bundle.getState() != Bundle.ACTIVE )
                {
                    ok = false;
                    printEmphasizedErrorMessage( "Failed to start Mosaic server - bundle '{}-{}[{}]' failed to start (shutting down)", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    try
                    {
                        bundleContext.getBundle( 0 ).stop( 0 );
                    }
                    catch( BundleException ignore )
                    {
                    }
                    break;
                }
            }
        }

        return ok;
    }
}

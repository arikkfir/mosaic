package org.mosaic.launcher;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.felix.framework.Felix;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;
import static org.apache.felix.framework.cache.BundleCache.CACHE_BUFSIZE_PROP;
import static org.apache.felix.framework.util.FelixConstants.LOG_LEVEL_PROP;
import static org.mosaic.launcher.EventsLogger.printEmphasizedInfoMessage;
import static org.mosaic.launcher.SystemError.bootstrapError;
import static org.mosaic.launcher.SystemPackages.getExtraSystemPackages;
import static org.osgi.framework.Constants.*;
import static org.osgi.framework.FrameworkEvent.STOPPED;

/**
 * @author arik
 */
final class InitFelixTask extends InitTask
{
    private static final Logger OSGI_SVC_LOG = LoggerFactory.getLogger( "org.osgi.service" );

    private static final Logger OSGI_FRWK_LOG = LoggerFactory.getLogger( "org.osgi.framework" );

    private static final int FELIX_CACHE_BUFSIZE = 1024 * 64;

    @Nullable
    private Felix felix;

    @Override
    public void start()
    {
        this.log.debug( "Starting OSGi container" );
        if( this.felix != null )
        {
            throw SystemError.bootstrapError( "Mosaic already started!" );
        }

        Map<Object, Object> felixConfig = new HashMap<>();

        // standard Felix configurations
        felixConfig.put( FRAMEWORK_STORAGE, Mosaic.getWork().resolve( "felix" ).toString() );   // specify work location for felix
        felixConfig.put( CACHE_BUFSIZE_PROP, FELIX_CACHE_BUFSIZE + "" );                        // buffer size for reading from storage
        felixConfig.put( LOG_LEVEL_PROP, "0" );                                                 // disable Felix logging output (we'll only log OSGi events)
        felixConfig.put( FRAMEWORK_SYSTEMPACKAGES_EXTRA, getExtraSystemPackages() );            // extra packages exported by system bundle
        felixConfig.put( FRAMEWORK_BOOTDELEGATION, "javax.*," +
                                                   "org.w3c.*," +
                                                   "com.sun.*," +
                                                   "sun.*," +
                                                   "com.yourkit.*" );                           // extra packages available via classloader delegation (ie. not "Import-Package" necessary)
        felixConfig.put( FRAMEWORK_BUNDLE_PARENT, FRAMEWORK_BUNDLE_PARENT_EXT );                // parent class-loader of all bundles
        felixConfig.put( FRAMEWORK_BEGINNING_STARTLEVEL, "1" );                                 // start at 1, we'll increase the start level manually

        // mosaic configurations
        felixConfig.put( "mosaic.version", Mosaic.getVersion() );
        felixConfig.put( "mosaic.devMode", Mosaic.isDevMode() + "" );
        felixConfig.put( "mosaic.home", Mosaic.getHome().toString() );
        felixConfig.put( "mosaic.home.apps", Mosaic.getApps().toString() );
        felixConfig.put( "mosaic.home.etc", Mosaic.getEtc().toString() );
        felixConfig.put( "mosaic.home.lib", Mosaic.getLib().toString() );
        felixConfig.put( "mosaic.home.logs", Mosaic.getLogs().toString() );
        felixConfig.put( "mosaic.home.work", Mosaic.getWork().toString() );

        // for LogService
        felixConfig.put( "org.apache.felix.log.maxSize", "0" );

        // for EventAdmin
        felixConfig.put( "org.apache.felix.eventadmin.ThreadPoolSize", "3" );
        felixConfig.put( "org.apache.felix.eventadmin.Timeout", "30000" );

        // create container
        try
        {
            this.log.trace( "Creating Felix instance with configuration: {}", felixConfig );
            Felix felix = new Felix( felixConfig );
            felix.init();

            // get bundle context, aborting if missing
            BundleContext bundleContext = felix.getBundleContext();
            if( bundleContext == null )
            {
                try
                {
                    felix.stop( Bundle.STOP_TRANSIENT );
                }
                catch( BundleException ignore )
                {
                }
                throw SystemError.bootstrapError( "Felix not started correctly" );
            }

            // add listeners
            OsgiEventsLoggingListener loggingListener = new OsgiEventsLoggingListener();
            bundleContext.addFrameworkListener( loggingListener );
            bundleContext.addServiceListener( loggingListener );
            bundleContext.addBundleListener( loggingListener );

            // felix started!
            this.felix = felix;
            felix.start();

            // create bundle scanner
            BundleScanner bundleScanner = new BundleScanner( bundleContext );
            Hashtable<String, Object> bundleScannerDict = new Hashtable<>();
            bundleScannerDict.put( "bundleScanner", true );
            bundleContext.registerService( Runnable.class, bundleScanner, bundleScannerDict );
            bundleScanner.run();

            // climb the start levels
            FrameworkStartLevel frameworkStartLevel = bundleContext.getBundle().adapt( FrameworkStartLevel.class );
            for( int i = 1; i <= 5; i++ )
            {
                final AtomicBoolean done = new AtomicBoolean( false );
                frameworkStartLevel.setStartLevel( i, new FrameworkListener()
                {
                    @Override
                    public void frameworkEvent( FrameworkEvent event )
                    {
                        done.set( true );
                    }
                } );
                while( !done.get() )
                {
                    Thread.sleep( 100 );
                }
                Thread.sleep( 200 );
            }

            // started!
            printEmphasizedInfoMessage( "MOSAIC STARTED!" );
        }
        catch( Exception e )
        {
            throw bootstrapError( "Could not create Felix instance: {}", e.getMessage(), e );
        }
    }

    @Override
    public void stop()
    {
        Felix felix = this.felix;
        if( felix != null )
        {
            this.log.info( "Stopping OSGi container" );
            try
            {
                felix.stop( Bundle.STOP_TRANSIENT );
                while( true )
                {
                    try
                    {
                        if( felix.waitForStop( 1000 ).getType() == STOPPED )
                        {
                            break;
                        }
                    }
                    catch( InterruptedException e )
                    {
                        break;
                    }
                }
            }
            catch( BundleException e )
            {
                this.log.warn( "Error stopping Felix: {}", e.getMessage(), e );
            }
            this.felix = null;
        }
    }

    @Nullable
    public Felix getFelix()
    {
        return this.felix;
    }

    private class OsgiEventsLoggingListener implements FrameworkListener, ServiceListener, SynchronousBundleListener
    {
        @Override
        public void frameworkEvent( @Nonnull FrameworkEvent event )
        {
            Throwable throwable = event.getThrowable();
            String throwableMsg = throwable != null ? throwable.getMessage() : "";

            Bundle bundle = event.getBundle();
            String bstr = bundle == null ? "unknown" : bundle.getSymbolicName() + "-" + bundle.getVersion() + "[" + bundle.getBundleId() + "]";

            switch( event.getType() )
            {
                case FrameworkEvent.INFO:
                    OSGI_FRWK_LOG.info( "OSGi framework informational has occurred: {}", throwableMsg, throwable );
                    break;

                case FrameworkEvent.WARNING:
                    OSGI_FRWK_LOG.warn( "OSGi framework warning has occurred: {}", throwableMsg, throwable );
                    break;

                case FrameworkEvent.ERROR:
                    OSGI_FRWK_LOG.error( "OSGi framework error for/from bundle '{}' has occurred:", bstr, throwable );
                    break;

                case FrameworkEvent.STARTLEVEL_CHANGED:
                    OSGI_FRWK_LOG.warn( "OSGi framework start level has been changed to: {}", event.getBundle().adapt( FrameworkStartLevel.class ).getStartLevel() );
                    break;
            }
        }

        @Override
        public void bundleChanged( @Nonnull BundleEvent event )
        {
            Bundle bundle = event.getBundle();
            switch( event.getType() )
            {
                case BundleEvent.INSTALLED:
                    OSGI_FRWK_LOG.info( "Bundle {}@{}[{}] has been installed", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.RESOLVED:
                    OSGI_FRWK_LOG.info( "Bundle {}@{}[{}] has been resolved", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.STARTING:
                    OSGI_FRWK_LOG.info( "Bundle {}@{}[{}] is starting", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.STARTED:
                    OSGI_FRWK_LOG.info( "Bundle {}@{}[{}] has been started", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.STOPPING:
                    OSGI_FRWK_LOG.info( "Bundle {}@{}[{}] is stopping", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.STOPPED:
                    OSGI_FRWK_LOG.info( "Bundle {}@{}[{}] has been stopped", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.UNRESOLVED:
                    OSGI_FRWK_LOG.info( "Bundle {}@{}[{}] has been unresolved", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.UNINSTALLED:
                    OSGI_FRWK_LOG.info( "Bundle {}@{}[{}] has been uninstalled", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.UPDATED:
                    OSGI_FRWK_LOG.info( "Bundle {}@{}[{}] has been updated", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
            }
        }

        @Override
        public void serviceChanged( ServiceEvent event )
        {
            if( OSGI_SVC_LOG.isTraceEnabled() )
            {
                ServiceReference<?> sr = event.getServiceReference();
                Bundle bundle = sr.getBundle();
                switch( event.getType() )
                {
                    case ServiceEvent.REGISTERED:
                        OSGI_SVC_LOG.trace(
                                "OSGi service of type {} registered from '{}-{}[{}]'",
                                asList( ( String[] ) sr.getProperty( Constants.OBJECTCLASS ) ),
                                bundle.getSymbolicName(),
                                bundle.getVersion(),
                                bundle.getBundleId()
                        );
                        break;

                    case ServiceEvent.UNREGISTERING:
                        OSGI_SVC_LOG.trace(
                                "OSGi service of type {} from from '{}-{}[{}]' was unregistered",
                                asList( ( String[] ) sr.getProperty( Constants.OBJECTCLASS ) ),
                                bundle.getSymbolicName(),
                                bundle.getVersion(),
                                bundle.getBundleId()
                        );
                        break;
                }
            }
        }
    }
}

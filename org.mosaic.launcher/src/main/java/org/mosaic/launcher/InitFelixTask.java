package org.mosaic.launcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.Files.exists;
import static java.util.Arrays.asList;
import static org.mosaic.launcher.IO.deletePath;
import static org.mosaic.launcher.SystemError.bootstrapError;
import static org.mosaic.launcher.SystemPackages.getExtraSystemPackages;
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

    InitFelixTask( @Nonnull Mosaic mosaic )
    {
        super( mosaic );
    }

    @Override
    public void start()
    {
        this.log.debug( "Starting OSGi container" );
        if( this.felix != null )
        {
            throw bootstrapError( "Mosaic already started!" );
        }

        Path felixWork = getConfiguration().getWork().resolve( "felix" );
        if( exists( felixWork ) )
        {
            try
            {
                this.log.debug( "Clearing Felix work directory at: {}", felixWork );
                deletePath( felixWork );
            }
            catch( IOException e )
            {
                throw bootstrapError( "Could not clean Felix work directory at '{}': {}", felixWork, e.getMessage(), e );
            }
        }

        Map<Object, Object> felixConfig = new HashMap<>();

        // standard Felix configurations
        felixConfig.put( FelixConstants.FRAMEWORK_STORAGE, felixWork.toString() );                  // specify work location for felix
        felixConfig.put( BundleCache.CACHE_BUFSIZE_PROP, FELIX_CACHE_BUFSIZE + "" );                // buffer size for reading from storage
        felixConfig.put( FelixConstants.LOG_LEVEL_PROP, "0" );                                      // disable Felix logging output (we'll only log OSGi events)
        felixConfig.put( FelixConstants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, getExtraSystemPackages() ); // extra packages exported by system bundle
        felixConfig.put( FelixConstants.FRAMEWORK_BOOTDELEGATION, "javax.xml.*,org.w3c.*,sun.*,com.yourkit.*" ); // extra packages available via classloader delegation (ie. not "Import-Package" necessary)
        felixConfig.put( FelixConstants.FRAMEWORK_BUNDLE_PARENT, FelixConstants.FRAMEWORK_BUNDLE_PARENT_EXT ); // parent class-loader of all bundles

        // mosaic configurations
        felixConfig.put( "mosaic.version", getConfiguration().getVersion() );
        felixConfig.put( "mosaic.devMode", getConfiguration().isDevMode() + "" );
        felixConfig.put( "mosaic.home", getConfiguration().getHome().toString() );
        felixConfig.put( "mosaic.home.boot", getConfiguration().getBoot().toString() );
        felixConfig.put( "mosaic.home.apps", getConfiguration().getApps().toString() );
        felixConfig.put( "mosaic.home.etc", getConfiguration().getEtc().toString() );
        felixConfig.put( "mosaic.home.lib", getConfiguration().getLib().toString() );
        felixConfig.put( "mosaic.home.logs", getConfiguration().getLogs().toString() );
        felixConfig.put( "mosaic.home.work", getConfiguration().getWork().toString() );

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
            felix.start();
            BundleContext bundleContext = felix.getBundleContext();
            if( bundleContext == null )
            {
                try
                {
                    felix.stop();
                }
                catch( BundleException ignore )
                {
                }
                throw bootstrapError( "Felix not started correctly" );
            }

            this.log.debug( "Adding OSGi events listener" );
            OsgiEventsLoggingListener loggingListener = new OsgiEventsLoggingListener();
            bundleContext.addFrameworkListener( loggingListener );
            bundleContext.addServiceListener( loggingListener );

            this.felix = felix;
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
        if( felix != null && felix.getState() == Bundle.ACTIVE )
        {
            this.log.info( "Stopping OSGi container" );
            try
            {
                felix.stop();
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
        }
        this.felix = null;
    }

    @Nullable
    public Felix getFelix()
    {
        return this.felix;
    }

    private class OsgiEventsLoggingListener implements FrameworkListener, ServiceListener
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
                    OSGI_FRWK_LOG.warn( "OSGi framework error for/from bundle '{}' has occurred:", bstr, throwable );
                    break;

                case FrameworkEvent.STARTLEVEL_CHANGED:
                    OSGI_FRWK_LOG.info( "OSGi framework start level has been changed to: {}", event.getBundle().adapt( FrameworkStartLevel.class ).getStartLevel() );
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

package org.mosaic.launcher;

import com.google.common.base.Optional;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.felix.framework.Felix;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Integer.parseInt;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;
import static java.util.Arrays.asList;
import static org.apache.felix.framework.cache.BundleCache.CACHE_BUFSIZE_PROP;
import static org.apache.felix.framework.util.FelixConstants.LOG_LEVEL_PROP;
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

    @Nonnull
    private static final Map<String, Integer> BOOT_BUNDLES;

    static
    {
        Map<String, Integer> bootBundles = new HashMap<>();
        bootBundles.put( "com.fasterxml.classmate", 1 );
        bootBundles.put( "com.fasterxml.jackson.core.jackson-annotations", 1 );
        bootBundles.put( "com.fasterxml.jackson.core.jackson-core", 1 );
        bootBundles.put( "com.fasterxml.jackson.core.jackson-databind", 1 );
        bootBundles.put( "com.fasterxml.jackson.dataformat.jackson-dataformat-csv", 1 );
        bootBundles.put( "com.google.guava", 1 );
        bootBundles.put( "javax.el-api", 1 );
        bootBundles.put( "jcl.over.slf4j", 1 );
        bootBundles.put( "joda-time", 1 );
        bootBundles.put( "log4j.over.slf4j", 1 );
        bootBundles.put( "org.apache.commons.lang3", 1 );
        bootBundles.put( "org.apache.felix.configadmin", 1 );
        bootBundles.put( "org.apache.felix.eventadmin", 1 );
        bootBundles.put( "org.apache.felix.log", 1 );
        bootBundles.put( "org.glassfish.web.javax.el", 1 );
        BOOT_BUNDLES = bootBundles;
    }

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

        Path felixWork = Mosaic.getWork().resolve( "felix" );
        if( exists( felixWork ) )
        {
            try
            {
                this.log.debug( "Clearing Felix work directory at: {}", felixWork );
                IO.deletePath( felixWork );
            }
            catch( IOException e )
            {
                throw SystemError.bootstrapError( "Could not clean Felix work directory at '{}': {}", felixWork, e.getMessage(), e );
            }
        }

        Map<Object, Object> felixConfig = new HashMap<>();

        // standard Felix configurations
        felixConfig.put( FRAMEWORK_STORAGE, felixWork.toString() );                     // specify work location for felix
        felixConfig.put( CACHE_BUFSIZE_PROP, FELIX_CACHE_BUFSIZE + "" );                // buffer size for reading from storage
        felixConfig.put( LOG_LEVEL_PROP, "0" );                                         // disable Felix logging output (we'll only log OSGi events)
        felixConfig.put( FRAMEWORK_SYSTEMPACKAGES_EXTRA, getExtraSystemPackages() );    // extra packages exported by system bundle
        felixConfig.put( FRAMEWORK_BOOTDELEGATION, "javax.*," +
                                                   "org.w3c.*," +
                                                   "com.sun.*," +
                                                   "sun.*," +
                                                   "com.yourkit.*" );                   // extra packages available via classloader delegation (ie. not "Import-Package" necessary)
        felixConfig.put( FRAMEWORK_BUNDLE_PARENT, FRAMEWORK_BUNDLE_PARENT_EXT );        // parent class-loader of all bundles
        felixConfig.put( FRAMEWORK_BEGINNING_STARTLEVEL, "3" );                         // framework level climbs to 3

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
                    felix.stop();
                }
                catch( BundleException ignore )
                {
                }
                throw SystemError.bootstrapError( "Felix not started correctly" );
            }

            // install bundles
            installBundles( bundleContext );

            // add listeners
            OsgiEventsLoggingListener loggingListener = new OsgiEventsLoggingListener();
            bundleContext.addFrameworkListener( loggingListener );
            bundleContext.addServiceListener( loggingListener );
            bundleContext.addBundleListener( loggingListener );

            // felix started!
            this.felix = felix;
            felix.start();
        }
        catch( Exception e )
        {
            throw SystemError.bootstrapError( "Could not create Felix instance: {}", e.getMessage(), e );
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
            this.felix = null;
        }
    }

    @Nullable
    public Felix getFelix()
    {
        return this.felix;
    }

    private void installBundles( @Nonnull final BundleContext bundleContext )
    {
        final List<Bundle> bootBundles = new LinkedList<>();

        // first just install the boot bundles without starting them (so they can use classes from one another in any order)
        this.log.debug( "Installing bundles" );
        try
        {
            Files.walkFileTree( Mosaic.getLib(), new SimpleFileVisitor<Path>()
            {
                @Nonnull
                @Override
                public FileVisitResult visitFile( @Nonnull Path file, @Nonnull BasicFileAttributes attrs )
                        throws IOException
                {
                    if( file.toString().toLowerCase().endsWith( ".jar" ) )
                    {
                        bootBundles.add( installBootBundle( bundleContext, file ) );
                    }
                    return FileVisitResult.CONTINUE;
                }
            } );
        }
        catch( Exception e )
        {
            throw bootstrapError( "could not install bundles: {}", e.getMessage(), e );
        }

        // now that we've installed them, lets start them
        this.log.debug( "Starting bundles: {}", bootBundles );
        for( Bundle bundle : bootBundles )
        {
            try
            {
                this.log.debug( "Starting bundle {}@{}[{}]", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                bundle.start();
            }
            catch( Throwable e )
            {
                throw bootstrapError( "Could not start boot bundle at '{}': {}", bundle.getLocation(), e.getMessage(), e );
            }
        }
    }

    @Nonnull
    private Bundle installBootBundle( @Nonnull BundleContext bundleContext, @Nonnull Path path )
    {
        try
        {
            // install the bundle
            this.log.debug( "Installing boot bundle at '{}'", path );
            Bundle bundle = bundleContext.installBundle( "file:" + path.toString(), newInputStream( path ) );

            Integer startLevel = null;

            String explicitStartLevel = bundle.getHeaders().get( "Start-Level" );
            if( explicitStartLevel != null )
            {
                try
                {
                    startLevel = parseInt( explicitStartLevel );
                }
                catch( NumberFormatException ignore )
                {
                }
            }

            if( startLevel == null && BOOT_BUNDLES.containsKey( bundle.getSymbolicName() ) )
            {
                startLevel = BOOT_BUNDLES.get( bundle.getSymbolicName() );
            }

            bundle.adapt( BundleStartLevel.class ).setStartLevel( Optional.fromNullable( startLevel ).or( 3 ) );

            return bundle;
        }
        catch( Exception e )
        {
            throw bootstrapError( "Could not install boot bundle at '{}': {}", path, e.getMessage(), e );
        }
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
                    OSGI_FRWK_LOG.debug( "Bundle {}@{}[{}] has been installed", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.RESOLVED:
                    OSGI_FRWK_LOG.debug( "Bundle {}@{}[{}] has been resolved", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.STARTING:
                    OSGI_FRWK_LOG.debug( "Bundle {}@{}[{}] is starting", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.STARTED:
                    OSGI_FRWK_LOG.debug( "Bundle {}@{}[{}] has been started", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.STOPPING:
                    OSGI_FRWK_LOG.debug( "Bundle {}@{}[{}] is stopping", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.STOPPED:
                    OSGI_FRWK_LOG.debug( "Bundle {}@{}[{}] has been stopped", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.UNRESOLVED:
                    OSGI_FRWK_LOG.debug( "Bundle {}@{}[{}] has been unresolved", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.UNINSTALLED:
                    OSGI_FRWK_LOG.debug( "Bundle {}@{}[{}] has been uninstalled", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
                    break;
                case BundleEvent.UPDATED:
                    OSGI_FRWK_LOG.debug( "Bundle {}@{}[{}] has been updated", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId() );
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

package org.mosaic.core.modules.impl;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import org.mosaic.core.launcher.impl.ServerImpl;
import org.mosaic.core.modules.*;
import org.mosaic.core.services.ServiceRegistration;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.weaving.WeavingSpi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class ModuleManagerImpl implements ModuleManager
{
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger( ModuleManagerImpl.class );

    @Nonnull
    private final ServerImpl server;

    @Nullable
    private Map<Long, ModuleImpl> modules;

    @Nullable
    private ServiceRegistration<ModuleManager> registration;

    public ModuleManagerImpl( @Nonnull ServerImpl server )
    {
        this.server = server;

        this.server.addStartupHook( bundleContext -> WeavingSpi.setModuleManager( this ) );
        this.server.addShutdownHook( bundleContext -> WeavingSpi.setModuleManager( null ) );

        this.server.addStartupHook( bundleContext -> {

            // clear modules store
            this.modules = new HashMap<>();

            // listen to bundle events so we can update our corresponding modules map
            //noinspection RedundantCast
            bundleContext.addBundleListener( ( SynchronousBundleListener ) this::handleBundleEvent );

            // synchronize with pre-installed bundles
            for( Bundle bundle : bundleContext.getBundles() )
            {
                if( bundle.getBundleId() > 0 )
                {
                    switch( bundle.getState() )
                    {
                        case Bundle.INSTALLED:
                            handleBundleEvent( new BundleEvent( BundleEvent.INSTALLED, bundle, bundleContext.getBundle() ) );
                            break;

                        case Bundle.RESOLVED:
                        case Bundle.STOPPING:
                            handleBundleEvent( new BundleEvent( BundleEvent.INSTALLED, bundle, bundleContext.getBundle() ) );
                            handleBundleEvent( new BundleEvent( BundleEvent.RESOLVED, bundle, bundleContext.getBundle() ) );
                            break;

                        case Bundle.STARTING:
                            handleBundleEvent( new BundleEvent( BundleEvent.INSTALLED, bundle, bundleContext.getBundle() ) );
                            handleBundleEvent( new BundleEvent( BundleEvent.RESOLVED, bundle, bundleContext.getBundle() ) );
                            handleBundleEvent( new BundleEvent( BundleEvent.STARTING, bundle, bundleContext.getBundle() ) );
                            break;

                        case Bundle.ACTIVE:
                            handleBundleEvent( new BundleEvent( BundleEvent.INSTALLED, bundle, bundleContext.getBundle() ) );
                            handleBundleEvent( new BundleEvent( BundleEvent.RESOLVED, bundle, bundleContext.getBundle() ) );
                            handleBundleEvent( new BundleEvent( BundleEvent.STARTING, bundle, bundleContext.getBundle() ) );
                            handleBundleEvent( new BundleEvent( BundleEvent.STARTED, bundle, bundleContext.getBundle() ) );
                            break;
                    }
                }
            }

            this.server.getServiceManager().registerService( null, ModuleManager.class, this );
        } );
        this.server.addStartupHook( bundleContext -> this.registration = server.getServiceManager().registerService( null, ModuleManager.class, this ) );
        this.server.addShutdownHook( bundleContext -> {

            // stop all modules
            Map<Long, ModuleImpl> modules = this.modules;
            if( modules != null )
            {
                modules.values().stream().forEach( module -> {
                    try
                    {
                        module.stop( false );
                    }
                    catch( Throwable e )
                    {
                        LOG.warn( "Could not stop module {} (during Mosaic server stop)", module, e );
                    }
                } );
            }

            // stop listening to OSGi events
            //noinspection RedundantCast
            bundleContext.removeBundleListener( ( SynchronousBundleListener ) this::handleBundleEvent );

            // clear modules store
            this.modules = null;

        } );
        this.server.addShutdownHook( bundleContext -> {
            ServiceRegistration<ModuleManager> registration = this.registration;
            if( registration != null )
            {
                registration.unregister();
                this.registration = null;
            }
        } );
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this ).toString();
    }

    @Nonnull
    @Override
    public Module installModule( @Nonnull Path path )
    {
        Bundle bundle = this.server.getLock().write( () -> {

            BundleContext bundleContext = this.server.getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "server not available" );
            }

            try
            {
                return bundleContext.installBundle( "file:" + path.toString() );
            }
            catch( Throwable e )
            {
                throw new ModuleInstallException( "could not install module from '" + path + "'", e );
            }
        } );

        return this.server.getLock().write( () -> {

            Map<Long, ModuleImpl> modules = this.modules;
            if( modules == null )
            {
                throw new IllegalStateException( "server not available" );
            }

            ModuleImpl module = modules.get( bundle.getBundleId() );
            if( module == null )
            {
                try
                {
                    module = new ModuleImpl( this.server, bundle );
                    modules.put( bundle.getBundleId(), module );
                    module.bundleInstalled();
                }
                catch( Throwable e )
                {
                    throw new ModuleInstallException( "could not install module from '" + path + "'", e );
                }
            }

            return module;
        } );
    }

    @Nullable
    @Override
    public ModuleImpl getModule( long id )
    {
        return this.server.getLock().read( () -> {
            Map<Long, ModuleImpl> modules = this.modules;
            return modules == null ? null : modules.get( id );
        } );
    }

    @Nullable
    @Override
    public Module getModule( @Nonnull String name )
    {
        return this.server.getLock().read(
                () -> getModules().stream()
                                  .filter( module -> {
                                      ModuleRevision currentRevision = module.getCurrentRevision();
                                      return currentRevision != null && name.equals( currentRevision.getName() );
                                  } )
                                  .findFirst().orElse( null ) );
    }

    @Nonnull
    @Override
    public Collection<? extends Module> getModules()
    {
        return this.server.getLock().read( () -> {
            Map<Long, ModuleImpl> modules = this.modules;
            if( modules == null )
            {
                return Collections.emptyList();
            }
            else
            {
                return Collections.unmodifiableList( new LinkedList<>( modules.values() ) );
            }
        } );
    }

    public void notifyModuleListeners( @Nonnull Consumer<ModuleListener> action )
    {
        for( ServiceRegistration<ModuleListener> registration : this.server.getServiceManager().findServices( ModuleListener.class ) )
        {
            ModuleListener listener = registration.getService();
            if( listener != null )
            {
                action.accept( listener );
            }
        }
    }

    public void notifyModuleRevisionListeners( @Nonnull Consumer<ModuleRevisionListener> action )
    {
        for( ServiceRegistration<ModuleRevisionListener> registration : this.server.getServiceManager().findServices( ModuleRevisionListener.class ) )
        {
            ModuleRevisionListener listener = registration.getService();
            if( listener != null )
            {
                action.accept( listener );
            }
        }
    }

    private void handleBundleEvent( @Nonnull BundleEvent event )
    {
        this.server.getLock().write( () -> {

            Map<Long, ModuleImpl> modules = this.modules;
            if( modules != null )
            {
                long bundleId = event.getBundle().getBundleId();
                if( bundleId > 0 )
                {
                    switch( event.getType() )
                    {
                        case BundleEvent.INSTALLED:
                            ModuleImpl module = modules.get( bundleId );
                            if( module == null )
                            {
                                module = new ModuleImpl( this.server, event.getBundle() );
                                modules.put( bundleId, module );
                                module.bundleInstalled();
                            }
                            break;

                        case BundleEvent.RESOLVED:
                            modules.get( bundleId ).bundleResolved();
                            break;

                        case BundleEvent.STARTING:
                            modules.get( bundleId ).bundleStarting();
                            break;

                        case BundleEvent.STARTED:
                            modules.get( bundleId ).bundleStarted();
                            break;

                        case BundleEvent.STOPPING:
                            modules.get( bundleId ).bundleStopping();
                            break;

                        case BundleEvent.STOPPED:
                            modules.get( bundleId ).bundleStopped();
                            break;

                        case BundleEvent.UPDATED:
                            modules.get( bundleId ).bundleUpdated();
                            break;

                        case BundleEvent.UNRESOLVED:
                            modules.get( bundleId ).bundleUnresolved();
                            break;

                        case BundleEvent.UNINSTALLED:
                            modules.remove( bundleId ).bundleUninstalled();
                            break;

                        default:
                            LOG.warn( "Unknown event type ({}) received in modules bundle listener", event.getType() );
                    }
                }
            }
        } );
    }
}

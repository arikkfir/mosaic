package org.mosaic.core.impl;

import java.util.*;
import org.mosaic.core.Module;
import org.mosaic.core.ModuleManager;
import org.mosaic.core.Server;
import org.mosaic.core.ServiceManager;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.workflow.Status;
import org.mosaic.core.util.workflow.TransitionAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * @author arik
 */
class ModuleManagerImpl extends TransitionAdapter implements ModuleManager
{
    @Nonnull
    private final SynchronousBundleListener synchronousBundleListener = new SynchronousBundleListener()
    {
        @Override
        public void bundleChanged( BundleEvent event )
        {
            handleBundleEvent( event );
        }
    };

    @Nonnull
    private final ServerImpl server;

    @Nullable
    private Map<Long, ModuleImpl> modules;

    ModuleManagerImpl( @Nonnull ServerImpl server )
    {
        this.server = server;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this ).toString();
    }

    @Override
    public void execute( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        if( target == ServerStatus.STARTED )
        {
            initialize();
        }
        else if( target == ServerStatus.STOPPED )
        {
            shutdown();
        }
    }

    @Override
    public void revert( @Nonnull Status origin, @Nonnull Status target ) throws Exception
    {
        if( target == ServerStatus.STARTED )
        {
            shutdown();
        }
    }

    @Nullable
    @Override
    public ModuleImpl getModule( long id )
    {
        this.server.acquireReadLock();
        try
        {
            Map<Long, ModuleImpl> modules = this.modules;
            return modules == null ? null : modules.get( id );
        }
        finally
        {
            this.server.releaseReadLock();
        }
    }

    @Nonnull
    @Override
    public Collection<? extends Module> getModules()
    {
        this.server.acquireReadLock();
        try
        {
            Map<Long, ModuleImpl> modules = this.modules;
            if( modules == null )
            {
                return Collections.emptyList();
            }
            else
            {
                return Collections.unmodifiableList( new LinkedList<>( modules.values() ) );
            }
        }
        finally
        {
            this.server.releaseReadLock();
        }
    }

    private void handleBundleEvent( @Nonnull BundleEvent event )
    {
        this.server.acquireWriteLock();
        try
        {
            Map<Long, ModuleImpl> modules = this.modules;
            if( modules == null )
            {
                throw new IllegalStateException( "module manager not available (is server started?)" );
            }

            long bundleId = event.getBundle().getBundleId();
            switch( event.getType() )
            {
                case BundleEvent.INSTALLED:
                    ModuleImpl module = new ModuleImpl( this.server, event.getBundle() );
                    modules.put( bundleId, module );
                    module.bundleInstalled();
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
                    this.server.getLogger().warn( "Unknown event type ({}) received in modules bundle listener", event.getType() );
            }
        }
        finally
        {
            this.server.releaseWriteLock();
        }
    }

    private void initialize()
    {
        this.server.getLogger().info( "Initializing module manager" );

        // clear modules store
        this.modules = new HashMap<>();

        // listen to bundle events so we can update our corresponding modules map
        BundleContext bundleContext = this.server.getBundleContext();
        bundleContext.addBundleListener( this.synchronousBundleListener );

        // synchronize with pre-installed bundles
        for( Bundle bundle : bundleContext.getBundles() )
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

        // register service manager
        ModuleImpl coreModule = getModule( 1 );
        if( coreModule != null )
        {
            this.server.getServiceManager().registerService( coreModule, Server.class, this.server );
            this.server.getServiceManager().registerService( coreModule, ModuleManager.class, this );
            this.server.getServiceManager().registerService( coreModule, ServiceManager.class, this.server.getServiceManager() );
        }
    }

    private void shutdown()
    {
        this.server.getLogger().info( "Shutting down module manager" );

        // stop all modules
        Map<Long, ModuleImpl> modules = this.modules;
        if( modules != null )
        {
            for( ModuleImpl module : modules.values() )
            {
                if( module.getId() > 1 )
                {
                    try
                    {
                        module.stop();
                    }
                    catch( Throwable e )
                    {
                        this.server.getLogger().warn( "Could not stop module {} (during Mosaic server stop)", module, e );
                    }
                }
            }
        }

        // stop listening to OSGi events
        this.server.getBundleContext().removeBundleListener( this.synchronousBundleListener );

        // clear modules store
        this.modules = null;
    }
}

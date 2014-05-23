package org.mosaic.core.impl.module;

import java.util.*;
import org.mosaic.core.Module;
import org.mosaic.core.ModuleManager;
import org.mosaic.core.ModuleRevision;
import org.mosaic.core.ServiceManager;
import org.mosaic.core.impl.ServerStatus;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.workflow.Status;
import org.mosaic.core.util.workflow.TransitionAdapter;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;

/**
 * @author arik
 */
public class ModuleManagerImpl extends TransitionAdapter implements ModuleManager
{
    @Nonnull
    private final SynchronousBundleListener synchronousBundleListener = new SynchronousBundleListener()
    {
        @Override
        public void bundleChanged( @Nonnull BundleEvent event )
        {
            handleBundleEvent( event );
        }
    };

    @Nonnull
    private final Logger logger;

    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final ServiceManager serviceManager;

    @Nullable
    private Map<Long, ModuleImpl> modules;

    public ModuleManagerImpl( @Nonnull Logger logger,
                              @Nonnull ReadWriteLock lock,
                              @Nonnull ServiceManager serviceManager )
    {
        this.logger = logger;
        this.lock = lock;
        this.serviceManager = serviceManager;
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
        this.lock.acquireReadLock();
        try
        {
            Map<Long, ModuleImpl> modules = this.modules;
            return modules == null ? null : modules.get( id );
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    @Nonnull
    @Override
    public Collection<? extends Module> getModules()
    {
        this.lock.acquireReadLock();
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
            this.lock.releaseReadLock();
        }
    }

    @Nullable
    public ModuleRevision getModuleRevision( @Nonnull BundleRevision bundleRevision )
    {
        long bundleId = bundleRevision.getBundle().getBundleId();
        ModuleImpl module = getModule( bundleId );
        if( module == null )
        {
            throw new IllegalArgumentException( "unknown module: " + bundleId );
        }
        else
        {
            return module.getRevision( bundleRevision );
        }
    }

    private void handleBundleEvent( @Nonnull BundleEvent event )
    {
        this.lock.acquireWriteLock();
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
                    ModuleImpl module = new ModuleImpl( this.logger, this.lock, this.serviceManager, event.getBundle() );
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
                    this.logger.warn( "Unknown event type ({}) received in modules bundle listener", event.getType() );
            }
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    private void initialize()
    {
        this.logger.info( "Initializing module manager" );

        // clear modules store
        this.modules = new HashMap<>();

        // listen to bundle events so we can update our corresponding modules map
        Bundle coreBundle = FrameworkUtil.getBundle( getClass() );
        if( coreBundle != null )
        {
            BundleContext coreBundleContext = coreBundle.getBundleContext();
            if( coreBundleContext != null )
            {
                coreBundleContext.removeBundleListener( this.synchronousBundleListener );
            }
        }
        BundleContext bundleContext = getBundleContext();
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
    }

    private void shutdown()
    {
        this.logger.info( "Shutting down module manager" );

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
                        this.logger.warn( "Could not stop module {} (during Mosaic server stop)", module, e );
                    }
                }
            }
        }

        // stop listening to OSGi events
        getBundleContext().removeBundleListener( this.synchronousBundleListener );

        // clear modules store
        this.modules = null;
    }

    @Nonnull
    private BundleContext getBundleContext()
    {
        Bundle coreBundle = FrameworkUtil.getBundle( getClass() );
        if( coreBundle != null )
        {
            BundleContext coreBundleContext = coreBundle.getBundleContext();
            if( coreBundleContext != null )
            {
                return coreBundleContext;
            }
        }
        throw new IllegalStateException( "mosaic bundle context not available" );
    }
}

package org.mosaic.modules.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Module;
import org.mosaic.modules.ModuleManager;
import org.mosaic.server.Version;
import org.osgi.framework.*;

import static org.osgi.framework.BundleEvent.*;

/**
 * @author arik
 */
final class ModuleManagerImpl implements ModuleManager
{
    @Nonnull
    private final Map<Long, ModuleImpl> modules = new ConcurrentHashMap<>( 100 );

    @Nullable
    @Override
    public ModuleImpl getModule( long id )
    {
        return this.modules.get( id );
    }

    @Nullable
    @Override
    public ModuleImpl getModule( @Nonnull String name )
    {
        for( ModuleImpl module : this.modules.values() )
        {
            if( name.equals( module.getName() ) )
            {
                return module;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public ModuleImpl getModule( @Nonnull String name, @Nonnull Version version )
    {
        for( ModuleImpl module : this.modules.values() )
        {
            if( name.equals( module.getName() ) && version.equals( module.getVersion() ) )
            {
                return module;
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public Collection<? extends Module> getModules()
    {
        return Collections.unmodifiableCollection( this.modules.values() );
    }

    @Nullable
    @Override
    public ModuleImpl getModuleFor( @Nonnull Object target )
    {
        if( target instanceof Long )
        {
            return getModule( ( Long ) target );
        }
        else if( target instanceof Bundle )
        {
            return getModule( ( ( Bundle ) target ).getBundleId() );
        }
        else if( target instanceof BundleContext )
        {
            return getModule( ( ( BundleContext ) target ).getBundle().getBundleId() );
        }
        else if( target instanceof Module )
        {
            return ( ModuleImpl ) target;
        }
        else
        {
            return null;
        }
    }

    void open( @Nonnull BundleContext bundleContext )
    {
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

        // register a *synchronous* bundle listener for bundle events
        bundleContext.addBundleListener( new SynchronousBundleListener()
        {
            @Override
            public void bundleChanged( @Nonnull BundleEvent event )
            {
                handleBundleEvent( event );
            }
        } );
    }

    void close( @Nonnull BundleContext bundleContext )
    {
        for( Bundle bundle : bundleContext.getBundles() )
        {
            if( bundle.getBundleId() > bundleContext.getBundle().getBundleId() && bundle.getState() == Bundle.ACTIVE )
            {
                try
                {
                    bundle.stop();
                }
                catch( BundleException ignore )
                {
                }
            }
        }
    }

    private synchronized void handleBundleEvent( @Nonnull BundleEvent bundleEvent )
    {
        Bundle bundle = bundleEvent.getBundle();
        ModuleImpl module = getModule( bundle.getBundleId() );
        if( module == null )
        {
            module = new ModuleImpl( this, bundle, bundle.getBundleId() <= bundleEvent.getOrigin().getBundleId() );
            this.modules.put( bundle.getBundleId(), module );
        }

        switch( bundleEvent.getType() )
        {
            case INSTALLED:
                module.onBundleInstalled();
                break;

            case RESOLVED:
                module.onBundleResolved();
                break;

            case STARTING:
                module.onBundleStarting();
                break;

            case STARTED:
                module.onBundleStarted();
                break;

            case STOPPING:
                module.onBundleStopping();
                break;

            case STOPPED:
                module.onBundleStopped();
                break;

            case UPDATED:
                module.onBundleUpdated();
                break;

            case UNRESOLVED:
                module.onBundleUnresolved();
                break;

            case UNINSTALLED:
                module.onBundleUninstalled();
                break;
        }
    }
}

package org.mosaic.modules.impl;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.mosaic.modules.Module;
import org.mosaic.modules.ModuleManager;
import org.mosaic.util.version.Version;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.osgi.framework.BundleEvent.*;

/**
 * @author arik
 */
final class ModuleManagerImpl implements ModuleManager
{
    private static final Logger LOG = LoggerFactory.getLogger( ModuleManagerImpl.class );

    @Nonnull
    private final Map<Long, ModuleImpl> modules = new ConcurrentHashMap<>( 100 );

    @Nonnull
    @Override
    public Optional<ModuleImpl> getModule( long id )
    {
        return Optional.fromNullable( this.modules.get( id ) );
    }

    @Nonnull
    @Override
    public Optional<ModuleImpl> getModule( @Nonnull String name, @Nonnull Version version )
    {
        for( ModuleImpl module : this.modules.values() )
        {
            if( name.equals( module.getName() ) && version.equals( module.getVersion() ) )
            {
                return Optional.of( module );
            }
        }
        return Optional.absent();
    }

    @Nonnull
    @Override
    public Collection<ModuleImpl> getModules()
    {
        return Collections.unmodifiableCollection( this.modules.values() );
    }

    @Nonnull
    @Override
    public Optional<ModuleImpl> getModuleFor( @Nonnull Object source )
    {
        if( source instanceof Long )
        {
            return getModule( ( Long ) source );
        }
        else if( source instanceof Bundle )
        {
            return getModule( ( ( Bundle ) source ).getBundleId() );
        }
        else if( source instanceof BundleContext )
        {
            return getModule( ( ( BundleContext ) source ).getBundle().getBundleId() );
        }
        else if( source instanceof Module )
        {
            return Optional.of( ( ModuleImpl ) source );
        }
        else
        {
            return Optional.absent();
        }
    }

    void open( @Nonnull BundleContext bundleContext ) throws BundleException
    {
        // synchronize our modules with existing OSGi state
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
        Optional<ModuleImpl> module = getModule( bundle.getBundleId() );
        if( !module.isPresent() )
        {
            module = Optional.of( new ModuleImpl( this, bundle ) );
            this.modules.put( bundle.getBundleId(), module.get() );
        }

        switch( bundleEvent.getType() )
        {
            case INSTALLED:
                module.get().onBundleInstalled();
                break;

            case RESOLVED:
                module.get().onBundleResolved();
                break;

            case STARTING:
                module.get().onBundleStarting();
                break;

            case STARTED:
                module.get().onBundleStarted();
                break;

            case STOPPING:
                module.get().onBundleStopping();
                break;

            case STOPPED:
                module.get().onBundleStopped();
                break;

            case UPDATED:
                module.get().onBundleUpdated();
                break;

            case UNRESOLVED:
                module.get().onBundleUnresolved();
                break;

            case UNINSTALLED:
                module.get().onBundleUninstalled();
                break;
        }
    }
}

package org.mosaic.modules.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Module;
import org.mosaic.modules.ModuleContext;
import org.mosaic.modules.ModuleManager;
import org.mosaic.modules.Version;
import org.osgi.framework.*;

import static org.osgi.framework.BundleEvent.*;

/**
 * @author arik
 */
final class ModuleManagerImpl implements ModuleManager, ModuleContext
{
    @Nonnull
    private final Map<Long, ModuleImpl> modules = new ConcurrentHashMap<>( 100 );

    @Nullable
    private Version serverVersion;

    private boolean developmentMode;

    @Nullable
    private Path serverHome;

    @Nullable
    private Path serverAppsHome;

    @Nullable
    private Path serverEtcHome;

    @Nullable
    private Path serverLibHome;

    @Nullable
    private Path serverLogsHome;

    @Nullable
    private Path serverWorkHome;

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

    @Nonnull
    @Override
    public Version getServerVersion()
    {
        if( this.serverVersion == null )
        {
            throw new IllegalStateException( "not initialized" );
        }
        return this.serverVersion;
    }

    @Override
    public boolean isDevelopmentMode()
    {
        return this.developmentMode;
    }

    @Nonnull
    @Override
    public Path getServerHome()
    {
        if( this.serverHome == null )
        {
            throw new IllegalStateException( "not initialized" );
        }
        return this.serverHome;
    }

    @Nonnull
    @Override
    public Path getServerAppsHome()
    {
        if( this.serverAppsHome == null )
        {
            throw new IllegalStateException( "not initialized" );
        }
        return this.serverAppsHome;
    }

    @Nonnull
    @Override
    public Path getServerEtcHome()
    {
        if( this.serverEtcHome == null )
        {
            throw new IllegalStateException( "not initialized" );
        }
        return this.serverEtcHome;
    }

    @Nonnull
    @Override
    public Path getServerLibHome()
    {
        if( this.serverLibHome == null )
        {
            throw new IllegalStateException( "not initialized" );
        }
        return this.serverLibHome;
    }

    @Nonnull
    @Override
    public Path getServerLogsHome()
    {
        if( this.serverLogsHome == null )
        {
            throw new IllegalStateException( "not initialized" );
        }
        return this.serverLogsHome;
    }

    @Nonnull
    @Override
    public Path getServerWorkHome()
    {
        if( this.serverWorkHome == null )
        {
            throw new IllegalStateException( "not initialized" );
        }
        return this.serverWorkHome;
    }

    void open( @Nonnull BundleContext bundleContext )
    {
        this.serverVersion = new Version( bundleContext.getProperty( "mosaic.version" ) );
        this.developmentMode = Boolean.parseBoolean( bundleContext.getProperty( "mosaic.devMode" ) );
        this.serverHome = Paths.get( bundleContext.getProperty( "mosaic.home" ) );
        this.serverAppsHome = Paths.get( bundleContext.getProperty( "mosaic.home.apps" ) );
        this.serverEtcHome = Paths.get( bundleContext.getProperty( "mosaic.home.etc" ) );
        this.serverLibHome = Paths.get( bundleContext.getProperty( "mosaic.home.lib" ) );
        this.serverLogsHome = Paths.get( bundleContext.getProperty( "mosaic.home.logs" ) );
        this.serverWorkHome = Paths.get( bundleContext.getProperty( "mosaic.home.work" ) );

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

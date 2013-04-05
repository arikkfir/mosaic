package org.mosaic.lifecycle.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.*;
import org.mosaic.lifecycle.impl.util.ServiceUtils;
import org.mosaic.util.reflection.impl.MethodHandleFactoryImpl;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import static java.util.Collections.unmodifiableCollection;

/**
 * @author arik
 */
public class ModuleManagerImpl implements ModuleManager, SynchronousBundleListener, InitializingBean, DisposableBean
{
    private static final Logger LOG = LoggerFactory.getLogger( ModuleManagerImpl.class );

    @Nonnull
    private final Map<Long, ModuleImpl> modules = new HashMap<>();

    @Nonnull
    private final BundleContext bundleContext;

    @Nonnull
    private final MethodHandleFactoryImpl methodHandleFactory;

    @Nonnull
    private final ServiceTracker<ModuleListener, ModuleListener> moduleListeners;

    @Nullable
    private ServiceRegistration<ModuleManager> moduleManagerRegistration;

    public ModuleManagerImpl( @Nonnull BundleContext bundleContext,
                              @Nonnull MethodHandleFactoryImpl methodHandleFactory )
    {
        this.bundleContext = bundleContext;
        this.methodHandleFactory = methodHandleFactory;
        this.moduleListeners = new ServiceTracker<>( bundleContext, ModuleListener.class, new ServiceTrackerCustomizer<ModuleListener, ModuleListener>()
        {
            @Override
            public ModuleListener addingService( ServiceReference<ModuleListener> reference )
            {
                ModuleListener listener = ModuleManagerImpl.this.bundleContext.getService( reference );
                if( listener != null )
                {
                    for( ModuleImpl module : modules.values() )
                    {
                        if( module.getState() != ModuleState.UNINSTALLED )
                        {
                            listener.moduleInstalled( module );
                        }
                        if( module.getState() == ModuleState.ACTIVE )
                        {
                            listener.moduleActivated( module );
                        }
                    }
                }
                return listener;
            }

            @Override
            public void modifiedService( ServiceReference<ModuleListener> reference, ModuleListener service )
            {
                // no-op
            }

            @Override
            public void removedService( ServiceReference<ModuleListener> reference, ModuleListener service )
            {
                // no-op
            }
        } );
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        for( Bundle bundle : bundleContext.getBundles() )
        {
            ModuleImpl module = new ModuleImpl( this, this.methodHandleFactory, bundle );
            this.modules.put( module.getId(), module );
            switch( bundle.getState() )
            {
                case Bundle.INSTALLED:
                    bundleChanged( new BundleEvent( BundleEvent.INSTALLED, bundle, bundleContext.getBundle() ) );
                    break;
                case Bundle.RESOLVED:
                    bundleChanged( new BundleEvent( BundleEvent.INSTALLED, bundle, bundleContext.getBundle() ) );
                    bundleChanged( new BundleEvent( BundleEvent.RESOLVED, bundle, bundleContext.getBundle() ) );
                    break;
                case Bundle.STARTING:
                    bundleChanged( new BundleEvent( BundleEvent.INSTALLED, bundle, bundleContext.getBundle() ) );
                    bundleChanged( new BundleEvent( BundleEvent.RESOLVED, bundle, bundleContext.getBundle() ) );
                    bundleChanged( new BundleEvent( BundleEvent.STARTING, bundle, bundleContext.getBundle() ) );
                    break;
                case Bundle.ACTIVE:
                    bundleChanged( new BundleEvent( BundleEvent.INSTALLED, bundle, bundleContext.getBundle() ) );
                    bundleChanged( new BundleEvent( BundleEvent.RESOLVED, bundle, bundleContext.getBundle() ) );
                    bundleChanged( new BundleEvent( BundleEvent.STARTING, bundle, bundleContext.getBundle() ) );
                    bundleChanged( new BundleEvent( BundleEvent.STARTED, bundle, bundleContext.getBundle() ) );
                    break;
            }
        }

        this.moduleListeners.open();
        this.bundleContext.addBundleListener( this );
        this.moduleManagerRegistration = ServiceUtils.register( bundleContext, ModuleManager.class, this );
    }

    @Override
    public void destroy() throws Exception
    {
        for( ModuleImpl module : this.modules.values() )
        {
            try
            {
                module.stop();
            }
            catch( ModuleStopException e )
            {
                LOG.error( "Could not stop module '{}': {}", module, e.getMessage(), e );
            }
        }

        this.moduleManagerRegistration = ServiceUtils.unregister( this.moduleManagerRegistration );
        this.bundleContext.removeBundleListener( this );
        this.moduleListeners.close();
    }

    @Nullable
    @Override
    public Module getModule( long id )
    {
        return this.modules.get( id );
    }

    @Nullable
    @Override
    public Module getModule( @Nonnull String name )
    {
        for( ModuleImpl module : this.modules.values() )
        {
            if( name.equals( module.getBundle().getSymbolicName() ) )
            {
                return module;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Module getModuleFor( @Nonnull Object target )
    {
        Bundle bundle = FrameworkUtil.getBundle( target instanceof Class ? ( Class<?> ) target : target.getClass() );
        if( bundle == null )
        {
            return null;
        }
        else
        {
            return getModule( bundle.getBundleId() );
        }
    }

    @Nonnull
    @Override
    public Collection<Module> getModules()
    {
        Collection<Module> modules = new LinkedList<>();
        modules.addAll( this.modules.values() );
        return unmodifiableCollection( modules );
    }

    @Override
    public void bundleChanged( BundleEvent event )
    {
        switch( event.getType() )
        {
            case BundleEvent.INSTALLED:
            {
                ModuleImpl module = new ModuleImpl( this, this.methodHandleFactory, event.getBundle() );
                this.modules.put( module.getId(), module );
                module.handleBundleEvent( event );
                break;
            }
            case BundleEvent.UNINSTALLED:
            {
                ModuleImpl module = this.modules.get( event.getBundle().getBundleId() );
                if( module != null )
                {
                    module.handleBundleEvent( event );
                    this.modules.remove( module.getId() );
                }
                break;
            }
            default:
            {
                ModuleImpl module = this.modules.get( event.getBundle().getBundleId() );
                if( module != null )
                {
                    module.handleBundleEvent( event );
                }
                break;
            }
        }
    }

    public void notifyModuleInstalled( @Nonnull ModuleImpl module )
    {
        Object[] listeners = this.moduleListeners.getServices();
        if( listeners != null )
        {
            for( Object listener : listeners )
            {
                try
                {
                    ModuleListener moduleListener = ( ModuleListener ) listener;
                    moduleListener.moduleInstalled( module );
                }
                catch( Exception e )
                {
                    LOG.warn( "Module listener '{}' threw an exception: {}", listener, e.getMessage(), e );
                }
            }
        }
    }

    public void notifyModuleActivated( @Nonnull ModuleImpl module )
    {
        Object[] listeners = this.moduleListeners.getServices();
        if( listeners != null )
        {
            for( Object listener : listeners )
            {
                try
                {
                    ModuleListener moduleListener = ( ModuleListener ) listener;
                    moduleListener.moduleActivated( module );
                }
                catch( Exception e )
                {
                    LOG.warn( "Module listener '{}' threw an exception: {}", listener, e.getMessage(), e );
                }
            }
        }
    }

    public void notifyModuleDeactivated( @Nonnull ModuleImpl module )
    {
        Object[] listeners = this.moduleListeners.getServices();
        if( listeners != null )
        {
            for( Object listener : listeners )
            {
                try
                {
                    ModuleListener moduleListener = ( ModuleListener ) listener;
                    moduleListener.moduleDeactivated( module );
                }
                catch( Exception e )
                {
                    LOG.warn( "Module listener '{}' threw an exception: {}", listener, e.getMessage(), e );
                }
            }
        }
    }

    public void notifyModuleUninstalled( @Nonnull ModuleImpl module )
    {
        Object[] listeners = this.moduleListeners.getServices();
        if( listeners != null )
        {
            for( Object listener : listeners )
            {
                try
                {
                    ModuleListener moduleListener = ( ModuleListener ) listener;
                    moduleListener.moduleUninstalled( module );
                }
                catch( Exception e )
                {
                    LOG.warn( "Module listener '{}' threw an exception: {}", listener, e.getMessage(), e );
                }
            }
        }
    }
}

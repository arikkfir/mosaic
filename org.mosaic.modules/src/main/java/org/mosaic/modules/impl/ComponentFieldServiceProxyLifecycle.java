package org.mosaic.modules.impl;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.*;
import org.mosaic.modules.ServiceReference;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.osgi.FilterBuilder;
import org.mosaic.util.pair.Pair;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class ComponentFieldServiceProxyLifecycle extends ComponentField implements InvocationHandler,
                                                                                  ServiceTrackerCustomizer,
                                                                                  ModuleWiring.ServiceRequirement
{
    @Nonnull
    private final Class<?> serviceType;

    @Nullable
    private final String filter;

    @Nonnull
    private final Object proxy;

    @Nonnull
    private final ServiceTracker serviceTracker;

    @Nullable
    private org.osgi.framework.ServiceReference<?> serviceReference;

    @Nullable
    private Object service;

    ComponentFieldServiceProxyLifecycle( @Nonnull ComponentDescriptorImpl<?> componentDescriptor, @Nonnull Field field )
    {
        super( componentDescriptor, field );

        Service serviceAnn = field.getAnnotation( Service.class );
        if( serviceAnn.value().length > 0 )
        {
            String msg = "field '" + field.getName() + "' of component " + componentDescriptor + " defines the 'value' attribute on its @Service annotation (it should not)";
            throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), componentDescriptor.getModule() );
        }

        Pair<Class<?>, FilterBuilder> pair = ComponentDescriptorImpl.getServiceAndFilterFromType( this.componentDescriptor.getModule(), componentDescriptor.getComponentType(), field.getGenericType() );
        this.serviceType = pair.getKey();

        FilterBuilder filterBuilder = pair.getRight();
        for( Service.P property : serviceAnn.properties() )
        {
            filterBuilder.addEquals( property.key(), property.value() );
        }
        this.filter = filterBuilder.toString();

        BundleWiring bundleWiring = componentDescriptor.getModule().getBundle().adapt( BundleWiring.class );
        if( bundleWiring == null )
        {
            throw new IllegalStateException( "module " + componentDescriptor.getModule() + " has no bundle wiring" );
        }
        this.proxy = Proxy.newProxyInstance( bundleWiring.getClassLoader(), new Class[] { field.getType() }, this );

        try
        {
            BundleContext bundleContext = componentDescriptor.getModule().getBundle().getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "no bundle context for module " + componentDescriptor.getModule() );
            }
            Filter filter = FrameworkUtil.createFilter( this.filter );
            this.serviceTracker = new ServiceTracker( bundleContext, filter, this );
        }
        catch( InvalidSyntaxException e )
        {
            String msg = "field '" + field.getName() + "' of component " + componentDescriptor + " defines illegal filter: " + this.filter;
            throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), componentDescriptor.getModule() );
        }
    }

    @Nonnull
    @Override
    public Module getConsumer()
    {
        return this.componentDescriptor.getModule();
    }

    @Nonnull
    @Override
    public Class<?> getType()
    {
        return this.serviceType;
    }

    @Nullable
    @Override
    public String getFilter()
    {
        return this.filter;
    }

    @Nonnull
    @Override
    public List<ServiceReference<?>> getReferences()
    {
        org.osgi.framework.ServiceReference serviceReference = this.serviceTracker.getServiceReference();
        if( serviceReference != null )
        {
            return Arrays.<ServiceReference<?>>asList( new ServiceReferenceImpl( serviceReference ) );
        }
        else
        {
            return Collections.emptyList();
        }
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
    {
        Object service = this.service;
        if( service == null )
        {
            throw new IllegalStateException( "service '" + this.serviceType.getName() + "' is not available" );
        }

        switch( method.getName() )
        {
            case "equals":
                return equals( args[ 0 ] );
            case "hashCode":
                return hashCode();
            default:
                try
                {
                    return method.invoke( service, args );
                }
                catch( IllegalAccessException e )
                {
                    throw new IllegalStateException( e.getMessage(), e );
                }
                catch( InvocationTargetException e )
                {
                    throw e.getCause();
                }
        }
    }

    @Override
    public Object addingService( @Nonnull org.osgi.framework.ServiceReference reference )
    {
        Object service = this.service;
        if( service == null )
        {
            BundleContext bundleContext = this.componentDescriptor.getModule().getBundle().getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "no bundle context for module " + this.componentDescriptor.getModule() );
            }

            service = bundleContext.getService( reference );
            if( service != null )
            {
                this.serviceReference = reference;
                this.service = service;
                try
                {
                    this.componentDescriptor.getModule().activate();
                }
                catch( Exception ignore )
                {
                    // no need to do something - this was just a "activate if possible" attempt
                }
            }
        }
        return service;
    }

    @Override
    public void modifiedService( @Nonnull org.osgi.framework.ServiceReference reference, @Nonnull Object service )
    {
        // no-op
    }

    @Override
    public void removedService( @Nonnull org.osgi.framework.ServiceReference reference, @Nonnull Object service )
    {
        if( this.service == service )
        {
            org.osgi.framework.ServiceReference newReference = this.serviceTracker.getServiceReference();
            if( newReference != null )
            {
                Object newService = this.serviceTracker.getService( newReference );
                if( newService != null )
                {
                    this.serviceReference = newReference;
                    this.service = newService;
                    return;
                }
            }

            this.serviceReference = null;
            this.service = null;
            this.componentDescriptor.getModule().deactivate();
        }
    }

    @Nullable
    @Override
    protected String toStringInternal()
    {
        return "@Service '" + super.toStringInternal() + "'";
    }

    @Override
    protected synchronized boolean canActivateInternal()
    {
        return this.service != null;
    }

    @Override
    protected synchronized void onAfterStart()
    {
        this.serviceTracker.open();
    }

    @Override
    protected synchronized void onBeforeStop()
    {
        this.serviceTracker.close();
    }

    @Override
    protected synchronized void onAfterDeactivate()
    {
        this.serviceReference = null;
        this.service = null;
    }

    @Nonnull
    @Override
    protected Object getValue()
    {
        return this.proxy;
    }

    private class ServiceReferenceImpl implements ServiceReference
    {
        @Nonnull
        private final org.osgi.framework.ServiceReference<?> reference;

        private ServiceReferenceImpl( @Nonnull org.osgi.framework.ServiceReference<?> reference )
        {
            this.reference = reference;
        }

        @Override
        public long getId()
        {
            return ( Long ) this.reference.getProperty( Constants.SERVICE_ID );
        }

        @Nonnull
        @Override
        public Class getType()
        {
            return ComponentFieldServiceProxyLifecycle.this.serviceType;
        }

        @Nullable
        @Override
        public Module getProvider()
        {
            return Activator.getModuleManager().getModule( this.reference.getBundle().getBundleId() );
        }

        @Nonnull
        @Override
        public MapEx<String, Object> getProperties()
        {
            String[] propertyKeys = this.reference.getPropertyKeys();

            MapEx<String, Object> properties = new HashMapEx<>( propertyKeys.length );
            for( String key : propertyKeys )
            {
                properties.put( key, this.reference.getProperty( key ) );
            }
            return properties;
        }

        @Nullable
        @Override
        public Object get()
        {
            return ComponentFieldServiceProxyLifecycle.this.serviceTracker.getService( this.reference );
        }

        @Nonnull
        @Override
        public Object require()
        {
            Object service = get();
            if( service != null )
            {
                return service;
            }
            else
            {
                String typeName = ComponentFieldServiceProxyLifecycle.this.serviceType.getName();
                throw new IllegalStateException( "service of type '" + typeName + "' is not available" );
            }
        }
    }
}

package org.mosaic.modules.impl;

import com.google.common.base.Optional;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.ComponentDefinitionException;
import org.mosaic.modules.Module;
import org.mosaic.modules.Service;
import org.mosaic.modules.ServiceReference;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.osgi.FilterBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class TypeDescriptorFieldServiceProxy extends TypeDescriptorField
        implements InvocationHandler, ServiceTrackerCustomizer, Module.ServiceRequirement
{
    @Nonnull
    private final ServiceTypeHandle.Token<?> serviceType;

    @Nullable
    private final String filter;

    @Nonnull
    private final Object proxy;

    @Nonnull
    private final ServiceTracker serviceTracker;

    @Nullable
    private Object service;

    TypeDescriptorFieldServiceProxy( @Nonnull TypeDescriptor typeDescriptor, @Nonnull Field field )
    {
        super( typeDescriptor, field );

        Service serviceAnn = field.getAnnotation( Service.class );
        if( serviceAnn.value().length > 0 )
        {
            String msg = "field '" + field.getName() + "' of component " + this.typeDescriptor + " defines the 'value' attribute on its @Service annotation (it should not)";
            throw new ComponentDefinitionException( msg, this.typeDescriptor.getType(), this.typeDescriptor.getModule() );
        }

        this.serviceType = ServiceTypeHandle.createToken( field.getGenericType(),
                                                          ServiceTypeHandle.ServiceToken.class,
                                                          ServiceTypeHandle.MethodEndpointServiceToken.class,
                                                          ServiceTypeHandle.ServiceTemplateServiceToken.class,
                                                          ServiceTypeHandle.ServiceReferenceToken.class );

        FilterBuilder filterBuilder = this.serviceType.createFilterBuilder();
        for( Service.P property : serviceAnn.properties() )
        {
            filterBuilder.addEquals( property.key(), property.value() );
        }
        this.filter = filterBuilder.toString();

        BundleWiring bundleWiring = typeDescriptor.getModule().getBundle().adapt( BundleWiring.class );
        if( bundleWiring == null )
        {
            throw new IllegalStateException( "module " + typeDescriptor.getModule() + " has no bundle wiring" );
        }
        this.proxy = Proxy.newProxyInstance( bundleWiring.getClassLoader(), new Class[] { field.getType() }, this );

        try
        {
            BundleContext bundleContext = typeDescriptor.getModule().getBundle().getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "no bundle context for module " + typeDescriptor.getModule() );
            }
            this.serviceTracker = new ServiceTracker( bundleContext, FrameworkUtil.createFilter( this.filter ), this );
        }
        catch( InvalidSyntaxException e )
        {
            String msg = "field '" + field.getName() + "' of component " + typeDescriptor + " defines illegal filter: " + this.filter;
            throw new ComponentDefinitionException( msg, typeDescriptor.getType(), typeDescriptor.getModule() );
        }
    }

    @Nonnull
    @Override
    public Module getConsumer()
    {
        return this.typeDescriptor.getModule();
    }

    @Nonnull
    @Override
    public Class<?> getType()
    {
        return this.serviceType.getServiceClass();
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
            throw new IllegalStateException( "service '" + this.serviceType.getServiceClass().getName() + "' is not available" );
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
            BundleContext bundleContext = this.typeDescriptor.getModule().getBundle().getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "no bundle context for module " + this.typeDescriptor.getModule() );
            }

            service = bundleContext.getService( reference );
            if( service != null )
            {
                this.service = service;
                try
                {
                    this.typeDescriptor.getModule().activate();
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
                    this.service = newService;
                    return;
                }
            }

            this.service = null;
            this.typeDescriptor.getModule().deactivate();
        }
    }

    @Nullable
    @Override
    public String toString()
    {
        return "TypeDescriptorFieldServiceProxy[" + super.toString() + "]";
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
            return TypeDescriptorFieldServiceProxy.this.serviceType.getServiceClass();
        }

        @Nullable
        @Override
        public Module getProvider()
        {
            return Activator.getModuleManager().getModule( this.reference.getBundle().getBundleId() ).orNull();
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

        @Nonnull
        @Override
        public Optional<?> service()
        {
            return Optional.fromNullable( TypeDescriptorFieldServiceProxy.this.serviceTracker.getService( this.reference ) );
        }
    }
}

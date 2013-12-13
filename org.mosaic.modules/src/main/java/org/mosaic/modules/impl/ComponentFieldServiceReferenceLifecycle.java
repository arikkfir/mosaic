package org.mosaic.modules.impl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
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
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class ComponentFieldServiceReferenceLifecycle extends ComponentField implements ServiceReference,
                                                                                      ModuleWiring.ServiceRequirement
{
    @Nonnull
    private final Class serviceType;

    @Nullable
    private final String filter;

    @Nonnull
    private final ServiceTracker serviceTracker;

    ComponentFieldServiceReferenceLifecycle( @Nonnull ComponentDescriptorImpl<?> componentDescriptor,
                                             @Nonnull Field field )
    {
        super( componentDescriptor, field );

        Service serviceAnn = field.getAnnotation( Service.class );
        if( serviceAnn.value().length > 0 )
        {
            String msg = "field '" + field.getName() + "' of component " + componentDescriptor + " defines the 'value' attribute on its @Service annotation (it should not)";
            throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), componentDescriptor.getModule() );
        }

        java.lang.reflect.Type serviceRefType = field.getGenericType();
        if( !( serviceRefType instanceof ParameterizedType ) )
        {
            String msg = "field '" + field.getName() + "' of component " + componentDescriptor + " does not specify type for ServiceReference";
            throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), componentDescriptor.getModule() );
        }

        ParameterizedType parameterizedServiceRefType = ( ParameterizedType ) serviceRefType;
        java.lang.reflect.Type[] serviceRefTypeArguments = parameterizedServiceRefType.getActualTypeArguments();
        if( serviceRefTypeArguments.length == 0 )
        {
            String msg = "field '" + field.getName() + "' of component " + componentDescriptor + " does not specify type for ServiceReference";
            throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), componentDescriptor.getModule() );
        }

        Pair<Class<?>, FilterBuilder> pair = ComponentDescriptorImpl.getServiceAndFilterFromType( componentDescriptor.getModule(), componentDescriptor.getComponentType(), serviceRefTypeArguments[ 0 ] );
        this.serviceType = pair.getKey();

        FilterBuilder filterBuilder = pair.getRight();
        for( Service.P property : serviceAnn.properties() )
        {
            filterBuilder.addEquals( property.key(), property.value() );
        }
        this.filter = filterBuilder.toString();

        try
        {
            BundleContext bundleContext = componentDescriptor.getModule().getBundle().getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "no bundle context for module " + componentDescriptor.getModule() );
            }
            Filter filter = FrameworkUtil.createFilter( this.filter );
            this.serviceTracker = new ServiceTracker( bundleContext, filter, null );
        }
        catch( InvalidSyntaxException e )
        {
            String msg = "field '" + field.getName() + "' of component " + componentDescriptor + " defines illegal filter: " + this.filter;
            throw new ComponentDefinitionException( msg, componentDescriptor.getComponentType(), componentDescriptor.getModule() );
        }
    }

    @Override
    public long getId()
    {
        org.osgi.framework.ServiceReference reference = this.serviceTracker.getServiceReference();
        if( reference == null )
        {
            throw new IllegalStateException( "service not available" );
        }

        Long id = ( Long ) reference.getProperty( Constants.SERVICE_ID );
        if( id == null )
        {
            throw new IllegalStateException( "service has no ID" );
        }

        return id;
    }

    @Nonnull
    @Override
    public Class getType()
    {
        return this.serviceType;
    }

    @Nullable
    @Override
    public Module getProvider()
    {
        org.osgi.framework.ServiceReference ref = this.serviceTracker.getServiceReference();
        return ref != null ? Activator.getModuleManager().getModule( ref.getBundle().getBundleId() ) : null;
    }

    @Nonnull
    @Override
    public MapEx<String, Object> getProperties()
    {
        org.osgi.framework.ServiceReference ref = this.serviceTracker.getServiceReference();
        String[] propertyKeys = ref.getPropertyKeys();

        MapEx<String, Object> properties = new HashMapEx<>( propertyKeys.length );
        for( String key : propertyKeys )
        {
            properties.put( key, ref.getProperty( key ) );
        }
        return properties;
    }

    @Nullable
    @Override
    public Object get()
    {
        return this.serviceTracker.getService();
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
            throw new IllegalStateException( "service of type '" + this.serviceType.getName() + "' is not available" );
        }
    }

    @Nonnull
    @Override
    public Module getConsumer()
    {
        return this.componentDescriptor.getModule();
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
        return this.serviceTracker.isEmpty()
               ? Collections.<ServiceReference<?>>emptyList()
               : Arrays.<ServiceReference<?>>asList( this );
    }

    @Nullable
    @Override
    protected String toStringInternal()
    {
        return "@Service '" + super.toStringInternal() + "'";
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

    @Nonnull
    @Override
    protected Object getValue()
    {
        return this;
    }
}
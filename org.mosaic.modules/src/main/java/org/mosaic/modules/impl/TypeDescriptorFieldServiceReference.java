package org.mosaic.modules.impl;

import com.google.common.base.Optional;
import java.lang.reflect.Field;
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
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class TypeDescriptorFieldServiceReference extends TypeDescriptorField
        implements ServiceReference, Module.ServiceRequirement
{
    @Nonnull
    private final ServiceTypeHandle.Token<?> serviceType;

    @Nullable
    private final String filter;

    @Nonnull
    private final ServiceTracker serviceTracker;

    TypeDescriptorFieldServiceReference( @Nonnull TypeDescriptor typeDescriptor,
                                         @Nonnull Field field )
    {
        super( typeDescriptor, field );

        Service serviceAnn = field.getAnnotation( Service.class );
        if( serviceAnn.value().length > 0 )
        {
            String msg = "field '" + field.getName() + "' of component " + this.typeDescriptor + " defines the 'value' attribute on its @Service annotation (it should not)";
            throw new ComponentDefinitionException( msg, this.typeDescriptor.getType(), this.typeDescriptor.getModule() );
        }

        this.serviceType = ServiceTypeHandle.createToken( field.getGenericType(), ServiceTypeHandle.ServiceReferenceToken.class );

        FilterBuilder filterBuilder = this.serviceType.createFilterBuilder();
        for( Service.P property : serviceAnn.properties() )
        {
            filterBuilder.addEquals( property.key(), property.value() );
        }
        this.filter = filterBuilder.toString();

        try
        {
            BundleContext bundleContext = this.typeDescriptor.getModule().getBundle().getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "no bundle context for module " + this.typeDescriptor.getModule() );
            }
            this.serviceTracker = new ServiceTracker( bundleContext, FrameworkUtil.createFilter( this.filter ), null );
        }
        catch( InvalidSyntaxException e )
        {
            String msg = "field '" + field.getName() + "' of component " + this.typeDescriptor + " defines illegal filter: " + this.filter;
            throw new ComponentDefinitionException( msg, this.typeDescriptor.getType(), this.typeDescriptor.getModule() );
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
        return this.serviceType.getServiceClass();
    }

    @Nullable
    @Override
    public Module getProvider()
    {
        org.osgi.framework.ServiceReference ref = this.serviceTracker.getServiceReference();
        return ref != null ? Activator.getModuleManager().getModule( ref.getBundle().getBundleId() ).orNull() : null;
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

    @Nonnull
    @Override
    public Optional<?> service()
    {
        return Optional.fromNullable( this.serviceTracker.getService() );
    }

    @Nonnull
    @Override
    public Module getConsumer()
    {
        return this.typeDescriptor.getModule();
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
    public String toString()
    {
        return "TypeDescriptorFieldServiceReference[" + super.toString() + "]";
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

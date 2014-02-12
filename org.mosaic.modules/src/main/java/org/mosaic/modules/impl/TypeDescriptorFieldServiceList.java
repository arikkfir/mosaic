package org.mosaic.modules.impl;

import com.google.common.base.Optional;
import java.lang.reflect.Field;
import java.util.*;
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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import static org.osgi.framework.FrameworkUtil.createFilter;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class TypeDescriptorFieldServiceList extends TypeDescriptorField
        implements ServiceTrackerCustomizer, List, Module.ServiceRequirement
{
    @Nonnull
    private final ServiceTypeHandle.ServiceListToken serviceType;

    @Nullable
    private final String requiredFilter;

    @Nonnull
    private final ServiceTracker serviceTracker;

    @Nullable
    private List services;

    TypeDescriptorFieldServiceList( @Nonnull TypeDescriptor typeDescriptor, @Nonnull Field field )
    {
        super( typeDescriptor, field );

        Service serviceAnn = field.getAnnotation( Service.class );
        if( serviceAnn.value().length > 0 )
        {
            String msg = "field '" + field.getName() + "' of component " + typeDescriptor + " defines the 'value' attribute on its @Service annotation (it should not)";
            throw new ComponentDefinitionException( msg, typeDescriptor.getType(), typeDescriptor.getModule() );
        }

        this.serviceType = ( ServiceTypeHandle.ServiceListToken )
                ServiceTypeHandle.createToken( field.getGenericType(), ServiceTypeHandle.ServiceListToken.class );

        FilterBuilder filterBuilder = this.serviceType.createFilterBuilder();
        for( Service.P property : serviceAnn.properties() )
        {
            filterBuilder.addEquals( property.key(), property.value() );
        }
        this.requiredFilter = filterBuilder.toString();

        try
        {
            BundleContext bundleContext = typeDescriptor.getModule().getBundle().getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "no bundle context for module " + typeDescriptor.getModule() );
            }
            this.serviceTracker = new ServiceTracker( bundleContext, createFilter( this.requiredFilter ), this );
        }
        catch( InvalidSyntaxException e )
        {
            String msg = "field '" + field.getName() + "' of component " + typeDescriptor + " defines illegal filter: " + this.requiredFilter;
            throw new ComponentDefinitionException( msg, typeDescriptor.getType(), typeDescriptor.getModule() );
        }
    }

    @Override
    public Object addingService( @Nonnull org.osgi.framework.ServiceReference reference )
    {
        BundleContext bundleContext = this.typeDescriptor.getModule().getBundle().getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "no bundle context for module " + this.typeDescriptor.getModule() );
        }
        Object service = bundleContext.getService( reference );
        this.services = null;
        return service;
    }

    @Override
    public void modifiedService( @Nonnull org.osgi.framework.ServiceReference reference,
                                 @Nonnull Object service )
    {
        // no-op
    }

    @Override
    public void removedService( @Nonnull org.osgi.framework.ServiceReference reference,
                                @Nonnull Object service )
    {
        this.services = null;
    }

    @Override
    public int size()
    {
        return getServices().size();
    }

    @Override
    public boolean isEmpty()
    {
        return getServices().isEmpty();
    }

    @Override
    public boolean contains( Object o )
    {
        return getServices().contains( o );
    }

    @Nonnull
    @Override
    public Iterator iterator()
    {
        return getServices().iterator();
    }

    @Nonnull
    @Override
    public Object[] toArray()
    {
        return getServices().toArray();
    }

    @Nonnull
    @Override
    public Object[] toArray( @Nonnull Object[] a )
    {
        return getServices().toArray( a );
    }

    @Override
    public boolean add( Object o )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove( Object o )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll( @Nonnull Collection c )
    {
        return getServices().containsAll( c );
    }

    @Override
    public boolean addAll( @Nonnull Collection c )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll( int index, @Nonnull Collection c )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll( @Nonnull Collection c )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll( @Nonnull Collection c )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get( int index )
    {
        return getServices().get( index );
    }

    @Override
    public Object set( int index, Object element )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add( int index, Object element )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove( int index )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf( Object o )
    {
        return getServices().indexOf( o );
    }

    @Override
    public int lastIndexOf( Object o )
    {
        return getServices().lastIndexOf( o );
    }

    @Nonnull
    @Override
    public ListIterator listIterator()
    {
        return getServices().listIterator();
    }

    @Nonnull
    @Override
    public ListIterator listIterator( int index )
    {
        return getServices().listIterator( index );
    }

    @Nonnull
    @Override
    public List subList( int fromIndex, int toIndex )
    {
        return getServices().subList( fromIndex, toIndex );
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
        return this.requiredFilter;
    }

    @Nonnull
    @Override
    public List<ServiceReference<?>> getReferences()
    {
        org.osgi.framework.ServiceReference[] tracked = this.serviceTracker.getServiceReferences();
        if( tracked == null )
        {
            return Collections.emptyList();
        }
        else
        {
            List<ServiceReference<?>> serviceReferences = new LinkedList<>();
            for( org.osgi.framework.ServiceReference reference : tracked )
            {
                serviceReferences.add( new ServiceReferenceImpl( reference ) );
            }
            return serviceReferences;
        }
    }

    @Nullable
    @Override
    public String toString()
    {
        return "TypeDescriptorFieldServiceList[" + super.toString() + "]";
    }

    @Nonnull
    private List<?> getServices()
    {
        List services = this.services;
        if( services == null )
        {
            ServiceTypeHandle.Token<?> itemType = this.serviceType.getItemType();
            if( itemType instanceof ServiceTypeHandle.ServiceReferenceToken )
            {
                services = new ArrayList( getReferences() );
            }
            else
            {
                services = new ArrayList( this.serviceTracker.getTracked().values() );
            }
            this.services = services;
        }
        return services;
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
        this.services = null;
    }

    @Nonnull
    @Override
    protected Object getValue()
    {
        return this;
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
            return TypeDescriptorFieldServiceList.this.serviceType.getServiceClass();
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
            return Optional.fromNullable( TypeDescriptorFieldServiceList.this.serviceTracker.getService( this.reference ) );
        }
    }
}

package org.mosaic.modules.impl;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.*;
import org.mosaic.util.collections.EmptyMapEx;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.osgi.FilterBuilder;
import org.mosaic.util.pair.Pair;
import org.osgi.framework.*;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;
import static org.mosaic.modules.impl.Activator.getServiceAndFilterFromType;

/**
 * @author arik
 */
@SuppressWarnings( "unchecked" )
final class TypeDescriptorServiceAdapter extends Lifecycle
        implements ServiceTrackerCustomizer, ModuleWiring.ServiceRequirement, ServiceCapabilityProvider
{
    @Nonnull
    private final TypeDescriptor typeDescriptor;

    @Nonnull
    private final Class<?> adaptedType;

    @Nullable
    private final String filter;

    @Nonnull
    private final Constructor adapterConstructor;

    @Nonnull
    private final Class<?>[] serviceTypes;

    @Nonnull
    private final Map<String, Object> serviceProperties;

    @Nonnull
    private final ServiceTracker serviceTracker;

    @Nonnull
    private final Map<Long, Collection<ServiceRegistration<?>>> serviceAdaptations = new ConcurrentHashMap<>( 100 );

    TypeDescriptorServiceAdapter( @Nonnull TypeDescriptor typeDescriptor )
    {
        this.typeDescriptor = typeDescriptor;
        this.serviceTypes = getServiceInterfaces();
        this.serviceProperties = getServiceProperties();

        Pair<Constructor<?>, FilterBuilder> ctor = getServiceConstructor();
        this.adapterConstructor = ctor.getKey();
        this.adaptedType = this.adapterConstructor.getParameterTypes()[ 0 ];
        this.filter = ctor.getValue().toString();
        try
        {
            BundleContext bundleContext = this.typeDescriptor.getModule().getBundle().getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "no bundle context for module " + typeDescriptor.getModule() );
            }
            this.serviceTracker = new ServiceTracker( bundleContext, FrameworkUtil.createFilter( this.filter ), this );
        }
        catch( InvalidSyntaxException e )
        {
            String msg = "@Adapter '" + this + "' of component " + typeDescriptor + " defines illegal filter: " + this.filter;
            throw new ComponentDefinitionException( msg, this.typeDescriptor.getType(), this.typeDescriptor.getModule() );
        }
    }

    @Override
    public final String toString()
    {
        return "TypeDescriptorServiceAdapter[" + this.adaptedType.getSimpleName() + " as " + asList( this.serviceTypes ) + "]";
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
        return this.adaptedType;
    }

    @Nullable
    @Override
    public String getFilter()
    {
        return this.filter;
    }

    @Nonnull
    @Override
    public List<org.mosaic.modules.ServiceReference<?>> getReferences()
    {
        org.osgi.framework.ServiceReference[] tracked = this.serviceTracker.getServiceReferences();
        if( tracked == null )
        {
            return Collections.emptyList();
        }
        else
        {
            List<org.mosaic.modules.ServiceReference<?>> serviceReferences = new LinkedList<>();
            for( org.osgi.framework.ServiceReference reference : tracked )
            {
                serviceReferences.add( new ServiceReferenceImpl( reference ) );
            }
            return serviceReferences;
        }
    }

    @Nonnull
    @Override
    public List<ModuleWiring.ServiceCapability> getServiceCapabilities()
    {
        List<ModuleWiring.ServiceCapability> capabilities = new LinkedList<>();
        for( Collection<ServiceRegistration<?>> registrationsCollection : this.serviceAdaptations.values() )
        {
            for( ServiceRegistration<?> registration : registrationsCollection )
            {
                capabilities.add( new ServiceCapabilityImpl( registration ) );
            }
        }
        return capabilities;
    }

    @Override
    public Object addingService( @Nonnull ServiceReference reference )
    {
        BundleContext bundleContext = this.typeDescriptor.getModule().getBundle().getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "no bundle context for module " + typeDescriptor.getModule() );
        }
        Object service = bundleContext.getService( reference );
        if( service == null )
        {
            return null;
        }

        Object adapter;
        try
        {
            adapter = this.adapterConstructor.newInstance( service );
        }
        catch( Throwable e )
        {
            throw new ComponentCreateException( e, this.typeDescriptor.getType(), this.typeDescriptor.getModule() );
        }

        Dictionary<String, Object> properties = new Hashtable<>();
        for( Map.Entry<String, Object> entry : this.serviceProperties.entrySet() )
        {
            properties.put( entry.getKey(), entry.getValue() );
        }
        for( String propertyName : reference.getPropertyKeys() )
        {
            properties.put( propertyName, reference.getProperty( propertyName ) );
        }
        if( adapter instanceof ServicePropertiesProvider )
        {
            ( ( ServicePropertiesProvider ) adapter ).addProperties( properties );
        }

        Collection<ServiceRegistration<?>> registrations = new LinkedList<>();
        for( Class serviceType : this.serviceTypes )
        {
            registrations.add( bundleContext.registerService( serviceType, adapter, properties ) );
        }

        this.serviceAdaptations.put( ( Long ) reference.getProperty( Constants.SERVICE_ID ), registrations );
        return service;
    }

    @Override
    public void modifiedService( @Nonnull ServiceReference reference, @Nonnull Object service )
    {
        // no-op
    }

    @Override
    public void removedService( @Nonnull ServiceReference reference, @Nonnull Object service )
    {
        Long serviceId = ( Long ) reference.getProperty( Constants.SERVICE_ID );
        Collection<ServiceRegistration<?>> registrations = this.serviceAdaptations.remove( serviceId );
        if( registrations != null )
        {
            for( ServiceRegistration<?> registration : registrations )
            {
                try
                {
                    registration.unregister();
                }
                catch( Exception ignore )
                {
                }
            }
        }
    }

    @Override
    protected synchronized void onAfterActivate()
    {
        this.serviceTracker.open();
    }

    @Override
    protected synchronized void onBeforeDeactivate()
    {
        this.serviceTracker.close();

        Iterator<Map.Entry<Long, Collection<ServiceRegistration<?>>>> iterator = this.serviceAdaptations.entrySet().iterator();
        while( iterator.hasNext() )
        {
            Map.Entry<Long, Collection<ServiceRegistration<?>>> entry = iterator.next();
            for( ServiceRegistration<?> registration : entry.getValue() )
            {
                try
                {
                    registration.unregister();
                }
                catch( Exception ignore )
                {
                }
            }
            iterator.remove();
        }
    }

    @Nonnull
    private Class<?>[] getServiceInterfaces()
    {
        Class<?> type = this.typeDescriptor.getType();

        List<Class<?>> interfaces = new LinkedList<>();

        // add interfaces declared in @Service annotation to the service types list
        interfaces.addAll( asList( this.typeDescriptor.getType().getAnnotation( Adapter.class ).value() ) );

        // if none were declared in the annotation, add all interfaces implemented by the component
        if( interfaces.isEmpty() )
        {
            interfaces.addAll( asList( type.getInterfaces() ) );
        }

        // if no interfaces are implemented by the component and no interfaces declared in the annotation, fail
        if( interfaces.isEmpty() )
        {
            String msg = "@Adapter component must declare service interface (via 'implements' clause or in the @Adapter annotation)";
            throw new ComponentDefinitionException( msg, type, this.typeDescriptor.getModule() );
        }

        return interfaces.toArray( new Class[ interfaces.size() ] );
    }

    @Nonnull
    private Map<String, Object> getServiceProperties()
    {
        Map<String, Object> properties = new LinkedHashMap<>();
        for( Adapter.P property : this.typeDescriptor.getType().getAnnotation( Adapter.class ).properties() )
        {
            properties.put( property.key(), property.value() );
        }

        Ranking rankingAnn = this.typeDescriptor.getType().getAnnotation( Ranking.class );
        if( rankingAnn != null )
        {
            properties.put( Constants.SERVICE_RANKING, rankingAnn.value() );
        }

        return unmodifiableMap( properties );
    }

    @Nonnull
    private Pair<Constructor<?>, FilterBuilder> getServiceConstructor()
    {
        for( Constructor<?> ctor : this.typeDescriptor.getType().getDeclaredConstructors() )
        {
            Service serviceAnn = ctor.getAnnotation( Service.class );
            if( serviceAnn != null && ctor.getParameterTypes().length == 1 )
            {
                Pair<Class<?>, FilterBuilder> pair = getServiceAndFilterFromType( this.typeDescriptor.getModule(),
                                                                                  this.typeDescriptor.getType(),
                                                                                  ctor.getGenericParameterTypes()[ 0 ] );

                FilterBuilder filterBuilder = pair.getRight();
                for( Service.P property : serviceAnn.properties() )
                {
                    filterBuilder.addEquals( property.key(), property.value() );
                }

                ctor.setAccessible( true );
                return Pair.<Constructor<?>, FilterBuilder>of( ctor, filterBuilder );
            }
        }
        throw new ComponentDefinitionException( "@Adapter has no one-argument constructor annotated with @Adapter",
                                                this.typeDescriptor.getType(),
                                                this.typeDescriptor.getModule() );
    }

    private class ServiceReferenceImpl implements org.mosaic.modules.ServiceReference
    {
        @Nonnull
        private final org.osgi.framework.ServiceReference<?> reference;

        private ServiceReferenceImpl( @Nonnull ServiceReference<?> reference )
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
            return TypeDescriptorServiceAdapter.this.adaptedType;
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
            return TypeDescriptorServiceAdapter.this.serviceTracker.getService( this.reference );
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
                String typeName = TypeDescriptorServiceAdapter.this.adaptedType.getName();
                throw new IllegalStateException( "service of type '" + typeName + "' is not available" );
            }
        }
    }

    private class ServiceCapabilityImpl implements ModuleWiring.ServiceCapability
    {
        @Nonnull
        private final ServiceRegistration registration;

        private ServiceCapabilityImpl( @Nonnull ServiceRegistration registration )
        {
            this.registration = registration;
        }

        @Override
        public long getId()
        {
            ServiceReference reference = this.registration.getReference();
            if( reference != null )
            {
                Long id = ( Long ) reference.getProperty( Constants.SERVICE_ID );
                if( id != null )
                {
                    return id;
                }
            }
            throw new IllegalStateException( "id not found" );
        }

        @Nonnull
        @Override
        public Module getProvider()
        {
            return TypeDescriptorServiceAdapter.this.typeDescriptor.getModule();
        }

        @Nonnull
        @Override
        public Class<?> getType()
        {
            ServiceReference reference = this.registration.getReference();
            if( reference != null )
            {
                String[] objectClass = ( String[] ) reference.getProperty( Constants.OBJECTCLASS );
                ModuleImpl module = TypeDescriptorServiceAdapter.this.typeDescriptor.getModule();
                try
                {
                    return module.getModuleResources().loadClass( objectClass[ 0 ] );
                }
                catch( Throwable e )
                {
                    throw new IllegalStateException( "service type not found", e );
                }
            }
            throw new IllegalStateException( "service type not found" );
        }

        @Nonnull
        @Override
        public MapEx<String, Object> getProperties()
        {
            ServiceReference reference = this.registration.getReference();
            if( reference != null )
            {
                MapEx<String, Object> properties = new HashMapEx<>();
                for( String key : reference.getPropertyKeys() )
                {
                    properties.put( key, reference.getProperty( key ) );
                }
                return properties;
            }
            return EmptyMapEx.emptyMapEx();
        }

        @Nonnull
        @Override
        public Collection<Module> getConsumers()
        {
            ServiceReference<?> reference = this.registration.getReference();
            if( reference != null )
            {
                Bundle[] usingBundles = reference.getUsingBundles();
                if( usingBundles != null )
                {
                    List<Module> consumers = new LinkedList<>();
                    for( Bundle bundle : usingBundles )
                    {
                        consumers.add( Activator.getModuleManager().getModule( bundle.getBundleId() ) );
                    }
                    return consumers;
                }
            }
            return Collections.emptyList();
        }
    }
}

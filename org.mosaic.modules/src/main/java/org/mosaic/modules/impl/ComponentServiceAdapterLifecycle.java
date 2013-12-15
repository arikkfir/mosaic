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

import static org.mosaic.modules.impl.ComponentDescriptorImpl.getServiceAndFilterFromType;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class ComponentServiceAdapterLifecycle extends Lifecycle
        implements ServiceTrackerCustomizer, ModuleWiring.ServiceRequirement, ServiceCapabilityProvider
{
    @Nonnull
    private final ComponentDescriptorImpl<?> componentDescriptor;

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

    ComponentServiceAdapterLifecycle( @Nonnull ComponentDescriptorImpl<?> componentDescriptor )
    {
        this.componentDescriptor = componentDescriptor;

        Adapter adapterAnn = this.componentDescriptor.getComponentType().getAnnotation( Adapter.class );
        this.serviceTypes = adapterAnn.value();
        this.serviceProperties = new LinkedHashMap<>();

        Ranking rankingAnn = this.componentDescriptor.getComponentType().getAnnotation( Ranking.class );
        if( rankingAnn != null )
        {
            this.serviceProperties.put( Constants.SERVICE_RANKING, rankingAnn.value() );
        }

        for( Adapter.P property : adapterAnn.properties() )
        {
            this.serviceProperties.put( property.key(), property.value() );
        }

        FilterBuilder filterBuilder = null;
        Class<?> adaptedType = null;
        for( Constructor<?> ctor : this.componentDescriptor.getComponentType().getDeclaredConstructors() )
        {
            Service serviceAnn = ctor.getAnnotation( Service.class );
            if( serviceAnn != null && ctor.getParameterTypes().length == 1 )
            {
                Pair<Class<?>, FilterBuilder> pair = getServiceAndFilterFromType( this.componentDescriptor.getModule(),
                                                                                  this.componentDescriptor.getComponentType(),
                                                                                  ctor.getGenericParameterTypes()[ 0 ] );

                adaptedType = pair.getKey();

                filterBuilder = pair.getRight();
                for( Service.P property : serviceAnn.properties() )
                {
                    filterBuilder.addEquals( property.key(), property.value() );
                }
                break;
            }
        }
        if( filterBuilder == null )
        {
            throw new ComponentDefinitionException( "@Adapter has no one-argument constructor",
                                                    this.componentDescriptor.getComponentType(),
                                                    this.componentDescriptor.getModule() );
        }
        this.adaptedType = adaptedType;
        this.filter = filterBuilder.toString();

        try
        {
            BundleContext bundleContext = this.componentDescriptor.getModule().getBundle().getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "no bundle context for module " + componentDescriptor.getModule() );
            }
            Filter filter = FrameworkUtil.createFilter( this.filter );
            this.serviceTracker = new ServiceTracker( bundleContext, filter, this );
        }
        catch( InvalidSyntaxException e )
        {
            String msg = "@Adapter '" + this + "' of component " + componentDescriptor + " defines illegal filter: " + this.filter;
            throw new ComponentDefinitionException( msg, this.componentDescriptor.getComponentType(), this.componentDescriptor.getModule() );
        }

        try
        {
            this.adapterConstructor = this.componentDescriptor.getComponentType().getDeclaredConstructor( new Class[] {
                    adaptedType
            } );
            this.adapterConstructor.setAccessible( true );
        }
        catch( Exception e )
        {
            throw new ComponentCreateException( e, this.componentDescriptor.getComponentType(), this.componentDescriptor.getModule() );
        }
    }

    @Override
    public final String toString()
    {
        return "ComponentServiceAdapter[" + this.componentDescriptor + "]";
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
        BundleContext bundleContext = this.componentDescriptor.getModule().getBundle().getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "no bundle context for module " + componentDescriptor.getModule() );
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
            throw new ComponentCreateException( e, this.componentDescriptor.getComponentType(), this.componentDescriptor.getModule() );
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
            return ComponentServiceAdapterLifecycle.this.adaptedType;
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
            return ComponentServiceAdapterLifecycle.this.serviceTracker.getService( this.reference );
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
                String typeName = ComponentServiceAdapterLifecycle.this.adaptedType.getName();
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
            return ComponentServiceAdapterLifecycle.this.componentDescriptor.getModule();
        }

        @Nonnull
        @Override
        public Class<?> getType()
        {
            ServiceReference reference = this.registration.getReference();
            if( reference != null )
            {
                String[] objectClass = ( String[] ) reference.getProperty( Constants.OBJECTCLASS );
                ModuleImpl module = ComponentServiceAdapterLifecycle.this.componentDescriptor.getModule();
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

package org.mosaic.core.impl.module;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.mosaic.core.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;

import static java.util.Objects.requireNonNull;
import static org.mosaic.core.impl.Activator.getServiceManager;

/**
 * @author arik
 */
class ModuleTypeImpl implements ModuleType
{
    @Nonnull
    private static final Module.ServiceProperty[] EMPTY_SERVICE_PROPERTY_ARRAY = new Module.ServiceProperty[ 0 ];

    @Nonnull
    private final ModuleRevisionImpl moduleRevision;

    @Nonnull
    private final Class<?> type;

    @Nonnull
    private final Map<String, ValueProvider<?>> fieldValueProviders = new HashMap<>();

    @SuppressWarnings( "unchecked" )
    ModuleTypeImpl( @Nonnull ModuleRevisionImpl moduleRevision, @Nonnull Class<?> type )
    {
        this.moduleRevision = moduleRevision;
        this.type = type;

        TypeResolver typeResolver = new TypeResolver();
        for( Field field : this.type.getDeclaredFields() )
        {
            if( field.isAnnotationPresent( Inject.class ) )
            {
                Inject annotation = field.getAnnotation( Inject.class );

                ResolvedType resolvedType = typeResolver.resolve( field.getGenericType() );
                if( resolvedType.getErasedType().equals( Module.class ) )
                {
                    this.fieldValueProviders.put( field.getName(), new ModuleValueProvider() );
                }
                else if( resolvedType.getErasedType().equals( ServiceProvider.class ) )
                {
                    this.fieldValueProviders.put(
                            field.getName(),
                            new ServiceProviderValueProvider(
                                    this.moduleRevision.getServiceDependency(
                                            resolvedType,
                                            0,
                                            createFilter( annotation.properties() )
                                    )
                            )
                    );
                }
                else if( resolvedType.getErasedType().equals( ServicesProvider.class ) )
                {
                    this.fieldValueProviders.put(
                            field.getName(),
                            new ServicesProviderValueProvider(
                                    this.moduleRevision.getServiceDependency(
                                            resolvedType,
                                            0,
                                            createFilter( annotation.properties() )
                                    )
                            )
                    );
                }
                else if( resolvedType.getErasedType().equals( ServiceRegistration.class ) )
                {
                    this.fieldValueProviders.put(
                            field.getName(),
                            new ServiceRegistrationValueProvider(
                                    this.moduleRevision.getServiceDependency(
                                            resolvedType,
                                            field.isAnnotationPresent( Nonnull.class ) ? 1 : 0,
                                            createFilter( annotation.properties() )
                                    )
                            )
                    );
                }
                else if( resolvedType.getErasedType().equals( ServiceTracker.class ) )
                {
                    this.fieldValueProviders.put(
                            field.getName(),
                            new ServiceTrackerValueProvider<>(
                                    resolvedType.getTypeParameters().get( 0 ).getErasedType(),
                                    createFilter( annotation.properties() )
                            )
                    );
                }
                else if( resolvedType.getErasedType().equals( List.class ) )
                {
                    this.fieldValueProviders.put(
                            field.getName(),
                            new ServicesListValueProvider(
                                    this.moduleRevision.getServiceDependency(
                                            resolvedType,
                                            0,
                                            createFilter( annotation.properties() )
                                    )
                            )
                    );
                }
                else
                {
                    this.fieldValueProviders.put(
                            field.getName(),
                            new ServiceProxyValueProvider(
                                    this.moduleRevision.getServiceDependency(
                                            resolvedType,
                                            1,
                                            createFilter( annotation.properties() )
                                    )
                            )
                    );
                }
            }
        }
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "type", this.type )
                             .add( "revision", this.moduleRevision )
                             .toString();
    }

    @Nullable
    @Override
    public Object getInstanceFieldValue( @Nonnull String fieldName )
    {
        this.moduleRevision.getModule().getLock().acquireReadLock();
        try
        {
            ValueProvider<?> valueProvider = this.fieldValueProviders.get( fieldName );
            if( valueProvider == null )
            {
                throw new IllegalArgumentException( "unknown @Inject field name '" + fieldName + "' for '" + this.type.getName() + "'" );
            }
            else
            {
                return valueProvider.getValue();
            }
        }
        finally
        {
            this.moduleRevision.getModule().getLock().releaseReadLock();
        }
    }

    void shutdown()
    {
        for( ValueProvider<?> valueProvider : this.fieldValueProviders.values() )
        {
            valueProvider.shutdown();
        }
    }

    @Nonnull
    ModuleRevisionImpl getModuleRevision()
    {
        return this.moduleRevision;
    }

    @Nonnull
    Class<?> getType()
    {
        return this.type;
    }

    @Nonnull
    private Module.ServiceProperty[] createFilter( @Nonnull Inject.Property... properties )
    {
        if( properties.length == 0 )
        {
            return EMPTY_SERVICE_PROPERTY_ARRAY;
        }

        Module.ServiceProperty[] array = new Module.ServiceProperty[ properties.length ];
        for( int i = 0; i < properties.length; i++ )
        {
            array[ i ] = Module.ServiceProperty.p( properties[ i ].name(), properties[ i ].value() );
        }
        return array;
    }

    private abstract class ValueProvider<Value>
    {
        @Nullable
        abstract Value getValue();

        void shutdown()
        {
            // no-op
        }
    }

    private class ModuleValueProvider extends ValueProvider<Module>
    {
        @Nullable
        @Override
        Module getValue()
        {
            return ModuleTypeImpl.this.moduleRevision.getModule();
        }
    }

    private abstract class DependencyValueProvider<ServiceType, Type>
            extends ValueProvider<Type>
    {
        @Nonnull
        protected final ModuleRevisionImplServiceDependency<ServiceType> dependency;

        private DependencyValueProvider( @Nonnull ModuleRevisionImplServiceDependency<ServiceType> dependency )
        {
            this.dependency = dependency;
        }
    }

    private class ServiceProviderValueProvider<ServiceType>
            extends DependencyValueProvider<ServiceType, ServiceProvider<ServiceType>>
            implements ServiceProvider<ServiceType>
    {
        private ServiceProviderValueProvider( @Nonnull ModuleRevisionImplServiceDependency<ServiceType> dependency )
        {
            super( dependency );
        }

        @Nullable
        @Override
        public ServiceRegistration<ServiceType> getRegistration()
        {
            ServiceTracker<ServiceType> serviceTracker = this.dependency.getServiceTracker();
            List<? extends ServiceRegistration<ServiceType>> registrations = serviceTracker.getRegistrations();
            return registrations.isEmpty() ? null : registrations.get( 0 );
        }

        @Nullable
        @Override
        public ServiceType getService()
        {
            ServiceRegistration<ServiceType> registration = getRegistration();
            return registration == null ? null : registration.getService();
        }

        @Nonnull
        @Override
        ServiceProvider<ServiceType> getValue()
        {
            return this;
        }
    }

    private class ServiceProxyValueProvider<ServiceType>
            extends DependencyValueProvider<ServiceType, ServiceType> implements InvocationHandler
    {
        @Nullable
        private ServiceType proxy;

        private ServiceProxyValueProvider( @Nonnull ModuleRevisionImplServiceDependency<ServiceType> dependency )
        {
            super( dependency );
        }

        @Override
        public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
        {
            List<ServiceType> services = this.dependency.getServiceTracker().getServices();
            if( services.isEmpty() )
            {
                throw new IllegalStateException( "dependency " + this.dependency + " is not available" );
            }
            else
            {
                return method.invoke( services.get( 0 ), args );
            }
        }

        @Nonnull
        @Override
        ServiceType getValue()
        {
            ServiceType proxy = this.proxy;
            if( proxy != null )
            {
                return proxy;
            }
            else
            {
                synchronized( this )
                {
                    proxy = this.proxy;
                    if( proxy == null )
                    {
                        this.proxy = createProxy();
                    }
                    return getValue();
                }
            }
        }

        @SuppressWarnings( "unchecked" )
        @Nonnull
        private ServiceType createProxy()
        {
            ClassLoader classLoader = moduleRevision.getClassLoader();
            ResolvedType serviceType = this.dependency.getServiceKey().getServiceType();
            return ( ServiceType ) Proxy.newProxyInstance( classLoader, new Class<?>[] { serviceType.getErasedType() }, this );
        }
    }

    private class ServiceRegistrationValueProvider<ServiceType>
            extends DependencyValueProvider<ServiceType, ServiceRegistration>
            implements ServiceRegistration<ServiceType>
    {
        private ServiceRegistrationValueProvider( @Nonnull ModuleRevisionImplServiceDependency<ServiceType> dependency )
        {
            super( dependency );
        }

        @Nonnull
        @Override
        public Module getProvider()
        {
            ServiceRegistration<ServiceType> registration = getRegistration();
            if( registration == null )
            {
                throw new IllegalStateException( "dependency " + this.dependency + " not available for " + moduleRevision );
            }
            else
            {
                return registration.getProvider();
            }
        }

        @Nonnull
        @Override
        public Class<ServiceType> getType()
        {
            ServiceRegistration<ServiceType> registration = getRegistration();
            if( registration == null )
            {
                throw new IllegalStateException( "dependency " + this.dependency + " not available for " + moduleRevision );
            }
            else
            {
                return registration.getType();
            }
        }

        @Nonnull
        @Override
        public Map<String, Object> getProperties()
        {
            ServiceRegistration<ServiceType> registration = getRegistration();
            if( registration == null )
            {
                throw new IllegalStateException( "dependency " + this.dependency + " not available for " + moduleRevision );
            }
            else
            {
                return registration.getProperties();
            }
        }

        @Nullable
        @Override
        public ServiceType getService()
        {
            ServiceRegistration<ServiceType> registration = getRegistration();
            if( registration == null )
            {
                throw new IllegalStateException( "dependency " + this.dependency + " not available for " + moduleRevision );
            }
            else
            {
                return registration.getService();
            }
        }

        @Override
        public void unregister()
        {
            throw new UnsupportedOperationException( "dependants cannot unregister their dependencies" );
        }

        @Nullable
        @Override
        ServiceRegistration<ServiceType> getValue()
        {
            return this;
        }

        @Nullable
        private ServiceRegistration<ServiceType> getRegistration()
        {
            List<ServiceRegistration<ServiceType>> registrations = this.dependency.getServiceTracker().getRegistrations();
            return registrations.isEmpty() ? null : registrations.get( 0 );
        }
    }

    private class ServicesProviderValueProvider<ServiceType>
            extends DependencyValueProvider<ServiceType, ServicesProvider>
            implements ServicesProvider<ServiceType>
    {
        private ServicesProviderValueProvider( @Nonnull ModuleRevisionImplServiceDependency<ServiceType> dependency )
        {
            super( dependency );
        }

        @Nonnull
        @Override
        public List<ServiceRegistration<ServiceType>> getRegistrations()
        {
            return this.dependency.getServiceTracker().getRegistrations();
        }

        @Nonnull
        @Override
        public List<ServiceType> getServices()
        {
            return this.dependency.getServiceTracker().getServices();
        }

        @Nullable
        @Override
        ServicesProvider getValue()
        {
            return this;
        }
    }

    private class ServiceTrackerValueProvider<ServiceType>
            extends ValueProvider<ServiceTracker<ServiceType>>
            implements ServiceTracker<ServiceType>, ServiceListener<ServiceType>
    {
        @Nonnull
        private final ServiceTracker<ServiceType> serviceTracker;

        @Nonnull
        private final List<ServiceListener<ServiceType>> eventHandlers;

        private ServiceTrackerValueProvider( @Nonnull Class<ServiceType> serviceType,
                                             @Nonnull Module.ServiceProperty... properties )
        {
            this.eventHandlers = new CopyOnWriteArrayList<>();
            this.serviceTracker = requireNonNull( getServiceManager() ).createServiceTracker( serviceType, properties );
            this.serviceTracker.addEventHandler( this );
        }

        @Override
        public void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
        {
            for( ServiceListener<ServiceType> listener : this.eventHandlers )
            {
                try
                {
                    listener.serviceRegistered( registration );
                }
                catch( Throwable e )
                {
                    moduleRevision.getModule().getLogger().warn( "Service tracker listener '{}' threw an exception", listener, e );
                }
            }
        }

        @Override
        public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                         @Nonnull ServiceType service )
        {
            for( ServiceListener<ServiceType> listener : this.eventHandlers )
            {
                try
                {
                    listener.serviceUnregistered( registration, service );
                }
                catch( Throwable e )
                {
                    moduleRevision.getModule().getLogger().warn( "Service tracker listener '{}' threw an exception", listener, e );
                }
            }
        }

        @Nonnull
        @Override
        public List<ServiceRegistration<ServiceType>> getRegistrations()
        {
            return this.serviceTracker.getRegistrations();
        }

        @Nonnull
        @Override
        public List<ServiceType> getServices()
        {
            return this.serviceTracker.getServices();
        }

        @Override
        public void addEventHandler( @Nonnull ServiceListener<ServiceType> listener )
        {
            this.eventHandlers.add( listener );
        }

        @Override
        public void removeEventHandler( @Nonnull ServiceListener<ServiceType> listener )
        {
            this.eventHandlers.remove( listener );
        }

        @Override
        public void startTracking()
        {
            throw new UnsupportedOperationException( "injected service trackers cannot be started programmatically" );
        }

        @Override
        public void stopTracking()
        {
            throw new UnsupportedOperationException( "injected service trackers cannot be stopped programmatically" );
        }

        @Nullable
        @Override
        ServiceTracker<ServiceType> getValue()
        {
            return this;
        }
    }

    private class ServicesListValueProvider<ServiceType>
            extends DependencyValueProvider<ServiceType, List<ServiceType>>
    {
        private ServicesListValueProvider( @Nonnull ModuleRevisionImplServiceDependency<ServiceType> dependency )
        {
            super( dependency );
        }

        @Nullable
        @Override
        List<ServiceType> getValue()
        {
            return this.dependency.getServiceTracker().getServices();
        }
    }
}

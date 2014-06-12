package org.mosaic.core.modules.impl;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mosaic.core.components.Inject;
import org.mosaic.core.modules.Module;
import org.mosaic.core.modules.ModuleType;
import org.mosaic.core.services.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
class ModuleTypeImpl implements ModuleType
{
    @Nonnull
    private static final Module.ServiceProperty[] EMPTY_SERVICE_PROPERTY_ARRAY = new Module.ServiceProperty[ 0 ];

    @Nonnull
    private static Module.ServiceProperty[] createFilter( @Nonnull Inject.Property... properties )
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

    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final ModuleRevisionImpl moduleRevision;

    @Nonnull
    private final Class<?> type;

    @Nonnull
    private final Map<String, ValueProvider<?>> fieldValueProviders = new HashMap<>();

    @SuppressWarnings("unchecked")
    ModuleTypeImpl( @Nonnull ModuleRevisionImpl moduleRevision, @Nonnull Class<?> type )
    {
        this.lock = moduleRevision.getModule().getLock();
        this.moduleRevision = moduleRevision;
        this.type = type;

        TypeResolver typeResolver = new TypeResolver();
        asList( this.type.getDeclaredFields() )
                .stream()
                .filter( field -> field.isAnnotationPresent( Inject.class ) )
                .forEach( field -> {

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
                } );
    }

    @Override
    public String toString()
    {
        return this.lock.read( () -> ToStringHelper.create( this )
                                                   .add( "type", this.type )
                                                   .add( "revision", this.moduleRevision )
                                                   .toString() );
    }

    @Nullable
    @Override
    public Object getInstanceFieldValue( @Nonnull String fieldName )
    {
        return this.lock.read( () -> {
            ValueProvider<?> valueProvider = this.fieldValueProviders.get( fieldName );
            if( valueProvider == null )
            {
                throw new IllegalArgumentException( "unknown @Inject field name '" + fieldName + "' for '" + this.type.getName() + "'" );
            }
            else
            {
                return valueProvider.getValue();
            }
        } );
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
        return this.lock.read( () -> this.moduleRevision );
    }

    @Nonnull
    Class<?> getType()
    {
        return this.lock.read( () -> this.type );
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

        @SuppressWarnings("unchecked")
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

    private class ServiceTrackerValueProvider<ServiceType> extends ValueProvider<ServiceTracker<ServiceType>>
            implements ServiceTracker<ServiceType>
    {
        @Nonnull
        private final ServiceTracker<ServiceType> serviceTracker;

        private ServiceTrackerValueProvider( @Nonnull Class<ServiceType> serviceType,
                                             @Nonnull Module.ServiceProperty... properties )
        {
            this.serviceTracker = moduleRevision.getModule().createServiceTracker( serviceType, properties );
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
            this.serviceTracker.addEventHandler( listener );
        }

        @Override
        public void addEventHandler( @Nonnull ServiceRegistrationListener<ServiceType> onRegister,
                                     @Nonnull ServiceUnregistrationListener<ServiceType> onUnregister )
        {
            this.serviceTracker.addEventHandler( onRegister, onUnregister );
        }

        @Override
        public void removeEventHandler( @Nonnull ServiceListener<ServiceType> listener )
        {
            this.serviceTracker.removeEventHandler( listener );
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

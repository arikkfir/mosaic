package org.mosaic.core.modules.impl;

import com.fasterxml.classmate.TypeResolver;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.mosaic.core.components.*;
import org.mosaic.core.modules.Module;
import org.mosaic.core.services.ServiceListener;
import org.mosaic.core.services.ServiceListenerRegistration;
import org.mosaic.core.services.ServiceRegistration;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.logging.Logging;
import org.slf4j.Logger;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toCollection;
import static org.mosaic.core.modules.Module.ServiceProperty.p;
import static org.mosaic.core.util.base.AnnotationUtils.findMetaAnnotationTarget;

/**
 * @author arik
 */
class ModuleComponentImpl
{
    @Nonnull
    private static final Module.ServiceProperty[] EMPTY_PROPERTIES_ARRAY = new Module.ServiceProperty[ 0 ];

    @Nonnull
    private static Module.ServiceProperty[] createFilter( @Nonnull ServiceAdapter.Property... properties )
    {
        if( properties.length == 0 )
        {
            return EMPTY_PROPERTIES_ARRAY;
        }

        Module.ServiceProperty[] array = new Module.ServiceProperty[ properties.length ];
        for( int i = 0; i < properties.length; i++ )
        {
            array[ i ] = Module.ServiceProperty.p( properties[ i ].name(), properties[ i ].value() );
        }
        return array;
    }

    @Nonnull
    private static Module.ServiceProperty[] createFilter( @Nonnull OnServiceRegistration.Property... properties )
    {
        if( properties.length == 0 )
        {
            return EMPTY_PROPERTIES_ARRAY;
        }

        Module.ServiceProperty[] array = new Module.ServiceProperty[ properties.length ];
        for( int i = 0; i < properties.length; i++ )
        {
            array[ i ] = Module.ServiceProperty.p( properties[ i ].name(), properties[ i ].value() );
        }
        return array;
    }

    @Nonnull
    private static Module.ServiceProperty[] createFilter( @Nonnull OnServiceUnregistration.Property... properties )
    {
        if( properties.length == 0 )
        {
            return EMPTY_PROPERTIES_ARRAY;
        }

        Module.ServiceProperty[] array = new Module.ServiceProperty[ properties.length ];
        for( int i = 0; i < properties.length; i++ )
        {
            array[ i ] = Module.ServiceProperty.p( properties[ i ].name(), properties[ i ].value() );
        }
        return array;
    }

    @Nonnull
    private final ModuleTypeImpl moduleType;

    @Nonnull
    private final List<ProvidedType> providedTypes;

    @Nonnull
    private final Callable<Object> instantiator;

    @Nonnull
    private final List<Method> deactivationMethods;

    @Nonnull
    private final List<ServiceAdapterHandler> serviceAdapterMethods;

    @Nonnull
    private final List<MethodEndpointImpl> methodEndpoints;

    @Nonnull
    private final List<ServiceRegistrationHandler<?>> serviceRegistrationHandlers;

    @Nonnull
    private final List<ServiceUnregistrationHandler<?>> serviceUnregistrationHandlers;

    @Nullable
    private Object instance;

    ModuleComponentImpl( @Nonnull ModuleTypeImpl moduleType, List<Component> annotations )
    {
        this.moduleType = moduleType;
        this.instantiator = createInstantiator( this.moduleType.getType() );
        this.providedTypes = unmodifiableList( annotations.stream()
                                                          .filter( annotation -> !void.class.equals( annotation.value() ) )
                                                          .map( ProvidedType::new )
                                                          .collect( toCollection( LinkedList::new ) ) );

        Logger logger = this.moduleType.getModuleRevision().getModule().getLogger();

        List<Method> deactivationMethods = new LinkedList<>();
        List<MethodEndpointImpl> methodEndpoints = new LinkedList<>();
        List<ServiceRegistrationHandler<?>> serviceRegistrationHandlers = new LinkedList<>();
        List<ServiceUnregistrationHandler<?>> serviceUnregistrationHandlers = new LinkedList<>();
        List<ServiceAdapterHandler> serviceAdapterMethods = new LinkedList<>();
        Class<?> type = this.moduleType.getType();
        TypeResolver typeResolver = new TypeResolver();
        while( type != null && !type.getPackage().getName().startsWith( "java." ) && !type.getPackage().getName().startsWith( "javax." ) )
        {
            for( Method method : this.moduleType.getType().getDeclaredMethods() )
            {
                if( method.isAnnotationPresent( OnDeactivation.class ) )
                {
                    if( method.getParameterTypes().length > 0 )
                    {
                        logger.warn( "@OnDeactivation methods must not have parameters (found in method '{}' of type '{}')", method.toGenericString(), this.moduleType );
                    }
                    else
                    {
                        method.setAccessible( true );
                        deactivationMethods.add( method );
                    }
                }

                ServiceAdapter serviceAdapterAnn = method.getAnnotation( ServiceAdapter.class );
                if( serviceAdapterAnn != null )
                {
                    Type[] parameterTypes = method.getGenericParameterTypes();
                    if( parameterTypes.length != 1 )
                    {
                        logger.warn( "@ServiceAdapter methods must have exactly one parameter of type ServiceRegistration<...> (found in method '{}' of type '{}')",
                                     method.toGenericString(), this.moduleType );
                    }
                    else if( method.getParameterTypes()[ 0 ].equals( ServiceRegistration.class ) )
                    {
                        method.setAccessible( true );
                        ServiceKey serviceKey = new ServiceKey( typeResolver.resolve( parameterTypes[ 0 ] ), 0, createFilter( serviceAdapterAnn.properties() ) );
                        serviceAdapterMethods.add( new ServiceAdapterHandler<>( method, serviceKey ) );
                    }
                    else
                    {
                        logger.warn( "@ServiceAdapter methods must have exactly one parameter of type ServiceRegistration<...> (found in method '{}' of type '{}')",
                                     method.toGenericString(), this.moduleType );
                    }
                }

                OnServiceRegistration serviceRegistrationAnn = method.getAnnotation( OnServiceRegistration.class );
                if( serviceRegistrationAnn != null )
                {
                    Type[] parameterTypes = method.getGenericParameterTypes();
                    if( parameterTypes.length != 1 )
                    {
                        logger.warn( "@OnServiceRegistration methods must have exactly one parameter of type ServiceRegistration<...> (found in method '{}' of type '{}')",
                                     method.toGenericString(), this.moduleType );
                    }
                    else if( method.getParameterTypes()[ 0 ].equals( ServiceRegistration.class ) )
                    {
                        method.setAccessible( true );
                        ServiceKey serviceKey = new ServiceKey( typeResolver.resolve( parameterTypes[ 0 ] ), 0, createFilter( serviceRegistrationAnn.properties() ) );
                        serviceRegistrationHandlers.add( new ServiceRegistrationHandler<>( method, serviceKey ) );
                    }
                    else
                    {
                        logger.warn( "@OnServiceRegistration methods must have exactly one parameter of type ServiceRegistration<...> (found in method '{}' of type '{}')",
                                     method.toGenericString(), this.moduleType );
                    }
                }

                OnServiceUnregistration serviceUnregistrationAnn = method.getAnnotation( OnServiceUnregistration.class );
                if( serviceUnregistrationAnn != null )
                {
                    Type[] parameterTypes = method.getGenericParameterTypes();
                    if( parameterTypes.length != 2 )
                    {
                        logger.warn( "@OnServiceUnregistration methods must have exactly 2 parameters of type ServiceRegistration<...> and the actual service (found in method '{}' of type '{}')",
                                     method.toGenericString(), this.moduleType );
                    }
                    else if( method.getParameterTypes()[ 0 ].equals( ServiceRegistration.class ) )
                    {
                        method.setAccessible( true );
                        ServiceKey serviceKey = new ServiceKey( typeResolver.resolve( parameterTypes[ 0 ] ), 0, createFilter( serviceUnregistrationAnn.properties() ) );
                        serviceUnregistrationHandlers.add( new ServiceUnregistrationHandler<>( method, serviceKey ) );
                    }
                    else
                    {
                        logger.warn( "@OnServiceUnregistration methods must have exactly 2 parameters of type ServiceRegistration<...> and the actual service (found in method '{}' of type '{}')",
                                     method.toGenericString(), this.moduleType );
                    }
                }

                Annotation annotation = findMetaAnnotationTarget( method, EndpointMarker.class );
                if( annotation != null )
                {
                    method.setAccessible( true );
                    methodEndpoints.add( new MethodEndpointImpl<>( method, annotation ) );
                }
            }
            type = type.getSuperclass();
        }
        this.deactivationMethods = unmodifiableList( deactivationMethods );
        this.methodEndpoints = unmodifiableList( methodEndpoints );
        this.serviceAdapterMethods = serviceAdapterMethods;
        this.serviceRegistrationHandlers = serviceRegistrationHandlers;
        this.serviceUnregistrationHandlers = serviceUnregistrationHandlers;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "type", this.moduleType )
                             .add( "provides", this.providedTypes )
                             .toString();
    }

    void activate() throws Throwable
    {
        this.moduleType.getModuleRevision().getModule().getLogger().debug( "Activating component {}", this );
        try
        {
            this.instance = this.instantiator.call();
        }
        catch( InvocationTargetException e )
        {
            throw e.getCause();
        }

        this.serviceRegistrationHandlers.forEach( ServiceRegistrationHandler::register );
        this.serviceUnregistrationHandlers.forEach( ServiceUnregistrationHandler::register );
        this.providedTypes.forEach( type -> type.register( this.instance ) );
        this.methodEndpoints.forEach( MethodEndpointImpl::register );
        this.serviceAdapterMethods.forEach( ServiceAdapterHandler::register );
    }

    void deactivate()
    {
        Object instance = this.instance;
        if( instance != null )
        {
            Logger logger = this.moduleType.getModuleRevision().getModule().getLogger();

            logger.debug( "Deactivating component {}", this );

            this.serviceAdapterMethods.forEach( ServiceAdapterHandler::unregister );
            this.methodEndpoints.forEach( MethodEndpointImpl::unregister );
            this.providedTypes.forEach( ProvidedType::unregister );
            this.serviceUnregistrationHandlers.forEach( ServiceUnregistrationHandler::unregister );
            this.serviceRegistrationHandlers.forEach( ServiceRegistrationHandler::unregister );
            this.deactivationMethods.forEach( method -> {
                try
                {
                    method.invoke( instance );
                }
                catch( Throwable e )
                {
                    logger.warn( "@OnDeactivation method '{}' of component {} threw an exception",
                                 method.toGenericString(), this, e );
                }
            } );
            this.instance = null;
        }
    }

    @Nonnull
    private Callable<Object> createInstantiator( @Nonnull Class<?> type )
    {
        try
        {
            Method getInstanceMethod = type.getDeclaredMethod( "getInstance" );
            if( Modifier.isStatic( getInstanceMethod.getModifiers() ) )
            {
                getInstanceMethod.setAccessible( true );
                return new FactoryMethodInstantiator( getInstanceMethod );
            }
        }
        catch( NoSuchMethodException ignore )
        {
        }
        catch( Throwable e )
        {
            this.moduleType.getModuleRevision().getModule().getLogger().warn( "Error discovering factory method for {}", this, e );
        }

        try
        {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible( true );
            return new ConstructorInstantiator( constructor );
        }
        catch( NoSuchMethodException ignore )
        {
        }
        catch( Throwable e )
        {
            this.moduleType.getModuleRevision().getModule().getLogger().warn( "Error discovering constructor for {}", this, e );
        }

        throw new IllegalStateException( "@Component class " + type.getName() + " has no default constructor nor a 'getInstance()' static method" );
    }

    private class ProvidedType
    {
        @Nonnull
        private final Class type;

        @Nonnull
        private final Module.ServiceProperty[] properties;

        @Nullable
        private ServiceRegistration registration;

        private ProvidedType( @Nonnull Component annotation )
        {
            this.type = annotation.value();
            List<Module.ServiceProperty> properties = new LinkedList<>();
            for( Component.Property property : annotation.properties() )
            {
                properties.add( p( property.name(), property.value() ) );
            }
            this.properties = properties.toArray( new Module.ServiceProperty[ properties.size() ] );
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "type", this.type.getName() )
                                 .toString();
        }

        @SuppressWarnings( "unchecked" )
        private void register( @Nonnull Object instance )
        {
            ModuleImpl module = ModuleComponentImpl.this.moduleType.getModuleRevision().getModule();
            this.registration = module.registerService( this.type, instance, this.properties );
        }

        private void unregister()
        {
            ServiceRegistration registration = this.registration;
            if( registration != null )
            {
                registration.unregister();
                this.registration = null;
            }
        }
    }

    private class ConstructorInstantiator implements Callable<Object>
    {
        @Nonnull
        private final Constructor<?> constructor;

        private ConstructorInstantiator( @Nonnull Constructor<?> constructor )
        {
            this.constructor = constructor;
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "constructor", this.constructor.toGenericString() )
                                 .toString();
        }

        @Override
        public Object call() throws Exception
        {
            this.constructor.setAccessible( true );
            return this.constructor.newInstance();
        }
    }

    private class FactoryMethodInstantiator implements Callable<Object>
    {
        @Nonnull
        private final Method method;

        private FactoryMethodInstantiator( @Nonnull Method method )
        {
            this.method = method;
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "method", this.method.toGenericString() )
                                 .toString();
        }

        @Override
        public Object call() throws Exception
        {
            this.method.setAccessible( true );
            return this.method.invoke( null );
        }
    }

    private class MethodEndpointImpl<AnnType extends Annotation> implements MethodEndpoint<AnnType>
    {
        @Nonnull
        private final Method method;

        @Nonnull
        private final AnnType annotation;

        @Nullable
        private ServiceRegistration<MethodEndpoint> registration;

        private MethodEndpointImpl( @Nonnull Method method, @Nonnull AnnType annotation )
        {
            this.method = method;
            this.method.setAccessible( true );
            this.annotation = annotation;
        }

        @Override
        public String toString()
        {
            return ToStringHelper.create( this )
                                 .add( "method", this.method.getName() )
                                 .add( "type", this.annotation.annotationType().getSimpleName() )
                                 .toString();
        }

        @Nonnull
        @Override
        public String getName()
        {
            return this.method.getName();
        }

        @Nonnull
        @Override
        public AnnType getEndpointType()
        {
            return this.annotation;
        }

        @Nonnull
        @Override
        public Method getMethod()
        {
            return this.method;
        }

        @Nullable
        @Override
        public Object invoke( @Nullable Object... args ) throws Throwable
        {
            Object instance = ModuleComponentImpl.this.instance;
            if( instance == null )
            {
                throw new IllegalStateException( "component not created" );
            }

            try
            {
                return this.method.invoke( instance, args );
            }
            catch( IllegalAccessException | IllegalArgumentException e )
            {
                throw new IllegalStateException( e );
            }
            catch( InvocationTargetException e )
            {
                throw e.getCause();
            }
        }

        void register()
        {
            ModuleImpl module = ModuleComponentImpl.this.moduleType.getModuleRevision().getModule();
            this.registration = module.registerService( MethodEndpoint.class, this,
                                                        p( "name", getName() ),
                                                        p( "type", this.annotation.annotationType().getName() ) );
        }

        void unregister()
        {
            ServiceRegistration<MethodEndpoint> registration = this.registration;
            if( registration != null )
            {
                registration.unregister();
                this.registration = null;
            }
        }
    }

    private class ServiceAdapterHandler<OriginalType, AdaptedType> implements ServiceListener<OriginalType>
    {
        @Nonnull
        private final Method method;

        @Nonnull
        private final Class<OriginalType> serviceType;

        @Nonnull
        private final Module.ServiceProperty[] filter;

        @Nonnull
        private final Map<ServiceRegistration<OriginalType>, ServiceRegistration<AdaptedType>> registrations = new HashMap<>();

        @Nullable
        private ServiceListenerRegistration<OriginalType> listenerRegistration;

        @SuppressWarnings( "unchecked" )
        private ServiceAdapterHandler( @Nonnull Method method, @Nonnull ServiceKey serviceKey )
        {
            this.method = method;
            this.serviceType = ( Class<OriginalType> ) serviceKey.getServiceType().getErasedType();
            this.filter = serviceKey.getServicePropertiesArray();
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public void serviceRegistered( @Nonnull ServiceRegistration<OriginalType> registration )
        {
            ModuleImpl module = ModuleComponentImpl.this.moduleType.getModuleRevision().getModule();
            module.getLock().write( () -> {

                Object instance = ModuleComponentImpl.this.instance;
                if( instance != null )
                {
                    try
                    {
                        Object adapted = this.method.invoke( instance, registration );
                        Class serviceType = this.method.getReturnType();
                        this.registrations.put( registration, module.registerService( serviceType, adapted ) );
                    }
                    catch( Throwable e )
                    {
                        Logging.getLogger().error( "Could not adapt {} into {}", registration, this.method.getGenericReturnType(), e );
                    }
                }
            } );
        }

        @Override
        public void serviceUnregistered( @Nonnull ServiceRegistration<OriginalType> registration,
                                         @Nonnull OriginalType service )
        {
            ModuleImpl module = ModuleComponentImpl.this.moduleType.getModuleRevision().getModule();
            module.getLock().write( () -> {

                try
                {
                    ServiceRegistration<AdaptedType> adaptedRegistration = this.registrations.remove( registration );
                    if( adaptedRegistration != null )
                    {
                        adaptedRegistration.unregister();
                    }
                }
                catch( Throwable e )
                {
                    Logging.getLogger().error( "Could not adapt {} into {}", registration, this.method.getGenericReturnType(), e );
                }

            } );
        }

        void register()
        {
            this.listenerRegistration = moduleType.getModuleRevision().getModule().addServiceListener( this, this.serviceType, this.filter );
        }

        void unregister()
        {
            ServiceListenerRegistration<OriginalType> registration = this.listenerRegistration;
            if( registration != null )
            {
                registration.unregister();
                this.listenerRegistration = null;
            }

            for( ServiceRegistration<AdaptedType> adaptedRegistration : this.registrations.values() )
            {
                adaptedRegistration.unregister();
            }
        }
    }

    private class ServiceRegistrationHandler<ServiceType> implements ServiceListener<ServiceType>
    {
        @Nonnull
        private final Method method;

        @Nonnull
        private final Class<ServiceType> serviceType;

        @Nonnull
        private final Module.ServiceProperty[] filter;

        @Nullable
        private ServiceListenerRegistration<ServiceType> listenerRegistration;

        @SuppressWarnings( "unchecked" )
        private ServiceRegistrationHandler( @Nonnull Method method, @Nonnull ServiceKey serviceKey )
        {
            this.method = method;
            this.serviceType = ( Class<ServiceType> ) serviceKey.getServiceType().getErasedType();
            this.filter = serviceKey.getServicePropertiesArray();
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
        {
            ModuleImpl module = ModuleComponentImpl.this.moduleType.getModuleRevision().getModule();
            module.getLock().write( () -> {

                Object instance = ModuleComponentImpl.this.instance;
                if( instance != null )
                {
                    try
                    {
                        this.method.invoke( instance, registration );
                    }
                    catch( Throwable e )
                    {
                        Logging.getLogger().error( "Service registration listener method {} threw an exception", this.method.getGenericReturnType(), e );
                    }
                }
            } );
        }

        @Override
        public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                         @Nonnull ServiceType service )
        {
            // no-op
        }

        void register()
        {
            this.listenerRegistration = moduleType.getModuleRevision().getModule().addServiceListener( this, this.serviceType, this.filter );
        }

        void unregister()
        {
            ServiceListenerRegistration<ServiceType> registration = this.listenerRegistration;
            if( registration != null )
            {
                registration.unregister();
                this.listenerRegistration = null;
            }
        }
    }

    private class ServiceUnregistrationHandler<ServiceType> implements ServiceListener<ServiceType>
    {
        @Nonnull
        private final Method method;

        @Nonnull
        private final Class<ServiceType> serviceType;

        @Nonnull
        private final Module.ServiceProperty[] filter;

        @Nullable
        private ServiceListenerRegistration<ServiceType> listenerRegistration;

        @SuppressWarnings( "unchecked" )
        private ServiceUnregistrationHandler( @Nonnull Method method, @Nonnull ServiceKey serviceKey )
        {
            this.method = method;
            this.serviceType = ( Class<ServiceType> ) serviceKey.getServiceType().getErasedType();
            this.filter = serviceKey.getServicePropertiesArray();
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public void serviceRegistered( @Nonnull ServiceRegistration<ServiceType> registration )
        {
            // no-op
        }

        @Override
        public void serviceUnregistered( @Nonnull ServiceRegistration<ServiceType> registration,
                                         @Nonnull ServiceType service )
        {
            ModuleImpl module = ModuleComponentImpl.this.moduleType.getModuleRevision().getModule();
            module.getLock().write( () -> {

                Object instance = ModuleComponentImpl.this.instance;
                if( instance != null )
                {
                    try
                    {
                        this.method.invoke( instance, registration, service );
                    }
                    catch( Throwable e )
                    {
                        Logging.getLogger().error( "Service unregistration listener method {} threw an exception", this.method.getGenericReturnType(), e );
                    }
                }
            } );
        }

        void register()
        {
            this.listenerRegistration = moduleType.getModuleRevision().getModule().addServiceListener( this, this.serviceType, this.filter );
        }

        void unregister()
        {
            ServiceListenerRegistration<ServiceType> registration = this.listenerRegistration;
            if( registration != null )
            {
                registration.unregister();
                this.listenerRegistration = null;
            }
        }
    }
}

package org.mosaic.core.modules.impl;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.mosaic.core.components.*;
import org.mosaic.core.impl.Activator;
import org.mosaic.core.modules.Module;
import org.mosaic.core.services.ServiceListener;
import org.mosaic.core.services.ServiceRegistration;
import org.mosaic.core.services.impl.ServiceManagerEx;
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
    private final ModuleTypeImpl moduleType;

    @Nonnull
    private final List<ProvidedType> providedTypes;

    @Nullable
    private final Callable<Object> instantiator;

    @Nonnull
    private final List<Method> deactivationMethods;

    @SuppressWarnings({ "FieldCanBeLocal", "UnusedDeclaration" })
    @Nonnull
    private final List<ServiceAdapterHandler> serviceAdapterMethods;

    @Nonnull
    private final List<MethodEndpointImpl<?>> methodEndpoints;

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
        List<MethodEndpointImpl<?>> methodEndpoints = new LinkedList<>();
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
                        ModuleRevisionImpl moduleRevision = this.moduleType.getModuleRevision();
                        ResolvedType resolvedType = typeResolver.resolve( parameterTypes[ 0 ] );
                        ModuleRevisionImplServiceDependency<Object> dependency = moduleRevision.getServiceDependency( resolvedType, 0, createFilter( serviceAdapterAnn.properties() ) );
                        serviceAdapterMethods.add( new ServiceAdapterHandler<>( dependency, method ) );
                    }
                    else
                    {
                        logger.warn( "@ServiceAdapter methods must have exactly one parameter of type ServiceRegistration<...> (found in method '{}' of type '{}')",
                                     method.toGenericString(), this.moduleType );
                    }
                }

                Annotation annotation = findMetaAnnotationTarget( method, EndpointMarker.class );
                if( annotation != null )
                {
                    methodEndpoints.add( new MethodEndpointImpl<>( method, annotation ) );
                }
            }
            type = type.getSuperclass();
        }
        this.deactivationMethods = unmodifiableList( deactivationMethods );
        this.methodEndpoints = unmodifiableList( methodEndpoints );
        this.serviceAdapterMethods = serviceAdapterMethods;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "type", this.moduleType )
                             .add( "provides", this.providedTypes )
                             .toString();
    }

    void activate()
    {
        ModuleImpl module = this.moduleType.getModuleRevision().getModule();
        Logger logger = module.getLogger();

        if( this.instantiator != null )
        {
            module.getLogger().debug( "Activating component {}", this );
            try
            {
                this.instance = this.instantiator.call();
            }
            catch( Exception e )
            {
                logger.error( "Error activating component {} - instantiation error", this, e );
                return;
            }

            for( ProvidedType providedType : this.providedTypes )
            {
                providedType.register( this.instance );
            }

            for( MethodEndpointImpl<?> methodEndpoint : this.methodEndpoints )
            {
                methodEndpoint.register();
            }
        }
    }

    void deactivate()
    {
        Object instance = this.instance;
        if( instance != null )
        {
            Logger logger = this.moduleType.getModuleRevision().getModule().getLogger();

            logger.debug( "Deactivating component {}", this );

            //noinspection Convert2MethodRef
            this.methodEndpoints.forEach( ( t ) -> t.unregister() );
            this.providedTypes.forEach( ProvidedType::unregister );

            for( Method method : this.deactivationMethods )
            {
                try
                {
                    method.invoke( instance );
                }
                catch( Throwable e )
                {
                    logger.warn( "@OnDeactivation method '{}' of component {} threw an exception",
                                 method.toGenericString(), this, e );
                }
            }
            this.instance = null;
        }
    }

    @Nullable
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

        return null;
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

        @SuppressWarnings("unchecked")
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
            ModuleImpl module = ModuleComponentImpl.this.moduleType.getModuleRevision().getModule();
            Logger logger = module.getLogger();
            try
            {
                this.constructor.setAccessible( true );
                return this.constructor.newInstance();
            }
            catch( Throwable e )
            {
                logger.error( "Error activating component {} - instantiation error", this, e );
                return null;
            }
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
            ModuleImpl module = ModuleComponentImpl.this.moduleType.getModuleRevision().getModule();
            Logger logger = module.getLogger();
            try
            {
                this.method.setAccessible( true );
                return this.method.invoke( null );
            }
            catch( Throwable e )
            {
                logger.error( "Error activating component {} - instantiation error", this, e );
                return null;
            }
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

        @Nonnull
        @Override
        public String getName()
        {
            return this.method.getName();
        }

        @Nonnull
        @Override
        public AnnType getType()
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
        private final ModuleRevisionImplServiceDependency<OriginalType> dependency;

        @Nonnull
        private final Method method;

        @Nonnull
        private final Map<ServiceRegistration<OriginalType>, ServiceRegistration<AdaptedType>> registrations = new HashMap<>();

        private ServiceAdapterHandler( @Nonnull ModuleRevisionImplServiceDependency<OriginalType> dependency,
                                       @Nonnull Method method )
        {
            this.dependency = dependency;
            this.method = method;
            this.dependency.getServiceTracker().addEventHandler( this );
        }

        @SuppressWarnings("unchecked")
        @Override
        public void serviceRegistered( @Nonnull ServiceRegistration<OriginalType> registration )
        {
            ModuleImpl module = ModuleComponentImpl.this.moduleType.getModuleRevision().getModule();
            module.getLock().acquireWriteLock();
            try
            {
                ServiceManagerEx serviceManager = Activator.getServiceManager();
                Object instance = ModuleComponentImpl.this.instance;
                if( serviceManager != null && instance != null )
                {
                    try
                    {
                        Object adapted = this.method.invoke( instance, registration );
                        Class serviceType = this.method.getReturnType();
                        this.registrations.put( registration, serviceManager.registerService( module, serviceType, adapted ) );
                    }
                    catch( Throwable e )
                    {
                        Logging.getLogger().error( "Could not adapt {} into {}", registration, this.method.getGenericReturnType(), e );
                    }
                }
            }
            finally
            {
                module.getLock().releaseWriteLock();
            }
        }

        @Override
        public void serviceUnregistered( @Nonnull ServiceRegistration<OriginalType> registration,
                                         @Nonnull OriginalType service )
        {
            ModuleImpl module = ModuleComponentImpl.this.moduleType.getModuleRevision().getModule();
            module.getLock().acquireWriteLock();
            try
            {
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
            }
            finally
            {
                module.getLock().releaseWriteLock();
            }
        }
    }
}

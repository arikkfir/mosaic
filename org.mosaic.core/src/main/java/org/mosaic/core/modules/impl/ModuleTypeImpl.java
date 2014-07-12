package org.mosaic.core.modules.impl;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.UnaryOperator;
import org.mosaic.core.components.ClassEndpoint;
import org.mosaic.core.components.EndpointMarker;
import org.mosaic.core.components.Inject;
import org.mosaic.core.launcher.impl.ServerImpl;
import org.mosaic.core.modules.Module;
import org.mosaic.core.modules.ModuleRevision;
import org.mosaic.core.modules.ModuleType;
import org.mosaic.core.modules.ServiceProperty;
import org.mosaic.core.services.ServiceProvider;
import org.mosaic.core.services.ServiceRegistration;
import org.mosaic.core.services.ServicesProvider;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;

import static java.util.Arrays.asList;
import static org.mosaic.core.modules.ServiceProperty.p;
import static org.mosaic.core.util.base.AnnotationUtils.findMetaAnnotationTarget;

/**
 * @author arik
 */
class ModuleTypeImpl implements ModuleType
{
    @Nonnull
    private static final ServiceProperty[] EMPTY_SERVICE_PROPERTY_ARRAY = new ServiceProperty[ 0 ];

    @Nonnull
    static ServiceProperty[] createFilter( @Nonnull Inject.Property... properties )
    {
        if( properties.length == 0 )
        {
            return EMPTY_SERVICE_PROPERTY_ARRAY;
        }

        ServiceProperty[] array = new ServiceProperty[ properties.length ];
        for( int i = 0; i < properties.length; i++ )
        {
            array[ i ] = ServiceProperty.p( properties[ i ].name(), properties[ i ].value() );
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

    @Nullable
    private final ClassEndpointImpl classEndpoint;

    @SuppressWarnings( "unchecked" )
    ModuleTypeImpl( @Nonnull ServerImpl server, @Nonnull ModuleRevisionImpl moduleRevision, @Nonnull Class<?> type )
    {
        this.lock = server.getLock();
        this.moduleRevision = moduleRevision;
        this.type = type;

        Annotation classEndpointType = findMetaAnnotationTarget( this.type, EndpointMarker.class );
        if( classEndpointType != null )
        {
            this.classEndpoint = new ClassEndpointImpl( classEndpointType );
            this.classEndpoint.register();
        }
        else
        {
            this.classEndpoint = null;
        }

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
                    else if( resolvedType.getErasedType().equals( List.class ) )
                    {
                        ResolvedType itemType = resolvedType.getTypeParameters().get( 0 );
                        if( itemType.getErasedType().equals( ServiceRegistration.class ) )
                        {
                            // FEATURE arik: add support for List<ServiceRegistration<ServiceType>>
                            throw new UnsupportedOperationException( "Lists of service registrations are not yet supported" );
                        }
                        else
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
        return ToStringHelper.create( this )
                             .add( "type", getType() )
                             .add( "revision", getModuleRevision() )
                             .toString();
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

    void activate()
    {
        // no-op
    }

    void deactivate()
    {
        // no-op
    }

    void shutdown()
    {
        for( ValueProvider<?> valueProvider : this.fieldValueProviders.values() )
        {
            valueProvider.shutdown();
        }

        ClassEndpointImpl endpoint = this.classEndpoint;
        if( endpoint != null )
        {
            endpoint.unregister();
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
        // idea 14 has a problem with using "return this.lock.read( ()->this.type );" so we're using the lock/release pattern here
        this.lock.acquireReadLock();
        try
        {
            return this.type;
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    private class ClassEndpointImpl<Type extends Annotation> implements ClassEndpoint<Type>
    {
        @Nonnull
        private final Type endpointType;

        @Nullable
        private ServiceRegistration<ClassEndpoint> registration;

        private ClassEndpointImpl( @Nonnull Type endpointType )
        {
            this.endpointType = endpointType;
        }

        @Nonnull
        @Override
        public Type getEndpointType()
        {
            return this.endpointType;
        }

        @Nonnull
        @Override
        public Class<?> getType()
        {
            return ModuleTypeImpl.this.type;
        }

        void register()
        {
            this.registration = moduleRevision.registerService( ClassEndpoint.class, this, p( "type", this.endpointType.annotationType().getName() ) );
        }

        void unregister()
        {
            ServiceRegistration<ClassEndpoint> registration = this.registration;
            if( registration != null )
            {
                registration.unregister();
                this.registration = null;
            }
        }
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

    private abstract class DependencyValueProvider<ServiceType, Type> extends ValueProvider<Type>
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
    {
        private ServiceProviderValueProvider( @Nonnull ModuleRevisionImplServiceDependency<ServiceType> dependency )
        {
            super( dependency );
        }

        @Nonnull
        @Override
        ServiceProvider<ServiceType> getValue()
        {
            return this.dependency;
        }
    }

    private class ServicesProviderValueProvider<ServiceType>
            extends DependencyValueProvider<ServiceType, ServicesProvider<ServiceType>>
    {
        private ServicesProviderValueProvider( @Nonnull ModuleRevisionImplServiceDependency<ServiceType> dependency )
        {
            super( dependency );
        }

        @Nullable
        @Override
        ServicesProvider<ServiceType> getValue()
        {
            return this.dependency;
        }
    }

    private class ServiceRegistrationValueProvider<ServiceType>
            extends DependencyValueProvider<ServiceType, ServiceRegistration<ServiceType>>
            implements ServiceRegistration<ServiceType>
    {
        private ServiceRegistrationValueProvider( @Nonnull ModuleRevisionImplServiceDependency<ServiceType> dependency )
        {
            super( dependency );
        }

        @Nullable
        @Override
        public ModuleRevision getProvider()
        {
            return getRegistration().getProvider();
        }

        @Nonnull
        @Override
        public Class<ServiceType> getType()
        {
            return getRegistration().getType();
        }

        @Nonnull
        @Override
        public Map<String, Object> getProperties()
        {
            return getRegistration().getProperties();
        }

        @Nullable
        @Override
        public ServiceType getService()
        {
            return getRegistration().getService();
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

        @Nonnull
        private ServiceRegistration<ServiceType> getRegistration()
        {
            ServiceRegistration<ServiceType> registration = this.dependency.getRegistration();
            if( registration == null )
            {
                throw new IllegalStateException( "dependency " + this.dependency + " not available for " + moduleRevision );
            }
            else
            {
                return registration;
            }
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
            ServiceType service = this.dependency.getService();
            if( service == null )
            {
                throw new IllegalStateException( "dependency " + this.dependency + " is not available" );
            }
            else
            {
                return method.invoke( service, args );
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
                        proxy = createProxy();
                        this.proxy = proxy;
                    }
                    return proxy;
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Nonnull
        private ServiceType createProxy()
        {
            ResolvedType serviceType = this.dependency.getServiceKey().getServiceType();
            Class<?> serviceErasedType = serviceType.getErasedType();
            return ( ServiceType ) Proxy.newProxyInstance(
                    serviceErasedType.getClassLoader(), new Class<?>[] { serviceErasedType }, this );
        }
    }

    private class ServicesListValueProvider<ServiceType>
            extends DependencyValueProvider<ServiceType, List<ServiceType>>
            implements List<ServiceType>
    {
        private ServicesListValueProvider( @Nonnull ModuleRevisionImplServiceDependency<ServiceType> dependency )
        {
            super( dependency );
        }

        @Override
        public int size()
        {
            return this.dependency.doWithServices( List::size );
        }

        @Override
        public boolean isEmpty()
        {
            return this.dependency.doWithServices( List::isEmpty );
        }

        @Override
        public boolean contains( Object o )
        {
            return this.dependency.doWithServices( list -> list.contains( o ) );
        }

        @Nonnull
        @Override
        public Iterator<ServiceType> iterator()
        {
            return new Iterator<ServiceType>()
            {
                @Nonnull
                private final Iterator<ServiceType> iterator = dependency.getServices().iterator();

                @Override
                public boolean hasNext()
                {
                    return this.iterator.hasNext();
                }

                @Override
                public ServiceType next()
                {
                    return this.iterator.next();
                }
            };
        }

        @Nonnull
        @Override
        public Object[] toArray()
        {
            return this.dependency.doWithServices( List::toArray );
        }

        @Nonnull
        @Override
        public <T> T[] toArray( @Nonnull T[] a )
        {
            //noinspection SuspiciousToArrayCall
            return this.dependency.doWithServices( list -> list.toArray( a ) );
        }

        @Override
        public boolean add( ServiceType serviceType )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove( Object o )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll( @Nonnull Collection<?> c )
        {
            return this.dependency.doWithServices( list -> list.containsAll( c ) );
        }

        @Override
        public boolean addAll( @Nonnull Collection<? extends ServiceType> c )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll( int index, @Nonnull Collection<? extends ServiceType> c )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll( @Nonnull Collection<?> c )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll( @Nonnull Collection<?> c )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceAll( UnaryOperator<ServiceType> operator )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sort( Comparator<? super ServiceType> c )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public ServiceType get( int index )
        {
            return this.dependency.doWithServices( list -> list.get( index ) );
        }

        @Override
        public ServiceType set( int index, ServiceType element )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add( int index, ServiceType element )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public ServiceType remove( int index )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf( Object o )
        {
            return this.dependency.doWithServices( list -> list.indexOf( o ) );
        }

        @Override
        public int lastIndexOf( Object o )
        {
            return this.dependency.doWithServices( list -> list.lastIndexOf( o ) );
        }

        @Nonnull
        @Override
        public ListIterator<ServiceType> listIterator()
        {
            return new ListIterator<ServiceType>()
            {
                @Nonnull
                private final ListIterator<ServiceType> iterator = dependency.getServices().listIterator();

                @Override
                public boolean hasNext()
                {
                    return this.iterator.hasNext();
                }

                @Override
                public ServiceType next()
                {
                    return this.iterator.next();
                }

                @Override
                public boolean hasPrevious()
                {
                    return this.iterator.hasPrevious();
                }

                @Override
                public ServiceType previous()
                {
                    return this.iterator.previous();
                }

                @Override
                public int nextIndex()
                {
                    return this.iterator.nextIndex();
                }

                @Override
                public int previousIndex()
                {
                    return this.iterator.previousIndex();
                }

                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set( ServiceType serviceType )
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void add( ServiceType serviceType )
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Nonnull
        @Override
        public ListIterator<ServiceType> listIterator( int index )
        {
            return new ListIterator<ServiceType>()
            {
                @Nonnull
                private final ListIterator<ServiceType> iterator = dependency.getServices().listIterator( index );

                @Override
                public boolean hasNext()
                {
                    return this.iterator.hasNext();
                }

                @Override
                public ServiceType next()
                {
                    return this.iterator.next();
                }

                @Override
                public boolean hasPrevious()
                {
                    return this.iterator.hasPrevious();
                }

                @Override
                public ServiceType previous()
                {
                    return this.iterator.previous();
                }

                @Override
                public int nextIndex()
                {
                    return this.iterator.nextIndex();
                }

                @Override
                public int previousIndex()
                {
                    return this.iterator.previousIndex();
                }

                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set( ServiceType serviceType )
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void add( ServiceType serviceType )
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Nonnull
        @Override
        public List<ServiceType> subList( int fromIndex, int toIndex )
        {
            return this.dependency.getServices().subList( fromIndex, toIndex );
        }

        @Nullable
        @Override
        List<ServiceType> getValue()
        {
            return this;
        }
    }
}

package org.mosaic.util.reflection.impl;

import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.impl.util.ServiceUtils;
import org.mosaic.util.collect.HashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.pair.MutablePair;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodHandleFactory;
import org.mosaic.util.reflection.MethodParameter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import static java.util.Arrays.asList;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * @author arik
 */
public class MethodHandleFactoryImpl implements MethodHandleFactory, InitializingBean, DisposableBean
{
    @Nonnull
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * @todo replace with guava cache
     */
    @Nonnull
    private final Map<Method, MethodHandleImpl> handlesCacheByMethod = new WeakHashMap<>( 1000 );

    @Nonnull
    private final BundleContext bundleContext;

    @Nullable
    private ServiceRegistration<MethodHandleFactory> serviceRegistration;

    @Nonnull
    private ConversionService conversionService;

    public MethodHandleFactoryImpl( @Nonnull BundleContext bundleContext, @Nonnull ConversionService conversionService )
    {
        this.bundleContext = bundleContext;
        this.conversionService = conversionService;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        this.serviceRegistration = ServiceUtils.register( this.bundleContext, MethodHandleFactory.class, this );
    }

    @Override
    public void destroy() throws Exception
    {
        this.serviceRegistration = ServiceUtils.unregister( this.serviceRegistration );
    }

    @Override
    @Nullable
    public MethodHandle findMethodHandle( @Nonnull Class<?> clazz,
                                          @Nonnull String methodName,
                                          @Nonnull Class<?>... argumentTypes )
    {
        // TODO arik: cache by signature to save reflection search of method
        Method method = ReflectionUtils.findMethod( clazz, methodName, argumentTypes );
        if( method == null )
        {
            return null;
        }
        else
        {
            return findMethodHandle( method );
        }
    }

    @Nonnull
    @Override
    public MethodHandle findMethodHandle( @Nonnull Method method )
    {
        // see if we have a cached handle for the method - we do that while holding a read lock to prevent updates while we do it
        this.lock.readLock().lock();
        MethodHandleImpl handle = this.handlesCacheByMethod.get( method );
        this.lock.readLock().unlock();

        // if we have a cached handle - return that
        if( handle != null )
        {
            return handle;
        }

        // no cache - create a new handle while holding the write-lock, thus ensuring we have exclusive access to the map
        this.lock.writeLock().lock();
        try
        {
            handle = this.handlesCacheByMethod.get( method );
            if( handle == null )
            {
                handle = new MethodHandleImpl( method );
                this.handlesCacheByMethod.put( method, handle );
            }
        }
        finally
        {
            this.lock.writeLock().unlock();
        }
        return handle;
    }

    private class MethodParameterImpl implements MethodParameter
    {
        private final MethodHandleImpl handle;

        private final org.springframework.core.MethodParameter methodParameter;

        private final TypeToken<?> type;

        private final String desc;

        private MethodParameterImpl( MethodHandleImpl handle, int index )
        {
            this.handle = handle;

            LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
            org.springframework.core.MethodParameter methodParameter = new org.springframework.core.MethodParameter( this.handle.getNativeMethod(), index );
            methodParameter.initParameterNameDiscovery( discoverer );
            this.methodParameter = methodParameter;

            this.type = TypeToken.of( this.methodParameter.getGenericParameterType() );

            this.desc = "MethodParam[ '" + getName() + "' (" + this.type + ") of " + this.handle.toString() + " ]";
        }

        @Override
        public String toString()
        {
            return desc;
        }

        @Nonnull
        @Override
        public MethodHandle getMethod()
        {
            return this.handle;
        }

        @Nonnull
        @Override
        public String getName()
        {
            return this.methodParameter.getParameterName();
        }

        @Override
        public int getIndex()
        {
            return this.methodParameter.getParameterIndex();
        }

        @Nonnull
        @Override
        public TypeToken<?> getType()
        {
            return this.type;
        }

        @Nonnull
        @Override
        public Collection<Annotation> getAnnotations()
        {
            return asList( this.methodParameter.getParameterAnnotations() );
        }

        @Override
        public <T extends Annotation> T getAnnotation( @Nonnull Class<T> annType )
        {
            return this.methodParameter.getParameterAnnotation( annType );
        }

        @Override
        public <T extends Annotation> boolean hasAnnotation( @Nonnull Class<T> annType )
        {
            return this.methodParameter.getParameterAnnotation( annType ) != null;
        }

        @Override
        public boolean isPrimitive()
        {
            return this.type.getRawType().isPrimitive();
        }

        @Override
        public boolean isArray()
        {
            return this.type.isArray();
        }

        @Override
        public boolean isList()
        {
            return this.type.isAssignableFrom( List.class );
        }

        @Override
        public boolean isSet()
        {
            return this.type.isAssignableFrom( Set.class );
        }

        @Override
        public boolean isCollection()
        {
            return this.type.isAssignableFrom( Collection.class );
        }

        @Override
        public TypeToken<?> getCollectionItemType()
        {
            if( isList() )
            {
                return this.type.resolveType( List.class.getTypeParameters()[ 0 ] );
            }
            else if( isSet() )
            {
                return this.type.resolveType( Set.class.getTypeParameters()[ 0 ] );
            }
            else if( isCollection() )
            {
                return this.type.resolveType( Collection.class.getTypeParameters()[ 0 ] );
            }
            else if( isArray() )
            {
                return this.type.getComponentType();
            }
            else
            {
                return null;
            }
        }

        @Override
        public boolean isMapEx()
        {
            return this.type.getRawType().equals( MapEx.class );
        }

        @Override
        public boolean isMap()
        {
            return this.type.isAssignableFrom( Map.class );
        }

        @Override
        public MutablePair<TypeToken<?>, TypeToken<?>> getMapType()
        {
            TypeToken<?> keyToken = this.type.resolveType( Map.class.getTypeParameters()[ 0 ] );
            TypeToken<?> valueToken = this.type.resolveType( Map.class.getTypeParameters()[ 1 ] );
            return MutablePair.<TypeToken<?>, TypeToken<?>>of( keyToken, valueToken );
        }

        @Override
        public boolean isProperties()
        {
            return this.type.isAssignableFrom( Properties.class );
        }
    }

    private class MethodHandleImpl implements MethodHandle
    {
        private final Method nativeMethod;

        private final String desc;

        private final List<MethodParameter> parameters;

        private final TypeToken<?> returnType;

        public MethodHandleImpl( Method method )
        {
            this.desc = "MethodHandle[" + method.getDeclaringClass().getSimpleName() + "#" + method.getName() + "]";

            this.nativeMethod = method;

            List<MethodParameter> parameters = new LinkedList<>();
            for( int i = 0, count = this.nativeMethod.getParameterTypes().length; i < count; i++ )
            {
                parameters.add( new MethodParameterImpl( this, i ) );
            }
            this.parameters = Collections.unmodifiableList( parameters );

            this.returnType = TypeToken.of( this.nativeMethod.getGenericReturnType() );
        }

        @Override
        public String toString()
        {
            return this.desc;
        }

        @Nonnull
        @Override
        public String getName()
        {
            return this.nativeMethod.getName();
        }

        @Nonnull
        @Override
        public Class<?> getDeclaringClass()
        {
            return this.nativeMethod.getDeclaringClass();
        }

        @Nonnull
        @Override
        public Method getNativeMethod()
        {
            return this.nativeMethod;
        }

        @Nonnull
        @Override
        public TypeToken<?> getReturnType()
        {
            return this.returnType;
        }

        @Nonnull
        @Override
        public List<MethodParameter> getParameters()
        {
            return this.parameters;
        }

        @Nonnull
        @Override
        public Collection<Annotation> getAnnotations()
        {
            return asList( AnnotationUtils.getAnnotations( this.nativeMethod ) );
        }

        @Override
        public <T extends Annotation> T getAnnotation( @Nonnull Class<T> annotationType )
        {
            return findAnnotation( this.nativeMethod, annotationType );
        }

        @Nonnull
        @Override
        public <T extends Annotation> T requireAnnotation( @Nonnull Class<T> annotationType )
        {
            T annotation = getAnnotation( annotationType );
            if( annotation == null )
            {
                throw new IllegalStateException( "Annotation '" + annotationType.getName() + "' is required but not found on method '" + this + "'" );
            }
            else
            {
                return annotation;
            }
        }

        @Override
        public Object invoke( @Nullable Object bean, Object... args ) throws Exception
        {
            try
            {
                return this.nativeMethod.invoke( bean, args );
            }
            catch( InvocationTargetException e )
            {
                Throwable cause = e.getCause();
                if( cause instanceof Exception )
                {
                    throw ( Exception ) cause;
                }
                else
                {
                    throw e;
                }
            }
        }

        @Override
        public boolean hasAnnotation( @Nonnull Class<? extends Annotation> type )
        {
            return getAnnotation( type ) != null;
        }

        @Nonnull
        @Override
        public Invoker createInvoker( @Nonnull ParameterResolver... resolvers )
        {
            return createInvoker( Arrays.asList( resolvers ) );
        }

        @Nonnull
        @Override
        public Invoker createInvoker( @Nonnull Collection<ParameterResolver> resolvers )
        {
            return new InvokerImpl( resolvers );
        }

        private class InvokerImpl implements Invoker
        {
            @Nonnull
            private final Collection<ParameterResolver> resolvers;

            private InvokerImpl( @Nonnull Collection<ParameterResolver> resolvers )
            {
                this.resolvers = resolvers;
            }

            @Nonnull
            @Override
            public MethodHandle getMethod()
            {
                return MethodHandleImpl.this;
            }

            @Nonnull
            @Override
            public Invocation resolve( @Nonnull Map<String, Object> resolveContext )
            {
                MapEx<String, Object> context;
                if( resolveContext instanceof MapEx )
                {
                    context = ( MapEx<String, Object> ) resolveContext;
                }
                else
                {
                    context = new HashMapEx<>( resolveContext, conversionService );
                }
                Object[] arguments = new Object[ parameters.size() ];
                for( int i = 0; i < parameters.size(); i++ )
                {
                    MethodParameter parameter = parameters.get( i );
                    arguments[ i ] = ParameterResolver.SKIP;

                    // iterate our resolvers to find a value for this parameter
                    for( ParameterResolver resolver : this.resolvers )
                    {
                        try
                        {
                            Object argument = resolver.resolve( parameter, context );
                            if( argument != ParameterResolver.SKIP )
                            {
                                // resolved a value for this parameter
                                arguments[ i ] = argument;
                                break;
                            }
                        }
                        catch( Exception e )
                        {
                            // resolver failed
                            throw new UnresolvableArgumentException( e, parameter );
                        }
                    }

                    if( arguments[ i ] == ParameterResolver.SKIP )
                    {
                        // no resolver provided a value for this parameter
                        throw new UnresolvableArgumentException( parameter );
                    }
                }
                return new InvocationImpl( arguments );
            }

            private class InvocationImpl implements Invocation
            {
                @Nonnull
                private final Object[] arguments;

                private InvocationImpl( @Nonnull Object[] arguments )
                {
                    this.arguments = arguments;
                }

                @Nonnull
                @Override
                public Invoker getInvoker()
                {
                    return InvokerImpl.this;
                }

                @Nonnull
                @Override
                public Object[] getArguments()
                {
                    return this.arguments;
                }

                @Nullable
                @Override
                public Object invoke( @Nonnull Object bean ) throws Exception
                {
                    return MethodHandleImpl.this.invoke( bean, this.arguments );
                }
            }
        }
    }
}

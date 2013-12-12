package org.mosaic.util.reflection.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.pair.Pair;
import org.mosaic.util.reflection.*;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
final class MethodHandleFactoryImpl implements MethodHandleFactory
{
    @Nonnull
    private final LoadingCache<Method, MethodHandleImpl> methodHandlesCache;

    @Nonnull
    private final LoadingCache<MethodSignatureKey, Method> methodSignaturesCache;

    @Nonnull
    private final ParameterNameDiscoverer defaultParameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

    @Nonnull
    private final LoadingCache<ClassLoader, ParameterNameDiscoverer> parameterNameDiscoverersCache;

    public MethodHandleFactoryImpl()
    {
        this.methodHandlesCache = CacheBuilder.newBuilder()
                                              .concurrencyLevel( 100 )
                                              .expireAfterAccess( 1, TimeUnit.HOURS )
                                              .initialCapacity( 1000 )
                                              .maximumSize( 10000 )
                                              .weakKeys()
                                              .build( new CacheLoader<Method, MethodHandleImpl>()
                                              {
                                                  @Nonnull
                                                  @Override
                                                  public MethodHandleImpl load( @Nonnull Method key ) throws Exception
                                                  {
                                                      return new MethodHandleImpl( key );
                                                  }
                                              } );
        this.methodSignaturesCache = CacheBuilder.newBuilder()
                                                 .concurrencyLevel( 100 )
                                                 .expireAfterAccess( 1, TimeUnit.HOURS )
                                                 .initialCapacity( 1000 )
                                                 .maximumSize( 10000 )
                                                 .weakKeys()
                                                 .build( new CacheLoader<MethodSignatureKey, Method>()
                                                 {
                                                     @Nonnull
                                                     @Override
                                                     public Method load( @Nonnull MethodSignatureKey key )
                                                             throws Exception
                                                     {
                                                         return key.findMethod();
                                                     }
                                                 } );
        this.parameterNameDiscoverersCache = CacheBuilder.newBuilder()
                                                         .concurrencyLevel( 100 )
                                                         .expireAfterAccess( 1, TimeUnit.HOURS )
                                                         .initialCapacity( 1000 )
                                                         .maximumSize( 10000 )
                                                         .weakKeys()
                                                         .build( new CacheLoader<ClassLoader, ParameterNameDiscoverer>()
                                                         {
                                                             @Nonnull
                                                             @Override
                                                             public ParameterNameDiscoverer load( @Nonnull ClassLoader key )
                                                                     throws Exception
                                                             {
                                                                 return new LocalVariableTableParameterNameDiscoverer();
                                                             }
                                                         } );
    }

    @Override
    @Nullable
    public InvokableMethodHandle findMethodHandle( @Nonnull Class<?> clazz,
                                                   @Nonnull String methodName,
                                                   @Nonnull Class<?>... argumentTypes )
    {
        MethodSignatureKey key = new MethodSignatureKey( clazz, methodName, argumentTypes );
        return findMethodHandle( this.methodSignaturesCache.getUnchecked( key ) );
    }

    @Nonnull
    @Override
    public InvokableMethodHandle findMethodHandle( @Nonnull Method method )
    {
        return this.methodHandlesCache.getUnchecked( method );
    }

    private class MethodParameterImpl implements MethodParameter
    {
        @Nonnull
        private final MethodHandleImpl handle;

        @Nonnull
        private final org.springframework.core.MethodParameter methodParameter;

        @Nonnull
        private final TypeToken<?> type;

        @Nonnull
        private final String desc;

        private MethodParameterImpl( @Nonnull MethodHandleImpl handle, int index )
        {
            this.handle = handle;

            Class<?> declaringClass = this.handle.getDeclaringClass();
            ClassLoader classLoader = declaringClass.getClassLoader();
            ParameterNameDiscoverer discoverer =
                    classLoader == null ? defaultParameterNameDiscoverer : parameterNameDiscoverersCache.getUnchecked( classLoader );
            org.springframework.core.MethodParameter methodParameter = new org.springframework.core.MethodParameter( this.handle.getNativeMethod(), index );
            methodParameter.initParameterNameDiscovery( discoverer );
            this.methodParameter = methodParameter;

            this.type = TypeToken.of( this.methodParameter.getGenericParameterType() );

            this.desc = "MethodParam[ '" + getName() + "' (" + this.type + ") of " + this.handle.toString() + " ]";
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
            String parameterName = this.methodParameter.getParameterName();
            if( parameterName == null )
            {
                return "p" + this.methodParameter.getParameterIndex();
            }
            else
            {
                return parameterName;
            }
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

        @Nullable
        @Override
        public <T extends Annotation> T getAnnotation( @Nonnull Class<T> annType )
        {
            return this.methodParameter.getParameterAnnotation( annType );
        }

        @Nonnull
        @Override
        public <T extends Annotation> T requireAnnotation( @Nonnull Class<T> annType )
        {
            T annotation = getAnnotation( annType );
            if( annotation == null )
            {
                throw new IllegalStateException( "Annotation '" + annType.getName() + "' not found on " + this );
            }
            else
            {
                return annotation;
            }
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

        @Nullable
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

        @Nullable
        @Override
        public Pair<TypeToken<?>, TypeToken<?>> getMapType()
        {
            if( isMap() )
            {
                TypeToken<?> keyToken = this.type.resolveType( Map.class.getTypeParameters()[ 0 ] );
                TypeToken<?> valueToken = this.type.resolveType( Map.class.getTypeParameters()[ 1 ] );
                return Pair.<TypeToken<?>, TypeToken<?>>of( keyToken, valueToken );
            }
            else
            {
                return null;
            }
        }

        @Override
        public boolean isProperties()
        {
            return this.type.isAssignableFrom( Properties.class );
        }

        @Nonnull
        @Override
        public String toString()
        {
            return this.desc;
        }
    }

    private class MethodHandleImpl implements InvokableMethodHandle
    {
        @Nonnull
        private final Method nativeMethod;

        @Nonnull
        private final AnnotationFinder annotationFinder;

        @Nonnull
        private final String desc;

        @Nonnull
        private final List<MethodParameter> parameters;

        @Nonnull
        private final TypeToken<?> returnType;

        public MethodHandleImpl( @Nonnull Method method )
        {
            this.nativeMethod = method;
            this.annotationFinder = new AnnotationFinder( this.nativeMethod );
            this.desc = "MethodHandle[" + method.getDeclaringClass().getSimpleName() + "#" + method.getName() + "]";

            List<MethodParameter> parameters = new LinkedList<>();
            for( int i = 0, count = this.nativeMethod.getParameterTypes().length; i < count; i++ )
            {
                parameters.add( new MethodParameterImpl( this, i ) );
            }
            this.parameters = Collections.unmodifiableList( parameters );

            this.returnType = TypeToken.of( this.nativeMethod.getGenericReturnType() );
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
        public boolean hasAnnotation( @Nonnull Class<? extends Annotation> type )
        {
            return hasAnnotation( type, false );
        }

        @Override
        public boolean hasAnnotation( @Nonnull Class<? extends Annotation> type, boolean deep )
        {
            return getAnnotation( type, deep ) != null;
        }

        @Override
        public <T extends Annotation> T getAnnotation( @Nonnull Class<T> annotationType )
        {
            return getAnnotation( annotationType, false );
        }

        @Nullable
        @Override
        public <T extends Annotation> T getAnnotation( @Nonnull Class<T> annotationType, boolean deep )
        {
            if( deep )
            {
                return this.annotationFinder.findDeep( annotationType );
            }
            else
            {
                return this.annotationFinder.find( annotationType );
            }
        }

        @Nullable
        @Override
        public Annotation findMetaAnnotationTarget( @Nonnull Class<? extends Annotation> metaAnnotationType )
        {
            return this.annotationFinder.findAnnotationAnnotatedDeeplyBy( metaAnnotationType );
        }

        @Nonnull
        @Override
        public <T extends Annotation> T requireAnnotation( @Nonnull Class<T> annotationType )
        {
            return requireAnnotation( annotationType, false );
        }

        @Nonnull
        @Override
        public <T extends Annotation> T requireAnnotation( @Nonnull Class<T> annotationType, boolean deep )
        {
            T annotation = getAnnotation( annotationType, deep );
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

        @Override
        public String toString()
        {
            return this.desc;
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
                    context = new HashMapEx<>( resolveContext );
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

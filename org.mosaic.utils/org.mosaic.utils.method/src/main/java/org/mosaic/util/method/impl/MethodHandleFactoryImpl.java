package org.mosaic.util.method.impl;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.method.*;
import org.mosaic.util.reflection.ClassAnnotations;
import org.mosaic.util.reflection.MethodAnnotations;
import org.mosaic.util.reflection.MethodParameterNames;
import org.mosaic.util.reflection.TypeTokens;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;

/**
 * @author arik
 */
final class MethodHandleFactoryImpl implements MethodHandleFactory
{
    @Nonnull
    private final LoadingCache<Method, MethodHandleImpl> methodHandlesCache;

    @Nonnull
    private final LoadingCache<MethodSignatureKey, Method> methodSignaturesCache;

    public MethodHandleFactoryImpl()
    {
        this.methodHandlesCache = CacheBuilder.newBuilder()
                                              .concurrencyLevel( 100 )
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
    }

    @Override
    @Nullable
    public InvokableMethodHandle findMethodHandle( @Nonnull Class<?> clazz,
                                                   @Nonnull String methodName,
                                                   @Nonnull Class<?>... argumentTypes )
    {
        MethodSignatureKey key = new MethodSignatureKey( clazz, methodName, argumentTypes );
        try
        {
            return findMethodHandle( this.methodSignaturesCache.getUnchecked( key ) );
        }
        catch( UncheckedExecutionException e )
        {
            Throwable cause = e.getCause();
            if( cause instanceof RuntimeException )
            {
                throw ( RuntimeException ) cause;
            }
            else
            {
                throw e;
            }
        }
    }

    @Nonnull
    @Override
    public InvokableMethodHandle findMethodHandle( @Nonnull Method method )
    {
        try
        {
            return this.methodHandlesCache.getUnchecked( method );
        }
        catch( UncheckedExecutionException e )
        {
            Throwable cause = e.getCause();
            if( cause instanceof RuntimeException )
            {
                throw ( RuntimeException ) cause;
            }
            else
            {
                throw e;
            }
        }
    }

    void clearCaches()
    {
        this.methodHandlesCache.invalidateAll();
        this.methodSignaturesCache.invalidateAll();
    }

    private class MethodParameterImpl implements MethodParameter
    {
        @Nonnull
        private final MethodHandleImpl handle;

        private final int index;

        @Nonnull
        private final String name;

        @Nonnull
        private final TypeToken<?> type;

        @Nonnull
        private final Collection<Annotation> annotations;

        @Nonnull
        private final String desc;

        private MethodParameterImpl( @Nonnull MethodHandleImpl handle, int index )
        {
            this.handle = handle;
            this.index = index;
            this.name = MethodParameterNames.getParameterNames( this.handle.getNativeMethod() ).get( this.index );
            this.type = TypeTokens.of( this.handle.getNativeMethod().getGenericParameterTypes()[ this.index ] );
            this.annotations = unmodifiableCollection( asList( this.handle.getNativeMethod().getParameterAnnotations()[ this.index ] ) );
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
            return this.name;
        }

        @Override
        public int getIndex()
        {
            return this.index;
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
            return this.annotations;
        }

        @Nullable
        @Override
        public <AnnType extends Annotation> AnnType getAnnotation( @Nonnull Class<AnnType> annotationType )
        {
            for( Annotation annotation : this.annotations )
            {
                if( annotation.annotationType().equals( annotationType ) )
                {
                    return annotationType.cast( annotation );
                }
            }
            return null;
        }

        @Nullable
        @Override
        public <MetaAnnType extends Annotation> MetaAnnType getMetaAnnotation( @Nonnull Class<MetaAnnType> annotationType )
        {
            return findMetaAnnotation( annotationType, new HashSet<Class<? extends Annotation>>() );
        }

        @Nullable
        @Override
        public <MetaAnnType extends Annotation> Annotation getMetaAnnotationTarget( @Nonnull Class<MetaAnnType> annotationType )
        {
            return findMetaAnnotationTarget( annotationType, new HashSet<Class<? extends Annotation>>() );
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

        @Nullable
        private <MetaAnnType extends Annotation> MetaAnnType findMetaAnnotation(
                @Nonnull Class<MetaAnnType> metaAnnType,
                @Nonnull Set<Class<? extends Annotation>> annotations )
        {
            // first check if type is annotated with the meta annotation directly
            MetaAnnType metaAnn = getAnnotation( metaAnnType );
            if( metaAnn != null )
            {
                return metaAnn;
            }

            // not annotated directly - check recursively type's annotations if one of them has meta-ann somewhere
            for( Annotation annotation : this.annotations )
            {
                if( !annotations.contains( annotation.annotationType() ) )
                {
                    //
                    // remember this annotation type so we won't scan it again
                    // this is required because annotations can be cross-annotated, eg:
                    //      @A
                    //      public @interface B {}
                    //
                    //      @B
                    //      public @interface A {}
                    //
                    annotations.add( annotation.annotationType() );

                    // search this annotation
                    MetaAnnType result = ClassAnnotations.getMetaAnnotation( annotation.annotationType(), metaAnnType );
                    if( result != null )
                    {
                        return result;
                    }
                }
            }

            // not found
            return null;
        }

        @Nullable
        private Annotation findMetaAnnotationTarget(
                @Nonnull Class<? extends Annotation> metaAnnType,
                @Nonnull Set<Class<? extends Annotation>> annotations )
        {
            // check recursively method's annotations if one of them has meta-ann somewhere
            for( Annotation annotation : this.annotations )
            {
                Class<? extends Annotation> annotationType = annotation.annotationType();

                Annotation metaAnnotation = annotationType.getAnnotation( metaAnnType );
                if( metaAnnotation != null )
                {
                    // this annotation is annotated with the meta-annotation, we've found the grail! return the
                    // annotated annotation (NOT the actual meta annotation instance)
                    return annotation;
                }
                else if( !annotations.contains( annotationType ) )
                {
                    //
                    // remember this annotation type so we won't scan it again
                    // this is required because annotations can be cross-annotated, eg:
                    //      @A
                    //      public @interface B {}
                    //
                    //      @B
                    //      public @interface A {}
                    //
                    annotations.add( annotationType );

                    // search this annotation
                    Annotation result = ClassAnnotations.getMetaAnnotationTarget( annotationType, metaAnnType );
                    if( result != null )
                    {
                        return result;
                    }
                }
            }

            // not found
            return null;
        }
    }

    private class MethodHandleImpl implements InvokableMethodHandle
    {
        @Nonnull
        private final Method nativeMethod;

        @Nonnull
        private final String desc;

        @Nonnull
        private final List<MethodParameter> parameters;

        @Nonnull
        private final TypeToken<?> returnType;

        public MethodHandleImpl( @Nonnull Method method )
        {
            this.nativeMethod = method;
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
            return MethodAnnotations.getAnnotations( this.nativeMethod );
        }

        @Nullable
        @Override
        public <AnnType extends Annotation> AnnType getAnnotation( @Nonnull Class<AnnType> annotationType )
        {
            for( Annotation annotation : getAnnotations() )
            {
                if( annotation.annotationType().equals( annotationType ) )
                {
                    return annotationType.cast( annotation );
                }
            }
            return null;
        }

        @Nullable
        @Override
        public <MetaAnnType extends Annotation> MetaAnnType getMetaAnnotation( @Nonnull Class<MetaAnnType> annotationType )
        {
            return MethodAnnotations.getMetaAnnotation( this.nativeMethod, annotationType );
        }

        @Nullable
        @Override
        public <MetaAnnType extends Annotation> Annotation getMetaAnnotationTarget( @Nonnull Class<MetaAnnType> annotationType )
        {
            return MethodAnnotations.getMetaAnnotationTarget( this.nativeMethod, annotationType );
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
        public Invoker createInvoker( @Nonnull ParameterResolver<?>... resolvers )
        {
            return createInvoker( asList( resolvers ) );
        }

        @Nonnull
        @Override
        public Invoker createInvoker( @Nonnull Collection<ParameterResolver<?>> resolvers )
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
            private final Collection<ParameterResolver<?>> resolvers;

            private InvokerImpl( @Nonnull Collection<ParameterResolver<?>> resolvers )
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

                    // iterate our resolvers to find a value for this parameter
                    Optional<?> holder = Optional.absent();
                    for( ParameterResolver<?> resolver : this.resolvers )
                    {
                        try
                        {
                            holder = resolver.resolve( parameter, context );
                            if( holder == null || holder.isPresent() )
                            {
                                break;
                            }
                        }
                        catch( Exception e )
                        {
                            // resolver failed
                            throw new UnresolvableArgumentException( e, parameter );
                        }
                    }

                    // did we find a value for this parameter?
                    if( holder == null )
                    {
                        arguments[ i ] = null;
                    }
                    else if( holder.isPresent() )
                    {
                        arguments[ i ] = holder.get();
                    }
                    else
                    {
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

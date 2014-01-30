package org.mosaic.util.reflection;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public final class MethodAnnotations
{
    @Nonnull
    private static final LoadingCache<Method, Collection<Annotation>> annotationsListCache =
            CacheBuilder.newBuilder()
                        .concurrencyLevel( 10 )
                        .build( new CacheLoader<Method, Collection<Annotation>>()
                        {
                            @Nonnull
                            @Override
                            public Collection<Annotation> load( @Nonnull Method key ) throws Exception
                            {
                                return findAnnotations( key );
                            }
                        } );

    @Nonnull
    private static final LoadingCache<Pair<Method, Class<? extends Annotation>>, Optional<? extends Annotation>> metaAnnotationCache =
            CacheBuilder.newBuilder()
                        .concurrencyLevel( 10 )
                        .build( new CacheLoader<Pair<Method, Class<? extends Annotation>>, Optional<? extends Annotation>>()
                        {
                            @Nonnull
                            @Override
                            public Optional<? extends Annotation> load( @Nonnull Pair<Method, Class<? extends Annotation>> key )
                                    throws Exception
                            {
                                return findMetaAnnotation( key.getLeft(),
                                                           key.getRight(),
                                                           Sets.<Class<? extends Annotation>>newHashSet() );
                            }
                        } );

    @Nonnull
    private static final LoadingCache<Pair<Method, Class<? extends Annotation>>, Optional<Annotation>> metaAnnotationTargetCache =
            CacheBuilder.newBuilder()
                        .concurrencyLevel( 10 )
                        .build( new CacheLoader<Pair<Method, Class<? extends Annotation>>, Optional<Annotation>>()
                        {
                            @Nonnull
                            @Override
                            public Optional<Annotation> load( @Nonnull Pair<Method, Class<? extends Annotation>> key )
                                    throws Exception
                            {
                                return findMetaAnnotationTarget( key.getLeft(),
                                                                 key.getRight(),
                                                                 Sets.<Class<? extends Annotation>>newHashSet() );
                            }
                        } );

    @Nonnull
    public static Collection<Annotation> getAnnotations( @Nonnull Method method )
    {
        return annotationsListCache.getUnchecked( method );
    }

    @SuppressWarnings( "unchecked" )
    @Nonnull
    public static <MetaAnnType extends Annotation> Optional<MetaAnnType> getMetaAnnotation(
            @Nonnull Method method, @Nonnull Class<MetaAnnType> metaAnnType )
    {
        Pair<Method, Class<? extends Annotation>> key = Pair.<Method, Class<? extends Annotation>>of( method, metaAnnType );
        return ( Optional<MetaAnnType> ) metaAnnotationCache.getUnchecked( key );
    }

    @Nonnull
    public static Optional<Annotation> getMetaAnnotationTarget(
            @Nonnull Method method, @Nonnull Class<? extends Annotation> metaAnnType )
    {
        Pair<Method, Class<? extends Annotation>> key = Pair.<Method, Class<? extends Annotation>>of( method, metaAnnType );
        return metaAnnotationTargetCache.getUnchecked( key );
    }

    public static void clearCaches()
    {
        annotationsListCache.invalidateAll();
        metaAnnotationCache.invalidateAll();
        metaAnnotationTargetCache.invalidateAll();
    }

    @Nonnull
    private static Collection<Annotation> findAnnotations( @Nonnull Method method )
    {
        Collection<Annotation> annotations = new LinkedHashSet<>();

        Class<?>[] parameterTypes = method.getParameterTypes();
        for( TypeToken<?> typeToken : TypeTokens.of( method.getDeclaringClass() ).getTypes() )
        {
            Class<?> type = typeToken.getRawType();
            try
            {
                Method declaredMethod = type.getDeclaredMethod( method.getName(), parameterTypes );
                annotations.addAll( asList( declaredMethod.getAnnotations() ) );
            }
            catch( NoSuchMethodException ignore )
            {
            }
        }
        return annotations;
    }

    @Nonnull
    private static <MetaAnnType extends Annotation> Optional<MetaAnnType> findMetaAnnotation(
            @Nonnull Method method,
            @Nonnull Class<MetaAnnType> metaAnnType,
            @Nonnull Set<Class<? extends Annotation>> annotations )
    {
        // scan this method's annotations, traversing upwards through the superclasses and interfaces
        Class<?>[] parameterTypes = method.getParameterTypes();
        for( TypeToken<?> typeToken : TypeTokens.of( method.getDeclaringClass() ).getTypes() )
        {
            // find the method in current type
            Method declaredMethod;
            try
            {
                declaredMethod = typeToken.getRawType().getDeclaredMethod( method.getName(), parameterTypes );
            }
            catch( NoSuchMethodException ignore )
            {
                continue;
            }

            // first check if type is annotated with the meta annotation directly
            MetaAnnType metaAnn = declaredMethod.getAnnotation( metaAnnType );
            if( metaAnn != null )
            {
                return Optional.of( metaAnn );
            }

            // not annotated directly - check recursively type's annotations if one of them has meta-ann somewhere
            for( Annotation annotation : declaredMethod.getAnnotations() )
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
                    Optional<MetaAnnType> result = ClassAnnotations.getMetaAnnotation( annotation.annotationType(), metaAnnType );
                    if( result.isPresent() )
                    {
                        return result;
                    }
                }
            }
        }

        // not found
        return Optional.absent();
    }

    @Nonnull
    private static Optional<Annotation> findMetaAnnotationTarget(
            @Nonnull Method method,
            @Nonnull Class<? extends Annotation> metaAnnType,
            @Nonnull Set<Class<? extends Annotation>> annotations )
    {
        // scan this method's annotations, traversing upwards through the superclasses and interfaces
        Class<?>[] parameterTypes = method.getParameterTypes();
        for( TypeToken<?> typeToken : TypeTokens.of( method.getDeclaringClass() ).getTypes() )
        {
            // find the method in current type
            Method declaredMethod;
            try
            {
                declaredMethod = typeToken.getRawType().getDeclaredMethod( method.getName(), parameterTypes );
            }
            catch( NoSuchMethodException ignore )
            {
                continue;
            }

            // check recursively method's annotations if one of them has meta-ann somewhere
            for( Annotation annotation : declaredMethod.getAnnotations() )
            {
                Class<? extends Annotation> annotationType = annotation.annotationType();

                Annotation metaAnnotation = annotationType.getAnnotation( metaAnnType );
                if( metaAnnotation != null )
                {
                    // this annotation is annotated with the meta-annotation, we've found the grail! return the
                    // annotated annotation (NOT the actual meta annotation instance)
                    return Optional.of( annotation );
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
                    Optional<Annotation> result = ClassAnnotations.getMetaAnnotationTarget( annotationType, metaAnnType );
                    if( result.isPresent() )
                    {
                        return result;
                    }
                }
            }
        }

        // not found
        return Optional.absent();
    }

    private MethodAnnotations()
    {
    }
}

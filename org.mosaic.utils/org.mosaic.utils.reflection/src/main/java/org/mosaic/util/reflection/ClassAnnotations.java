package org.mosaic.util.reflection;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
public final class ClassAnnotations
{
    @Nonnull
    private static final LoadingCache<Class<?>, Collection<Annotation>> annotationsListCache =
            CacheBuilder.newBuilder()
                        .concurrencyLevel( 10 )
                        .build( new CacheLoader<Class<?>, Collection<Annotation>>()
                        {
                            @Nonnull
                            @Override
                            public Collection<Annotation> load( @Nonnull Class<?> key ) throws Exception
                            {
                                return findAnnotations( key );
                            }
                        } );

    @Nonnull
    private static final LoadingCache<Pair<Class<?>, Class<? extends Annotation>>, Optional<? extends Annotation>> metaAnnotationCache =
            CacheBuilder.newBuilder()
                        .concurrencyLevel( 10 )
                        .build( new CacheLoader<Pair<Class<?>, Class<? extends Annotation>>, Optional<? extends Annotation>>()
                        {
                            @Nonnull
                            @Override
                            public Optional<? extends Annotation> load( @Nonnull Pair<Class<?>, Class<? extends Annotation>> key )
                                    throws Exception
                            {
                                return findMetaAnnotation( key.getLeft(),
                                                           key.getRight(),
                                                           Sets.<Class<? extends Annotation>>newHashSet() );
                            }
                        } );

    @Nonnull
    private static final LoadingCache<Pair<Class<?>, Class<? extends Annotation>>, Optional<Annotation>> metaAnnotationTargetCache =
            CacheBuilder.newBuilder()
                        .concurrencyLevel( 10 )
                        .build( new CacheLoader<Pair<Class<?>, Class<? extends Annotation>>, Optional<Annotation>>()
                        {
                            @Nonnull
                            @Override
                            public Optional<Annotation> load( @Nonnull Pair<Class<?>, Class<? extends Annotation>> key )
                                    throws Exception
                            {
                                return findMetaAnnotationTarget( key.getLeft(),
                                                                 key.getRight(),
                                                                 Sets.<Class<? extends Annotation>>newHashSet() );
                            }
                        } );

    @Nonnull
    public static Collection<Annotation> getAnnotations( @Nonnull Class<?> type )
    {
        try
        {
            return annotationsListCache.getUnchecked( type );
        }
        catch( UncheckedExecutionException e )
        {
            Throwable cause = e.getCause();
            if( cause instanceof TypeNotPresentException )
            {
                return Collections.emptyList();
            }
            else
            {
                throw e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    public static <MetaAnnType extends Annotation> Optional<MetaAnnType> getMetaAnnotation(
            @Nonnull Class<?> type, @Nonnull Class<MetaAnnType> metaAnnType )
    {
        Pair<Class<?>, Class<? extends Annotation>> key = Pair.<Class<?>, Class<? extends Annotation>>of( type, metaAnnType );
        try
        {
            return ( Optional<MetaAnnType> ) metaAnnotationCache.getUnchecked( key );
        }
        catch( UncheckedExecutionException e )
        {
            Throwable cause = e.getCause();
            if( cause instanceof TypeNotPresentException )
            {
                return Optional.absent();
            }
            else
            {
                throw e;
            }
        }
    }

    @Nonnull
    public static Optional<Annotation> getMetaAnnotationTarget(
            @Nonnull Class<?> type, @Nonnull Class<? extends Annotation> metaAnnType )
    {
        try
        {
            Pair<Class<?>, Class<? extends Annotation>> key = Pair.<Class<?>, Class<? extends Annotation>>of( type, metaAnnType );
            return metaAnnotationTargetCache.getUnchecked( key );
        }
        catch( UncheckedExecutionException e )
        {
            Throwable cause = e.getCause();
            if( cause instanceof TypeNotPresentException )
            {
                return Optional.absent();
            }
            else
            {
                throw e;
            }
        }
    }

    public static void clearCaches()
    {
        annotationsListCache.invalidateAll();
        metaAnnotationCache.invalidateAll();
        metaAnnotationTargetCache.invalidateAll();
    }

    @Nonnull
    static Collection<Annotation> findAnnotations( @Nonnull Class<?> key )
    {
        Collection<Annotation> annotations = new LinkedHashSet<>();

        Class<?> type = key;
        while( type != null )
        {
            annotations.addAll( asList( type.getAnnotations() ) );
            type = type.getSuperclass();
        }

        return annotations;
    }

    @Nonnull
    static <MetaAnnType extends Annotation> Optional<MetaAnnType> findMetaAnnotation(
            @Nonnull Class<?> type,
            @Nonnull Class<MetaAnnType> metaAnnType,
            @Nonnull Set<Class<? extends Annotation>> annotations )
    {
        // scan this class' annotations, traversing upwards through the superclasses and interfaces
        for( TypeToken<?> typeToken : TypeTokens.of( type ).getTypes() )
        {
            Class<?> currentType = typeToken.getRawType();

            // first check if type is annotated with the meta annotation directly
            MetaAnnType metaAnn = currentType.getAnnotation( metaAnnType );
            if( metaAnn != null )
            {
                return Optional.of( metaAnn );
            }

            // not annotated directly - check recursively type's annotations if one of them has meta-ann somewhere
            for( Annotation annotation : currentType.getAnnotations() )
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
                    Optional<MetaAnnType> result = findMetaAnnotation( annotation.annotationType(), metaAnnType, annotations );
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
    static Optional<Annotation> findMetaAnnotationTarget(
            @Nonnull Class<?> type,
            @Nonnull Class<? extends Annotation> metaAnnType,
            @Nonnull Set<Class<? extends Annotation>> annotations )
    {
        // scan this class' annotation, traversing upwards through the superclasses and interfaces
        for( TypeToken<?> typeToken : TypeTokens.of( type ).getTypes() )
        {
            Class<?> currentType = typeToken.getRawType();
            for( Annotation annotation : currentType.getAnnotations() )
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
                    Optional<Annotation> result = findMetaAnnotationTarget( annotationType, metaAnnType, annotations );
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

    private ClassAnnotations()
    {
    }
}

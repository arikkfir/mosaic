package org.mosaic.core.util.base;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

import static java.util.Arrays.asList;

/**
 * @author arik
 * @todo the findAnnotations(*) methods can return the same annotation twice if they appear on different nodes in the hierarchy
 */
public final class AnnotationUtils
{
    @Nonnull
    public static Collection<Annotation> findAnnotations( @Nonnull Class<?> key )
    {
        return findAnnotations( getTypeResolver().resolve( key ) );
    }

    @Nullable
    public static <MetaAnnType extends Annotation> MetaAnnType findMetaAnnotation(
            @Nonnull Class<?> type,
            @Nonnull Class<MetaAnnType> metaAnnType )
    {
        return findMetaAnnotation( type, metaAnnType, new HashSet<>() );
    }

    @Nullable
    public static Annotation findMetaAnnotationTarget(
            @Nonnull Class<?> type,
            @Nonnull Class<? extends Annotation> metaAnnType )
    {
        return findMetaAnnotationTarget( type, metaAnnType, new HashSet<>() );
    }

    @Nonnull
    public static Collection<Annotation> findAnnotations( @Nonnull Method method )
    {
        Set<Annotation> annotations = null;

        ResolvedType resolvedType = getTypeResolver().resolve( method.getDeclaringClass() );
        while( resolvedType != null )
        {
            annotations = addAnnotationsFor( method, resolvedType, annotations );
            resolvedType = resolvedType.getParentClass();
        }

        return annotations == null ? Collections.<Annotation>emptyList() : annotations;
    }

    @Nullable
    public static <MetaAnnType extends Annotation> MetaAnnType findMetaAnnotation(
            @Nonnull Method method,
            @Nonnull Class<MetaAnnType> metaAnnType )
    {
        return findMetaAnnotation( method, metaAnnType, new HashSet<>() );
    }

    @Nullable
    public static Annotation findMetaAnnotationTarget(
            @Nonnull Method method,
            @Nonnull Class<? extends Annotation> metaAnnType )
    {
        return findMetaAnnotationTarget( method, metaAnnType, new HashSet<>() );
    }

    @Nonnull
    private static Collection<Annotation> findAnnotations( @Nullable ResolvedType type )
    {
        Set<Annotation> annotations = null;
        while( type != null )
        {
            annotations = addAnnotationsFor( type, annotations );
            type = type.getParentClass();
        }
        return annotations == null ? Collections.<Annotation>emptyList() : annotations;
    }

    @Nullable
    private static Set<Annotation> addAnnotationsFor( @Nonnull ResolvedType resolvedType,
                                                      @Nullable Set<Annotation> annotations )
    {
        // collect annotations from current type in the hierarchy
        Annotation[] typeAnnotations = resolvedType.getErasedType().getAnnotations();
        if( typeAnnotations != null && typeAnnotations.length > 0 )
        {
            if( annotations == null )
            {
                annotations = new LinkedHashSet<>();
            }
            annotations.addAll( asList( typeAnnotations ) );
        }

        // for current type in the hierarchy, and collect annotations from all its implemented interfaces
        for( ResolvedType interfaceType : resolvedType.getImplementedInterfaces() )
        {
            annotations = addAnnotationsFor( interfaceType, annotations );
        }
        return annotations;
    }

    @Nullable
    private static <MetaAnnType extends Annotation> MetaAnnType findMetaAnnotation(
            @Nullable Class<?> type,
            @Nonnull Class<MetaAnnType> metaAnnType,
            @Nonnull Set<Class<? extends Annotation>> annotations )
    {
        // scan this class' annotations, traversing upwards through the superclasses and interfaces
        while( type != null )
        {
            // first check if type is annotated with the meta annotation directly
            MetaAnnType metaAnn = type.getAnnotation( metaAnnType );
            if( metaAnn != null )
            {
                return metaAnn;
            }

            // not annotated directly - check recursively type's annotations if one of them has meta-ann somewhere
            for( Annotation annotation : type.getAnnotations() )
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
                    MetaAnnType result = findMetaAnnotation( annotation.annotationType(), metaAnnType, annotations );
                    if( result != null )
                    {
                        return result;
                    }
                }
            }

            type = type.getSuperclass();
        }

        // not found
        return null;
    }

    @Nullable
    private static Annotation findMetaAnnotationTarget(
            @Nullable Class<?> type,
            @Nonnull Class<? extends Annotation> metaAnnType,
            @Nonnull Set<Class<? extends Annotation>> annotations )
    {
        // scan this class' annotation, traversing upwards through the superclasses and interfaces
        while( type != null )
        {
            for( Annotation annotation : type.getAnnotations() )
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
                    Annotation result = findMetaAnnotationTarget( annotationType, metaAnnType, annotations );
                    if( result != null )
                    {
                        return result;
                    }
                }
            }
            type = type.getSuperclass();
        }

        // not found
        return null;
    }

    @Nullable
    private static Set<Annotation> addAnnotationsFor( @Nonnull Method method,
                                                      @Nonnull ResolvedType resolvedType,
                                                      @Nullable Set<Annotation> annotations )
    {
        // find our method in the current type in the type hierarchy (ie. method overriding)
        try
        {
            Method declaredMethod = resolvedType.getErasedType().getDeclaredMethod( method.getName(), method.getParameterTypes() );
            Annotation[] methodAnnotations = declaredMethod.getAnnotations();
            if( methodAnnotations != null && methodAnnotations.length > 0 )
            {
                if( annotations == null )
                {
                    annotations = new LinkedHashSet<>();
                }
                annotations.addAll( asList( methodAnnotations ) );
            }
        }
        catch( NoSuchMethodException ignore )
        {
        }

        // for current type in the hierarchy, and collect annotations for said method from all its implemented interfaces
        for( ResolvedType interfaceType : resolvedType.getImplementedInterfaces() )
        {
            annotations = addAnnotationsFor( method, interfaceType, annotations );
        }
        return annotations;
    }

    @Nullable
    private static <MetaAnnType extends Annotation> MetaAnnType findMetaAnnotation(
            @Nonnull Method method,
            @Nonnull Class<MetaAnnType> metaAnnType,
            @Nonnull Set<Class<? extends Annotation>> annotations )
    {
        // scan this method's annotations, traversing upwards through the superclasses and interfaces
        Class<?>[] parameterTypes = method.getParameterTypes();
        Class<?> type = method.getDeclaringClass();
        while( type != null )
        {
            // find the method in current type
            Method declaredMethod;
            try
            {
                declaredMethod = type.getDeclaredMethod( method.getName(), parameterTypes );
            }
            catch( NoSuchMethodException ignore )
            {
                type = type.getSuperclass();
                continue;
            }

            // first check if type is annotated with the meta annotation directly
            MetaAnnType metaAnn = declaredMethod.getAnnotation( metaAnnType );
            if( metaAnn != null )
            {
                return metaAnn;
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
                    MetaAnnType result = findMetaAnnotation( annotation.annotationType(), metaAnnType );
                    if( result != null )
                    {
                        return result;
                    }
                }
            }
            type = type.getSuperclass();
        }

        // not found
        return null;
    }

    @Nullable
    private static Annotation findMetaAnnotationTarget(
            @Nonnull Method method,
            @Nonnull Class<? extends Annotation> metaAnnType,
            @Nonnull Set<Class<? extends Annotation>> annotations )
    {
        // scan this method's annotations, traversing upwards through the superclasses and interfaces
        Class<?>[] parameterTypes = method.getParameterTypes();
        Class<?> type = method.getDeclaringClass();
        while( type != null )
        {
            // find the method in current type
            Method declaredMethod;
            try
            {
                declaredMethod = type.getDeclaredMethod( method.getName(), parameterTypes );
            }
            catch( NoSuchMethodException ignore )
            {
                type = type.getSuperclass();
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
                    Annotation result = findMetaAnnotationTarget( annotationType, metaAnnType, annotations );
                    if( result != null )
                    {
                        return result;
                    }
                }
            }

            type = type.getSuperclass();
        }

        // not found
        return null;
    }

    @Nonnull
    private static TypeResolver getTypeResolver()
    {
        return new TypeResolver();
    }

    private AnnotationUtils()
    {
    }
}

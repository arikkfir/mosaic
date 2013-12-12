package org.mosaic.util.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public class AnnotationFinder
{
    @Nonnull
    private final AnnotatedElement element;

    public AnnotationFinder( @Nonnull AnnotatedElement element )
    {
        this.element = element;
    }

    @Nullable
    public <T extends Annotation> T findDeep( @Nonnull Class<T> desiredAnnotationType )
    {
        Set<Class<? extends Annotation>> encountered = new HashSet<>();
        return findDeep( desiredAnnotationType, this.element, encountered );
    }

    @Nullable
    public Annotation findAnnotationAnnotatedDeeplyBy( @Nonnull Class<? extends Annotation> markerAnnotationType )
    {
        Set<Class<? extends Annotation>> encountered = new HashSet<>();
        return findAnnotationAnnotatedDeeplyBy( markerAnnotationType, this.element, encountered );
    }

    @Nullable
    public <T extends Annotation> T find( @Nonnull Class<T> desiredAnnotationType )
    {
        if( this.element instanceof Class<?> )
        {
            Class<?> type = ( Class<?> ) this.element;
            while( type != null )
            {
                T annotation = type.getAnnotation( desiredAnnotationType );
                if( annotation != null )
                {
                    return annotation;
                }
                type = type.getSuperclass();
            }
            return null;
        }
        else if( this.element instanceof Method )
        {
            Method method = ( Method ) this.element;
            Class<?>[] parameterTypes = method.getParameterTypes();

            Class<?> type = method.getDeclaringClass();
            while( type != null )
            {
                try
                {
                    Method declaredMethod = type.getMethod( method.getName(), parameterTypes );
                    T annotation = declaredMethod.getAnnotation( desiredAnnotationType );
                    if( annotation != null )
                    {
                        return annotation;
                    }
                }
                catch( NoSuchMethodException ignore )
                {
                }
                type = type.getSuperclass();
            }
            return null;
        }
        else
        {
            return this.element.getAnnotation( desiredAnnotationType );
        }
    }

    @Nullable
    private <T extends Annotation> T findDeep( @Nonnull Class<T> desiredAnnotationType,
                                               @Nonnull AnnotatedElement element,
                                               @Nonnull Set<Class<? extends Annotation>> encountered )
    {
        if( element instanceof Class<?> )
        {
            Class<?> elementAsClass = ( Class<?> ) element;
            if( elementAsClass.isAnnotation() )
            {
                Class<? extends Annotation> elementAsAnnotationClass = elementAsClass.asSubclass( Annotation.class );
                if( encountered.contains( elementAsAnnotationClass ) )
                {
                    return null;
                }
                encountered.add( elementAsAnnotationClass );
            }
        }

        for( Annotation annotation : element.getAnnotations() )
        {
            if( desiredAnnotationType.equals( annotation.annotationType() ) )
            {
                return desiredAnnotationType.cast( annotation );
            }

            T foundAnnotation = findDeep( desiredAnnotationType, annotation.annotationType(), encountered );
            if( foundAnnotation != null )
            {
                return foundAnnotation;
            }
        }
        return null;
    }

    @Nullable
    private Annotation findAnnotationAnnotatedDeeplyBy( @Nonnull Class<? extends Annotation> desiredAnnotationType,
                                                        @Nonnull AnnotatedElement element,
                                                        @Nonnull Set<Class<? extends Annotation>> encountered )
    {
        if( element instanceof Class<?> )
        {
            Class<?> elementAsClass = ( Class<?> ) element;
            if( elementAsClass.isAnnotation() )
            {
                Class<? extends Annotation> elementAsAnnotationClass = elementAsClass.asSubclass( Annotation.class );
                if( encountered.contains( elementAsAnnotationClass ) )
                {
                    return null;
                }
                encountered.add( elementAsAnnotationClass );
            }
        }

        for( Annotation annotation : element.getAnnotations() )
        {
            if( desiredAnnotationType.equals( annotation.annotationType() ) )
            {
                return annotation;
            }

            Annotation foundAnnotation = findAnnotationAnnotatedDeeplyBy( desiredAnnotationType, annotation.annotationType(), encountered );
            if( foundAnnotation != null )
            {
                return annotation;
            }
        }
        return null;
    }
}

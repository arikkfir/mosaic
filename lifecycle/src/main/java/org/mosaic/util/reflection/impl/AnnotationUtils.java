package org.mosaic.util.reflection.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotations;

/**
 * @author arik
 */
public abstract class AnnotationUtils
{
    @Nullable
    public static <T extends Annotation> T getAnnotation( @Nonnull Class<?> target, @Nonnull Class<T> searchFor )
    {
        try
        {
            Set<Annotation> annotations = new HashSet<>();
            Class<?> clazz = target;
            while( clazz != null )
            {
                for( Annotation annotation : clazz.getDeclaredAnnotations() )
                {
                    Annotation found = addAnnotations( annotations, annotation, searchFor );
                    if( found != null )
                    {
                        return searchFor.cast( found );
                    }
                }
                clazz = clazz.getSuperclass();
            }
            return null;
        }
        catch( ClassCastException e )
        {
            throw new IllegalStateException( "Could not search for annotation '" + searchFor.getName() + "' on '" + target.getName() + "': " + e.getMessage(), e );
        }
    }

    public static <T extends Annotation> T getAnnotation( Method target, Class<T> searchFor )
    {
        T annotation = findAnnotation( target, searchFor );
        if( annotation != null )
        {
            return annotation;
        }

        Set<Annotation> annotations = new HashSet<>();
        for( Annotation methodAnnotation : getAnnotations( target ) )
        {
            Annotation found = addAnnotations( annotations, methodAnnotation, searchFor );
            if( found != null )
            {
                return searchFor.cast( found );
            }
        }
        return null;
    }

    private static Annotation addAnnotations( @Nonnull Set<Annotation> annotations,
                                              @Nonnull Annotation annotation,
                                              @Nonnull Class<? extends Annotation> searchingFor )
    {
        if( annotations.contains( annotation ) )
        {
            return null;
        }
        else if( annotation.annotationType().equals( searchingFor ) )
        {
            return annotation;
        }
        else
        {
            annotations.add( annotation );
            for( Annotation ann : annotation.annotationType().getAnnotations() )
            {
                Annotation found = addAnnotations( annotations, ann, searchingFor );
                if( found != null )
                {
                    return found;
                }
            }
        }
        return null;
    }
}

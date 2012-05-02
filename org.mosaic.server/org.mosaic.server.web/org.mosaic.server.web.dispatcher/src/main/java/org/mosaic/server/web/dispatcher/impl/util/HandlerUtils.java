package org.mosaic.server.web.dispatcher.impl.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * @author arik
 */
public abstract class HandlerUtils
{
    public static <A extends Annotation> A findAnn( Method method, Class<A> type )
    {
        A ann = findAnnotation( method, type );
        if( ann == null )
        {
            return findAnnotation( method.getDeclaringClass( ), type );
        }
        else
        {
            return ann;
        }
    }
}

package org.mosaic.lifecycle;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author arik
 */
public interface MethodEndpointInfo
{

    String TYPE = "methodEndpointType";

    String SHORT_TYPE = "methodEndpointShortType";

    String METHOD_NAME = "methodName";

    @SuppressWarnings( "UnusedDeclaration" )
    boolean isOfType( Class<? extends Annotation> annotationType );

    String getOrigin( );

    Annotation getType( );

    Method getMethod( );

    Object invoke( Object... arguments ) throws InvocationTargetException, IllegalAccessException;
}

package org.mosaic.lifecycle;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.osgi.framework.Bundle;

/**
 * @author arik
 */
public interface MethodEndpointInfo {

    String TYPE = "methodEndpointType";

    String SHORT_TYPE = "methodEndpointShortType";

    boolean isOfType( Class<? extends Annotation> annotationType );

    Annotation getType();

    Method getMethod();

    Object invoke( Object... arguments ) throws InvocationTargetException, IllegalAccessException;

    Bundle getBundle();
}

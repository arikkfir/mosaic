package org.mosaic.lifecycle;

import java.lang.annotation.Annotation;

/**
 * @author arik
 */
public interface ClassEndpointInfo
{

    String TYPE = "classEndpointType";

    String SHORT_TYPE = "classEndpointShortType";

    String CLASS_NAME = "className";

    @SuppressWarnings( "UnusedDeclaration" )
    boolean isOfType( Class<? extends Annotation> annotationType );

    String getOrigin( );

    Annotation getType( );

    Class<?> getClassType( );

    Object getEndpoint( );

}

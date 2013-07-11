package org.mosaic.web.marshall.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mosaic.lifecycle.annotation.MethodEndpointMarker;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@MethodEndpointMarker
public @interface Marshaller
{
    String produces();
}

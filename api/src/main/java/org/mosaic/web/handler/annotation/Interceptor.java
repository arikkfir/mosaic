package org.mosaic.web.handler.annotation;

import java.lang.annotation.*;
import org.mosaic.lifecycle.annotation.MethodEndpointMarker;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@MethodEndpointMarker
public @interface Interceptor
{
    String[] path() default "";

    Class<? extends Annotation> type() default Handler.class;
}

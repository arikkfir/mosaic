package org.mosaic.lifecycle.annotation;

import java.lang.annotation.*;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
public @interface MethodEndpointRefs
{
    Class<? extends Annotation> value();

    // FEATURE arik: can be nice to support "min()" to specify minimum number of services required
}

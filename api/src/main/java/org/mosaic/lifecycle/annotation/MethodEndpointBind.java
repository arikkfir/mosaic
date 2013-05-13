package org.mosaic.lifecycle.annotation;

import java.lang.annotation.*;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
public @interface MethodEndpointBind
{
    Class<? extends Annotation> value();

    boolean updates() default false;
}

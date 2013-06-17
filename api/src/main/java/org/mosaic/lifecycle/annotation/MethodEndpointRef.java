package org.mosaic.lifecycle.annotation;

import java.lang.annotation.*;

/**
 * @author arik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodEndpointRef
{
    Class<? extends Annotation> value();

    boolean required() default true;
}

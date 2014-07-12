package org.mosaic.it.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
@Target(ElementType.METHOD)
public @interface WithServer
{
    String[] modules() default { };
}

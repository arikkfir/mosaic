package org.mosaic.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author arik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component
{
    Class<?> value() default void.class;

    Property[] properties() default { };

    @interface Property
    {
        String name();

        String value();
    }
}

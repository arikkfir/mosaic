package org.mosaic.core.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
public @interface OnServiceUnregistration
{
    Property[] properties() default { };

    @interface Property
    {
        String name();

        String value();
    }
}

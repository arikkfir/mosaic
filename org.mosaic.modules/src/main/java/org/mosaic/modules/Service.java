package org.mosaic.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author arik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR })
@Component
public @interface Service
{
    Class<?>[] value() default { };

    P[] properties() default { };

    @interface P
    {
        String key();

        String value();
    }
}

package org.mosaic.core.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author arik
 * @feature support method injections
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Inject
{
    Property[] properties() default { };

    @interface Property
    {
        String name();

        String value();
    }
}

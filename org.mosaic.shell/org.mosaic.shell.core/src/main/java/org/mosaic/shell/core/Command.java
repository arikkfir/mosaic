package org.mosaic.shell.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mosaic.core.components.EndpointMarker;

/**
 * @author arik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@EndpointMarker
public @interface Command
{
    String[] names() default { };

    String synopsis() default "";

    String description() default "";

    @Retention( RetentionPolicy.RUNTIME )
    @Target( ElementType.PARAMETER )
    @interface Arg
    {
        String name() default "";

        String synopsis() default "";

        String description() default "";
    }

    @Retention( RetentionPolicy.RUNTIME )
    @Target( ElementType.PARAMETER )
    @interface Option
    {
        String[] names() default { };

        String synopsis() default "";

        String description() default "";

        String defaultValue() default "##org.mosaic.null";
    }
}

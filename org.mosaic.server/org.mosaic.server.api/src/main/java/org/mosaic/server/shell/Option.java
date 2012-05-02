package org.mosaic.server.shell;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes a command arguments.
 * <p/>
 * Boolean arguments default to true if not specified on the command line.
 *
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.PARAMETER )
public @interface Option
{

    String alias() default "";

}

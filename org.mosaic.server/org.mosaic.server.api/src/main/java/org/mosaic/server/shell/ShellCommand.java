package org.mosaic.server.shell;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mosaic.lifecycle.MethodEndpointMarker;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@MethodEndpointMarker
public @interface ShellCommand
{

    String value( ) default "";

}

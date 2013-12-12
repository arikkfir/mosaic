package org.mosaic.pathwatchers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mosaic.modules.spi.MethodEndpointMarker;
import org.mosaic.util.resource.PathEvent;

import static org.mosaic.util.resource.PathEvent.*;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@MethodEndpointMarker
public @interface PathWatcher
{
    String value();

    PathEvent[] events() default { CREATED, MODIFIED, DELETED };
}

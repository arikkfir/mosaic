package org.mosaic.filewatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mosaic.lifecycle.annotation.MethodEndpointMarker;

/**
 * @author arik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@MethodEndpointMarker
public @interface FileWatcher
{
    WatchRoot root() default WatchRoot.HOME;

    String pattern();

    WatchEvent[] event() default { };

    boolean skipSvn() default true;
}

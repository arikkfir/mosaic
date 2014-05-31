package org.mosaic.demo.demo1;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mosaic.core.EndpointMarker;

/**
 * @author arik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@EndpointMarker
public @interface DemoEndpointOther
{
}

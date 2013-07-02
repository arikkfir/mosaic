package org.mosaic.web.handler.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mosaic.web.net.HttpMethod;

import static org.mosaic.web.net.HttpMethod.*;

/**
 * @author arik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface Method
{
    HttpMethod[] value() default { DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE };
}

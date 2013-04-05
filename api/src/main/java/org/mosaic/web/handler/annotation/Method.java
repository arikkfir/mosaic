package org.mosaic.web.handler.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mosaic.net.HttpMethod;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
public @interface Method
{
    HttpMethod[] value();
}
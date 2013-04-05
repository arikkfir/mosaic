package org.mosaic.lifecycle.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mosaic.util.weaving.EnableWeaving;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@EnableWeaving
public @interface Measure
{
}

package org.mosaic.dao;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mosaic.modules.Template;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Template
public @interface Dao
{
    String value();
}

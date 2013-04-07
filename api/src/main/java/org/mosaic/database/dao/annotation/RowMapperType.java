package org.mosaic.database.dao.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
public @interface RowMapperType
{
    Class<? extends org.mosaic.database.dao.RowMapper> value();

    boolean shareInstance() default true;
}
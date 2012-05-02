package org.mosaic.server.shell;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
public @interface Description
{

    String value();

}

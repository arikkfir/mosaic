package org.mosaic.describe;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author arik
 */
@Retention( RetentionPolicy.RUNTIME )
public @interface Label {

    String value();

}

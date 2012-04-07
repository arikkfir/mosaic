package org.mosaic.lifecycle;

import java.lang.annotation.Annotation;

/**
 * @author arik
 */
public interface MethodEndpointInfo {

    String TYPE = "methodEndpointType";

    String SHORT_TYPE = "methodEndpointShortType";

    Class<? extends Annotation> getType();
}

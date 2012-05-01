package org.mosaic.server.web;

import org.mosaic.util.collection.TypedDict;

/**
 * @author arik
 */
public interface PathParamsAware {

    void setPathParams( TypedDict<String> params );

}

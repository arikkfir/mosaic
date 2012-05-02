package org.mosaic.server.web;

import java.util.Map;

/**
 * @author arik
 */
public interface PathParamsAware
{
    void setPathParams( Map<String, String> params );
}

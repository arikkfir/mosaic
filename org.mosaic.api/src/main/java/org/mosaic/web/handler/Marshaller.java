package org.mosaic.web.handler;

import org.mosaic.web.HttpRequest;

/**
 * @author arik
 */
public interface Marshaller
{
    boolean matches( HttpRequest request, Object handlerResult );

    Object marshall( HttpRequest request, Object handlerResult ) throws Exception;
}

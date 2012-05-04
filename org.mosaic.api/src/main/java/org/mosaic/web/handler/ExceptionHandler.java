package org.mosaic.web.handler;

import org.mosaic.web.HttpRequest;

/**
 * @author arik
 */
public interface ExceptionHandler
{
    boolean matches( HttpRequest request, Exception exception );

    Object handle( HttpRequest request, Exception exception ) throws Exception;
}

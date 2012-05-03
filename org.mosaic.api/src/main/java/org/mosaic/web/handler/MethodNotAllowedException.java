package org.mosaic.web.handler;

import org.mosaic.web.HttpException;
import org.mosaic.web.HttpRequest;

/**
 * @author arik
 */
public class MethodNotAllowedException extends HttpException
{
    public MethodNotAllowedException( HttpRequest request )
    {
        super( String.format( "Method '%s' is not allowed for path '%s'", request.getMethod(), request.getUrl().getPath() ) );
    }
}

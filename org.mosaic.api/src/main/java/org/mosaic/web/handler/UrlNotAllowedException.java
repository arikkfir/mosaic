package org.mosaic.web.handler;

import org.mosaic.web.HttpException;
import org.mosaic.web.HttpRequest;

/**
 * @author arik
 */
public class UrlNotAllowedException extends HttpException
{
    public UrlNotAllowedException( HttpRequest request )
    {
        super( String.format( "Path '%s' is not allowed", request.getUrl().getPath() ) );
    }
}

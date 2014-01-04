package org.mosaic.management.impl;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.mosaic.modules.Component;
import org.mosaic.web.handler.Controller;
import org.mosaic.web.handler.GET;
import org.mosaic.web.http.HttpStatus;
import org.mosaic.web.request.WebInvocation;

/**
 * @author arik
 */
@Component
final class Management
{
    @GET
    @Controller(uri = "/", app = "application.id == 'management'")
    public void home( @Nonnull WebInvocation request ) throws IOException
    {
        request.getHttpResponse().setLocation( "/index.html" );
        request.getHttpResponse().setStatus( HttpStatus.MOVED_PERMANENTLY, "" );
    }
}

package org.mosaic.demo.accounting;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.mosaic.modules.Component;
import org.mosaic.web.application.Application;
import org.mosaic.web.endpoint.Controller;
import org.mosaic.web.endpoint.GET;
import org.mosaic.web.server.WebInvocation;

/**
 * @author arik
 */
@Component
final class Home
{
    @GET
    @Controller(uri = "/", app = "application.id == 'accounting'")
    public Application.ApplicationResource home( @Nonnull WebInvocation invocation ) throws IOException
    {
        return invocation.getApplication().getResource( "/index.html.ftl" );
    }
}

package org.mosaic.demo.accounting;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.mosaic.modules.Component;
import org.mosaic.modules.Module;
import org.mosaic.web.endpoint.Controller;
import org.mosaic.web.endpoint.GET;
import org.mosaic.web.endpoint.Secured;
import org.mosaic.web.server.HttpStatus;
import org.mosaic.web.server.WebInvocation;

/**
 * @author arik
 */
@Component
public class Test
{
    public static class Hello
    {
        public final String greeting;

        public Hello( String greeting )
        {
            this.greeting = greeting;
        }
    }

    @Nonnull
    @Component
    private Module module;

    @GET
    @Controller(uri = "/", app = "application.id == 'accounting'")
    public void home( @Nonnull WebInvocation request ) throws IOException
    {
        request.getHttpResponse().setLocation( "/index.html" );
        request.getHttpResponse().setStatus( HttpStatus.MOVED_PERMANENTLY, "" );
    }

    @GET
    @Controller(uri = "/hello", app = "application.id == 'accounting'")
    @Secured(value = "true", authMethods = "basic", challangeMethod = "basic")
    public Hello helloService()
    {
        return new Hello( "Hello There!" );
    }
}

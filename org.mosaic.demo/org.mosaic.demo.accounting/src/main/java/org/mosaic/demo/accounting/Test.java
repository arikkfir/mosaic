package org.mosaic.demo.accounting;

import javax.annotation.Nonnull;
import org.mosaic.modules.Component;
import org.mosaic.modules.Module;
import org.mosaic.web.endpoint.Controller;
import org.mosaic.web.endpoint.GET;
import org.mosaic.web.endpoint.Secured;

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
    @Controller(uri = "/hello", app = "application.id == 'accounting'")
    @Secured(value = "true", authMethods = "basic", challangeMethod = "basic")
    public Hello helloService()
    {
        return new Hello( "Hello There!" );
    }
}

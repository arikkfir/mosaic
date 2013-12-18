package org.mosaic.management;

import org.mosaic.modules.Component;
import org.mosaic.web.handler.Controller;
import org.mosaic.web.handler.GET;

/**
 * @author arik
 */
@Component
public class ManagementServices
{
    @GET
    @Controller(uri = "/hello", app = "application.id == 'management'")
    public Hello helloService()
    {
        return new Hello( "Hello There!" );
    }

    public static class Hello
    {
        public final String greeting;

        public Hello( String greeting )
        {
            this.greeting = greeting;
        }
    }
}

package org.mosaic.management.impl;

import org.mosaic.modules.Component;
import org.mosaic.web.handler.Controller;
import org.mosaic.web.handler.GET;
import org.mosaic.web.security.Secured;

/**
 * @author arik
 */
@Component
final class ManagementServices
{
    public static class Hello
    {
        public final String greeting;

        public Hello( String greeting )
        {
            this.greeting = greeting;
        }
    }

    @GET
    @Controller(uri = "/hello", app = "application.id == 'management'")
    @Secured(value = "true", authMethods = "basic", challangeMethod = "basic")
    public Hello helloService()
    {
        return new Hello( "Hello There!" );
    }
}

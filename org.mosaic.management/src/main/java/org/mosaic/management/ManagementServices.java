package org.mosaic.management;

import org.mosaic.modules.Component;
import org.mosaic.web.handler.Controller;

/**
 * @author arik
 */
@Component
public class ManagementServices
{
    @Controller( app = "application.id == 'management'", uri = "/hello" )
    public String helloService()
    {
        return "Hello World!";
    }
}

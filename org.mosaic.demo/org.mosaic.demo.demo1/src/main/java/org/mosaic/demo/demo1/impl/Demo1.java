package org.mosaic.demo.demo1.impl;

import org.mosaic.core.components.Component;
import org.mosaic.core.components.Inject;
import org.mosaic.core.services.ServiceManager;
import org.mosaic.demo.demo1.DemoEndpoint;
import org.mosaic.demo.demo1.DemoEndpointOther;
import org.mosaic.demo.demo1.DemoItem;

/**
 * @author arik
 */
@Component(DemoItem.class)
class Demo1 implements DemoItem
{
    @Inject
    private ServiceManager serviceManager;

    @Override
    public void printMe()
    {
        System.out.printf( "Service manager: %s\n", this.serviceManager );
    }

    @DemoEndpoint
    public void demoEndpoint()
    {
        System.out.println( "Demo endpoint activated!" );
    }

    @DemoEndpointOther
    public void demoEndpointOther()
    {
        System.out.println( "Demo endpoint (other) activated!" );
    }
}

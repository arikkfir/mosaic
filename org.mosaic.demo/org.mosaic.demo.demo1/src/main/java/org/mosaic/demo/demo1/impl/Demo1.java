package org.mosaic.demo.demo1.impl;

import org.mosaic.core.Component;
import org.mosaic.core.Inject;
import org.mosaic.core.ServiceManager;
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
}

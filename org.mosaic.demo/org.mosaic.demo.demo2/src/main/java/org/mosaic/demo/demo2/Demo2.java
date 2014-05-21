package org.mosaic.demo.demo2;

import org.mosaic.core.Component;
import org.mosaic.core.Inject;
import org.mosaic.core.ServiceProvider;
import org.mosaic.demo.demo1.DemoItem;

/**
 * @author arik
 */
@Component
public class Demo2
{
    @Inject
    private ServiceProvider<DemoItem> demo1Provider;

    @Inject
    private DemoItem demo1;

    public Demo2()
    {
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep( 2000 );
                }
                catch( InterruptedException e )
                {
                    e.printStackTrace();
                }
                demo1.printMe();
            }
        } ).start();
    }
}

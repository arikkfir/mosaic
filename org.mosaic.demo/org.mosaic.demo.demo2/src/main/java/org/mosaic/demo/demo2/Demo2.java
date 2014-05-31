package org.mosaic.demo.demo2;

import java.util.List;
import org.mosaic.core.Component;
import org.mosaic.core.Inject;
import org.mosaic.core.MethodEndpoint;
import org.mosaic.core.ServiceProvider;
import org.mosaic.core.util.logging.Logging;
import org.mosaic.demo.demo1.DemoEndpoint;
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

    @Inject
    private List<MethodEndpoint<DemoEndpoint>> demoEndpoints;

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

                for( MethodEndpoint<DemoEndpoint> demoEndpoint : demoEndpoints )
                {
                    try
                    {
                        demoEndpoint.invoke();
                    }
                    catch( Throwable throwable )
                    {
                        Logging.getLogger().info( "Error invoking endpoint: {}", throwable.getMessage(), throwable );
                    }
                }
            }
        } ).start();
    }
}

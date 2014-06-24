package org.mosaic.demo.demo2;

import java.util.List;
import org.mosaic.core.components.*;
import org.mosaic.core.services.ServiceProvider;
import org.mosaic.core.services.ServiceRegistration;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.logging.Logging;
import org.mosaic.demo.demo1.DemoEndpoint;
import org.mosaic.demo.demo1.DemoItem;
import org.slf4j.Logger;

/**
 * @author arik
 */
@Component
public class Demo2
{
    private static final Logger LOG = Logging.getLogger();

    @Inject
    private ServiceProvider<DemoItem> demo1Provider;

    @Inject
    @Nonnull
    private DemoItem demo1;

    @Inject
    private List<MethodEndpoint<DemoEndpoint>> demoEndpoints;

    public Demo2()
    {
        //noinspection ConstantConditions
        if( this.demo1 == null )
        {
            LOG.warn( "NO DemoItem INJECTED!" );
        }
        else
        {
            LOG.info( "Printout from INJECTED 'this.demo1': " );
            this.demo1.printMe();
        }

        new Thread( () -> {
            try
            {
                Thread.sleep( 5000 );
            }
            catch( InterruptedException e )
            {
                throw new RuntimeException( e );
            }

            DemoItem demoItem = this.demo1Provider.getService();
            if( demoItem == null )
            {
                LOG.warn( "NO DemoItem returned from DemoItem service provider!" );
            }
            else
            {
                LOG.info( "Printout from PROVIDED 'DemoItem': " );
                demoItem.printMe();
            }

            if( this.demoEndpoints.isEmpty() )
            {
                LOG.warn( "EMPTY DemoEndpoint LIST INJECTED!" );
            }
            else
            {
                for( MethodEndpoint<DemoEndpoint> endpoint : this.demoEndpoints )
                {
                    try
                    {
                        LOG.info( "Printout from an INJECTED-to-list 'DemoEndpoint': " );
                        endpoint.invoke();
                    }
                    catch( Throwable throwable )
                    {
                        LOG.error( "Error invoking endpoint: {}", throwable.getMessage(), throwable );
                    }
                }
            }

        } ).start();
    }

    @OnServiceRegistration
    void onDemoEndpointRegistered( @Nonnull ServiceRegistration<MethodEndpoint<DemoEndpoint>> registration )
    {
        MethodEndpoint<DemoEndpoint> endpoint = registration.getService();
        if( endpoint != null )
        {
            try
            {
                LOG.info( "Printout from a TRACKED 'DemoEndpoint': " );
                endpoint.invoke();
            }
            catch( Throwable throwable )
            {
                LOG.error( "Error invoking endpoint: {}", throwable.getMessage(), throwable );
            }
        }
    }

    @OnServiceUnregistration
    void onDemoEndpointUnregistered( @Nonnull ServiceRegistration<MethodEndpoint<DemoEndpoint>> registration,
                                     @Nonnull MethodEndpoint<DemoEndpoint> service )
    {
        LOG.info( "a TRACKED 'DemoEndpoint' disappeared: {}", service );
    }
}

package org.mosaic.it.tests;

import javax.annotation.Nonnull;
import org.junit.Assert;
import org.mosaic.it.runner.ServerBootstrap;

/**
 * @author arik
 */
public abstract class BaseServerTest extends Assert
{
    protected void doWithServer( @Nonnull ServerRunnable runnable ) throws Exception
    {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.start();
        try
        {
            runnable.run( bootstrap );
        }
        finally
        {
            bootstrap.shutdown();
        }
    }

    protected static interface ServerRunnable
    {
        void run( @Nonnull ServerBootstrap serverBootstrap ) throws Exception;
    }
}

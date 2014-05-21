package org.mosaic.core.impl;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author arik
 */
public class Activator implements BundleActivator
{
    @Nullable
    private static ServerImpl server;

    @Nullable
    public static ServerImpl getServer()
    {
        return Activator.server;
    }

    @Override
    public void start( @Nonnull BundleContext context ) throws Exception
    {
        ServerImpl server = new ServerImpl( context );
        server.transitionTo( ServerImpl.STARTED, true );
        Activator.server = server;
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        ServerImpl server = Activator.server;
        if( server != null )
        {
            server.transitionTo( ServerImpl.STOPPED, false );
        }
    }
}

package org.mosaic.server.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.server.Server;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author arik
 */
public class Activator implements BundleActivator
{
    @Nullable
    private ServiceRegistration<Server> serverServiceRegistration;

    @Override
    public void start( @Nonnull BundleContext context ) throws Exception
    {
        this.serverServiceRegistration = context.registerService( Server.class, new ServerImpl( context ), null );
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        ServiceRegistration<Server> serverServiceRegistration = this.serverServiceRegistration;
        if( serverServiceRegistration != null )
        {
            try
            {
                serverServiceRegistration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.serverServiceRegistration = null;
        }
    }
}

package org.mosaic.core.impl;

import org.mosaic.core.Module;
import org.mosaic.core.ModuleManager;
import org.mosaic.core.impl.methodinterception.MethodInterceptorsManager;
import org.mosaic.core.impl.module.ModuleManagerEx;
import org.mosaic.core.impl.service.ServiceManagerEx;
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

    @Nullable
    public static ServiceManagerEx getServiceManager()
    {
        ServerImpl server = getServer();
        if( server != null )
        {
            return server.getServiceManager();
        }
        return null;
    }

    @Nullable
    public static ModuleManagerEx getModuleManager()
    {
        ServerImpl server = getServer();
        if( server != null )
        {
            return server.getModuleManager();
        }
        return null;
    }

    @Nullable
    public static Module getCoreModule()
    {
        ModuleManager moduleManager = getModuleManager();
        if( moduleManager == null )
        {
            return null;
        }

        Module coreModule = moduleManager.getModule( 1 );
        if( coreModule == null )
        {
            return null;
        }

        return coreModule;
    }

    @Nullable
    public static MethodInterceptorsManager getMethodInterceptorsManager()
    {
        ServerImpl server = getServer();
        if( server != null )
        {
            return server.getMethodInterceptorsManager();
        }
        return null;
    }

    @Override
    public void start( @Nonnull BundleContext context ) throws Exception
    {
        ServerImpl server = new ServerImpl( context );
        Activator.server = server;
        server.transitionTo( ServerStatus.STARTED, true );
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        ServerImpl server = Activator.server;
        if( server != null )
        {
            server.transitionTo( ServerStatus.STOPPED, false );
        }
    }
}

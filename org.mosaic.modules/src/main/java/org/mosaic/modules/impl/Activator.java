package org.mosaic.modules.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.ModuleManager;
import org.mosaic.util.resource.PathEvent;
import org.mosaic.util.resource.PathWatcher;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;

import static org.mosaic.util.resource.PathEvent.*;

/**
 * @author arik
 */
public final class Activator implements BundleActivator
{
    @Nullable
    private static ModuleManagerImpl moduleManager;

    @Nonnull
    public static ModuleManager getModuleManager()
    {
        ModuleManagerImpl moduleManager = Activator.moduleManager;
        if( moduleManager == null )
        {
            throw new IllegalStateException( "module manager is not available" );
        }
        else
        {
            return moduleManager;
        }
    }

    @Nullable
    private ServiceRegistration<WeavingHook> weavingHookServiceRegistration;

    @Nullable
    private ServiceRegistration<ModuleManager> moduleManagerServiceRegistration;

    @Nullable
    private ServiceRegistration<PathWatcher> libWatcherServiceRegistration;

    @Override
    public void start( @Nonnull final BundleContext context ) throws Exception
    {
        this.weavingHookServiceRegistration = context.registerService( WeavingHook.class, new ModuleWeavingHook( context ), null );

        moduleManager = new ModuleManagerImpl();
        moduleManager.open( context );
        this.moduleManagerServiceRegistration = context.registerService( ModuleManager.class, moduleManager, null );

        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                Dictionary<String, Object> dict = new Hashtable<>();
                dict.put( "location", "${mosaic.home.lib}" );
                dict.put( "events", new PathEvent[] { CREATED, MODIFIED, DELETED, NOT_MODIFIED, SCAN_FINISHED } );
                Activator.this.libWatcherServiceRegistration = context.registerService( PathWatcher.class, new ServerLibWatcher(), dict );
            }
        }, "ModulesStarter" ).start();
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        ServiceRegistration<PathWatcher> libWatcherServiceRegistration = this.libWatcherServiceRegistration;
        if( libWatcherServiceRegistration != null )
        {
            try
            {
                libWatcherServiceRegistration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.libWatcherServiceRegistration = null;
        }

        ModuleManagerImpl moduleManager = Activator.moduleManager;
        if( moduleManager != null )
        {
            moduleManager.close( context );
            Activator.moduleManager = null;
        }

        ServiceRegistration<WeavingHook> weavingHookServiceRegistration = this.weavingHookServiceRegistration;
        if( weavingHookServiceRegistration != null )
        {
            try
            {
                weavingHookServiceRegistration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.weavingHookServiceRegistration = null;
        }

        ServiceRegistration<ModuleManager> moduleManagerServiceRegistration = this.moduleManagerServiceRegistration;
        if( moduleManagerServiceRegistration != null )
        {
            try
            {
                moduleManagerServiceRegistration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.moduleManagerServiceRegistration = null;
        }
    }
}

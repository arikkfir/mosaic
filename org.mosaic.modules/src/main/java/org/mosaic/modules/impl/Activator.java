package org.mosaic.modules.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.ModuleManager;
import org.mosaic.util.osgi.SimpleServiceTracker;
import org.mosaic.util.resource.PathMatcher;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.util.tracker.ServiceTracker;

import static org.mosaic.util.osgi.BundleUtils.bundleContext;
import static org.osgi.framework.FrameworkUtil.createFilter;

/**
 * @author arik
 */
public final class Activator implements BundleActivator
{
    @Nonnull
    private static final SimpleServiceTracker<PathMatcher> pathMatcherTracker = new SimpleServiceTracker<>( Activator.class, PathMatcher.class );

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

    @Nonnull
    public static PathMatcher getPathMatcher()
    {
        return Activator.pathMatcherTracker.require();
    }

    @Nonnull
    static Path getWorkPath()
    {
        BundleContext bundleContext = bundleContext( Activator.class );
        if( bundleContext == null )
        {
            throw new IllegalStateException( "could not find bundle context" );
        }

        String workPath = bundleContext.getProperty( "mosaic.home.work" );
        if( workPath == null )
        {
            throw new IllegalStateException( "bundle property 'mosaic.home.work' is missing" );
        }
        return Paths.get( workPath );
    }

    @Nullable
    private BytecodeWeavingHook bytecodeWeavingHook;

    @Nullable
    private ServiceRegistration<WeavingHook> weavingHookServiceRegistration;

    @Nullable
    private ServiceRegistration<ModuleManager> moduleManagerServiceRegistration;

    @Nullable
    private ServiceTracker<Runnable, Runnable> bundleScannerTracker;

    @Nullable
    private ScheduledExecutorService executor;

    @Override
    public void start( @Nonnull final BundleContext context ) throws Exception
    {
        this.bytecodeWeavingHook = new BytecodeWeavingHook();
        this.weavingHookServiceRegistration = context.registerService( WeavingHook.class, this.bytecodeWeavingHook, null );

        moduleManager = new ModuleManagerImpl();
        moduleManager.open( context );
        this.moduleManagerServiceRegistration = context.registerService( ModuleManager.class, moduleManager, null );

        this.bundleScannerTracker = new ServiceTracker<>( context,
                                                          createFilter( "(&(objectClass=java.lang.Runnable)(bundleScanner=true))" ),
                                                          null );
        this.bundleScannerTracker.open();

        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.executor.scheduleWithFixedDelay( new Runnable()
        {
            @Override
            public void run()
            {
                ServiceTracker<Runnable, Runnable> tracker = Activator.this.bundleScannerTracker;
                if( tracker != null )
                {
                    Runnable scanner = tracker.getService();
                    if( scanner != null )
                    {
                        scanner.run();
                    }
                }
            }
        }, 1, 1, TimeUnit.SECONDS );
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        ScheduledExecutorService executor = this.executor;
        if( executor != null )
        {
            executor.shutdown();
            this.executor = null;
        }

        ServiceTracker<Runnable, Runnable> tracker = this.bundleScannerTracker;
        if( tracker != null )
        {
            tracker.close();
            this.bundleScannerTracker = null;
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

        BytecodeWeavingHook hook = this.bytecodeWeavingHook;
        if( hook != null )
        {
            hook.stop();
            this.bytecodeWeavingHook = null;
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

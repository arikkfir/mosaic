package org.mosaic.core.impl;

import java.nio.file.Path;
import org.mosaic.core.ModuleRevision;
import org.mosaic.core.Server;
import org.mosaic.core.impl.bytecode.BytecodeWeavingHook;
import org.mosaic.core.impl.bytecode.ModuleRevisionLookup;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.version.Version;
import org.mosaic.core.util.workflow.Workflow;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRevision;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mosaic.core.util.logging.Logging.getMarkerLogger;

/**
 * @author arik
 */
class ServerImpl extends Workflow implements Server
{
    @Nonnull
    private final BundleContext bundleContext;

    @Nonnull
    private final Version version;

    @Nonnull
    private final Path home;

    @Nonnull
    private final Path apps;

    @Nonnull
    private final Path bin;

    @Nonnull
    private final Path etc;

    @Nonnull
    private final Path lib;

    @Nonnull
    private final Path logs;

    @Nonnull
    private final Path schemas;

    @Nonnull
    private final Path work;

    @Nonnull
    private final ServiceManagerImpl serviceManager;

    @Nonnull
    private final MethodInterceptorsManager methodInterceptorsManager;

    @Nonnull
    private final ModuleManagerImpl moduleManager;

    ServerImpl( @Nonnull BundleContext bundleContext )
    {
        super( new ReadWriteLock( "org.mosaic", 15, SECONDS ), "Mosaic", ServerStatus.STOPPED, getMarkerLogger( "server" ) );

        // initialize from external bundle configuration
        this.bundleContext = bundleContext;
        this.version = Version.valueOf( this.bundleContext.getProperty( "org.mosaic.version" ) );
        this.home = Util.getPath( this.bundleContext, "org.mosaic.home" );
        this.apps = Util.getPath( this.bundleContext, "org.mosaic.home.apps" );
        this.bin = Util.getPath( this.bundleContext, "org.mosaic.home.bin" );
        this.etc = Util.getPath( this.bundleContext, "org.mosaic.home.etc" );
        this.lib = Util.getPath( this.bundleContext, "org.mosaic.home.lib" );
        this.logs = Util.getPath( this.bundleContext, "org.mosaic.home.logs" );
        this.schemas = Util.getPath( this.bundleContext, "org.mosaic.home.schemas" );
        this.work = Util.getPath( this.bundleContext, "org.mosaic.home.work" );

        // add transitions
        addTransition( ServerStatus.STOPPED, ServerStatus.STARTED, TransitionDirection.FORWARD );
        addTransition( ServerStatus.STARTED, ServerStatus.STOPPED, TransitionDirection.BACKWARDS );

        // create bytecode weaver
        addListener( new BytecodeWeavingHook( this, this.work.resolve( "weaving" ), new ModuleRevisionLookup()
        {
            @Nullable
            @Override
            public ModuleRevision getModuleRevision( @Nonnull BundleRevision bundleRevision )
            {
                long bundleId = bundleRevision.getBundle().getBundleId();
                ModuleImpl module = moduleManager.getModule( bundleId );
                if( module == null )
                {
                    throw new IllegalArgumentException( "unknown module: " + bundleId );
                }
                else
                {
                    return module.getRevision( bundleRevision );
                }
            }
        } ) );

        // create the service manager
        this.serviceManager = addListener( new ServiceManagerImpl( this ) );

        // create method interceptors manager
        this.methodInterceptorsManager = addListener( new MethodInterceptorsManager( this ) );

        // create the module manager
        this.moduleManager = addListener( new ModuleManagerImpl( this ) );

        // create the module watcher
        addListener( new ModuleWatcher( this ) );
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this )
                             .add( "version", this.version )
                             .toString();
    }

    @Nonnull
    @Override
    public Version getVersion()
    {
        return this.version;
    }

    @Nonnull
    @Override
    public Path getHome()
    {
        return this.home;
    }

    @Nonnull
    @Override
    public Path getApps()
    {
        return this.apps;
    }

    @Nonnull
    @Override
    public Path getBin()
    {
        return this.bin;
    }

    @Nonnull
    @Override
    public Path getEtc()
    {
        return this.etc;
    }

    @Nonnull
    @Override
    public Path getLib()
    {
        return this.lib;
    }

    @Nonnull
    @Override
    public Path getLogs()
    {
        return this.logs;
    }

    @Nonnull
    @Override
    public Path getSchemas()
    {
        return this.schemas;
    }

    @Nonnull
    @Override
    public Path getWork()
    {
        return this.work;
    }

    @Nonnull
    BundleContext getBundleContext()
    {
        return this.bundleContext;
    }

    @Nonnull
    ServiceManagerImpl getServiceManager()
    {
        return this.serviceManager;
    }

    @Nonnull
    MethodInterceptorsManager getMethodInterceptorsManager()
    {
        return this.methodInterceptorsManager;
    }

    @Nonnull
    ModuleManagerImpl getModuleManager()
    {
        return this.moduleManager;
    }
}

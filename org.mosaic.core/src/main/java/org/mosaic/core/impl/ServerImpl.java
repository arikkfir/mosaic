package org.mosaic.core.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.mosaic.core.*;
import org.mosaic.core.impl.bytecode.BytecodeWeavingHook;
import org.mosaic.core.impl.bytecode.ModuleRevisionLookup;
import org.mosaic.core.impl.methodinterception.MethodInterceptorsManager;
import org.mosaic.core.impl.module.ModuleManagerImpl;
import org.mosaic.core.impl.module.ModuleWatcher;
import org.mosaic.core.impl.service.ServiceManagerImpl;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.version.Version;
import org.mosaic.core.util.workflow.Status;
import org.mosaic.core.util.workflow.TransitionAdapter;
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
    private static Path getPath( @Nonnull BundleContext bundleContext, @Nonnull String key )
    {
        String location = bundleContext.getProperty( key );
        if( location == null )
        {
            throw new IllegalStateException( "could not discover Mosaic directory location for '" + key + "'" );
        }
        else
        {
            return Paths.get( location );
        }
    }

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
        this.version = Version.valueOf( bundleContext.getProperty( "org.mosaic.version" ) );
        this.home = getPath( bundleContext, "org.mosaic.home" );
        this.apps = getPath( bundleContext, "org.mosaic.home.apps" );
        this.bin = getPath( bundleContext, "org.mosaic.home.bin" );
        this.etc = getPath( bundleContext, "org.mosaic.home.etc" );
        this.lib = getPath( bundleContext, "org.mosaic.home.lib" );
        this.logs = getPath( bundleContext, "org.mosaic.home.logs" );
        this.schemas = getPath( bundleContext, "org.mosaic.home.schemas" );
        this.work = getPath( bundleContext, "org.mosaic.home.work" );

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
                return moduleManager.getModuleRevision( bundleRevision );
            }
        } ) );

        // create the service manager
        this.serviceManager = addListener( new ServiceManagerImpl( this.logger, getLock() ) );

        // create method interceptors manager
        this.methodInterceptorsManager = addListener( new MethodInterceptorsManager( this.logger, this.getLock(), this.serviceManager ) );

        // create the module manager
        this.moduleManager = addListener( new ModuleManagerImpl( this.logger, getLock(), this.serviceManager ) );

        // add a listener to register core services
        addListener( new TransitionAdapter()
        {
            @Override
            public void execute( @Nonnull Status origin, @Nonnull Status target ) throws Exception
            {
                if( target == ServerStatus.STARTED )
                {
                    Module coreModule = moduleManager.getModule( 1 );
                    if( coreModule == null )
                    {
                        throw new IllegalStateException();
                    }

                    serviceManager.registerService( coreModule, Server.class, ServerImpl.this );
                    serviceManager.registerService( coreModule, ModuleManager.class, moduleManager );
                    serviceManager.registerService( coreModule, ServiceManager.class, serviceManager );
                }
            }
        } );

        // create the module watcher
        addListener( new ModuleWatcher( this.logger, this.lib ) );
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

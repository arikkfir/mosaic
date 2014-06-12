package org.mosaic.core.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.mosaic.core.Server;
import org.mosaic.core.impl.bytecode.BytecodeWeavingHook;
import org.mosaic.core.intercept.impl.MethodInterceptorsManager;
import org.mosaic.core.modules.Module;
import org.mosaic.core.modules.ModuleManager;
import org.mosaic.core.modules.impl.ModuleManagerEx;
import org.mosaic.core.modules.impl.ModuleManagerImpl;
import org.mosaic.core.modules.impl.ModuleWatcher;
import org.mosaic.core.services.ServiceManager;
import org.mosaic.core.services.impl.ServiceManagerImpl;
import org.mosaic.core.types.TypeResolver;
import org.mosaic.core.types.impl.TypeResolverImpl;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.version.Version;
import org.mosaic.core.util.workflow.Workflow;
import org.osgi.framework.BundleContext;

import static java.util.Objects.requireNonNull;
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
    private final TypeResolver typeResolver;

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
        //noinspection Anonymous2MethodRef
        new BytecodeWeavingHook( this, this.work.resolve( "weaving" ), bundleRevision -> {
            ModuleManagerEx moduleManager = Activator.getModuleManager();
            return moduleManager != null ? moduleManager.getModuleRevision( bundleRevision ) : null;
        } );

        // create the service manager
        this.serviceManager = new ServiceManagerImpl( this, this.logger, getLock() );

        // create method interceptors manager
        this.methodInterceptorsManager = new MethodInterceptorsManager( this, this.logger, this.getLock() );

        // create the type resolver
        this.typeResolver = new TypeResolverImpl( this.getLock() );

        // create the module manager
        this.moduleManager = new ModuleManagerImpl( this, this.logger, getLock(), this.serviceManager );

        // add a listener to register core services
        addAction( ServerStatus.STARTED, c -> {
            ServiceManager serviceManager = requireNonNull( Activator.getServiceManager() );
            Module coreModule = requireNonNull( Activator.getCoreModule() );
            serviceManager.registerService( coreModule, Server.class, ServerImpl.this );
            serviceManager.registerService( coreModule, ServiceManager.class, this.serviceManager );
            serviceManager.registerService( coreModule, TypeResolver.class, this.typeResolver );
            serviceManager.registerService( coreModule, ModuleManager.class, this.moduleManager );
        } );

        // create the module watcher
        new ModuleWatcher( this, this.logger, this.lib );
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
        return this.getLock().read( () -> this.version );
    }

    @Nonnull
    @Override
    public Path getHome()
    {
        return this.getLock().read( () -> this.home );
    }

    @Nonnull
    @Override
    public Path getApps()
    {
        return this.getLock().read( () -> this.apps );
    }

    @Nonnull
    @Override
    public Path getBin()
    {
        return this.getLock().read( () -> this.bin );
    }

    @Nonnull
    @Override
    public Path getEtc()
    {
        return this.getLock().read( () -> this.etc );
    }

    @Nonnull
    @Override
    public Path getLib()
    {
        return this.getLock().read( () -> this.lib );
    }

    @Nonnull
    @Override
    public Path getLogs()
    {
        return this.getLock().read( () -> this.logs );
    }

    @Nonnull
    @Override
    public Path getSchemas()
    {
        return this.getLock().read( () -> this.schemas );
    }

    @Nonnull
    @Override
    public Path getWork()
    {
        return this.getLock().read( () -> this.work );
    }

    @Nonnull
    ServiceManagerImpl getServiceManager()
    {
        return this.getLock().read( () -> this.serviceManager );
    }

    @Nonnull
    MethodInterceptorsManager getMethodInterceptorsManager()
    {
        return this.getLock().read( () -> this.methodInterceptorsManager );
    }

    @Nonnull
    TypeResolver getTypeResolver()
    {
        return this.getLock().read( () -> this.typeResolver );
    }

    @Nonnull
    ModuleManagerImpl getModuleManager()
    {
        return this.getLock().read( () -> this.moduleManager );
    }
}

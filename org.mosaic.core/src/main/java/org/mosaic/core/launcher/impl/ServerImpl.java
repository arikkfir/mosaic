package org.mosaic.core.launcher.impl;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.felix.framework.Felix;
import org.mosaic.core.Server;
import org.mosaic.core.modules.impl.ModuleManagerImpl;
import org.mosaic.core.modules.impl.ModuleWatcher;
import org.mosaic.core.services.impl.ServiceManagerImpl;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.version.Version;
import org.mosaic.core.weaving.MethodInterceptorsManager;
import org.mosaic.core.weaving.impl.BytecodeWeavingHook;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static java.lang.Thread.setDefaultUncaughtExceptionHandler;
import static java.nio.file.Files.*;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.felix.framework.cache.BundleCache.CACHE_BUFSIZE_PROP;
import static org.apache.felix.framework.util.FelixConstants.LOG_LEVEL_PROP;
import static org.apache.felix.framework.util.FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP;
import static org.mosaic.core.launcher.impl.SystemError.bootstrapError;
import static org.osgi.framework.Constants.*;

/**
 * @author arik
 */
public class ServerImpl implements Server, BundleActivator
{
    static
    {
        // connect SLF4J to java.util.logging
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // install an exception handler for all threads that don't have an exception handler, that simply logs the exception
        setDefaultUncaughtExceptionHandler( ( t, e ) -> {
            Logger logger = LoggerFactory.getLogger( Main.class );
            logger.error( Objects.toString( e.getMessage(), e.getClass().getName() + ": " + e.getMessage() ), e );
        } );
    }

    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger( ServerImpl.class );

    @Nonnull
    private static List<String> readLogo()
    {
        List<String> logoLines = new LinkedList<>();
        try
        {
            URL logoResource = ServerImpl.class.getResource( "logo.txt" );
            try( BufferedReader reader = new BufferedReader( new InputStreamReader( logoResource.openStream(), "UTF-8" ) ) )
            {
                String line;
                while( ( line = reader.readLine() ) != null )
                {
                    while( line.length() < 34 )
                    {
                        line += " ";
                    }
                    logoLines.add( line );
                }
            }
        }
        catch( IOException e )
        {
            throw bootstrapError( "Incomplete Mosaic installation - could not read from 'logo.txt' file.", e );
        }
        return logoLines;
    }

    @Nonnull
    private final ZonedDateTime startTime = now();

    @Nonnull
    private final ReadWriteLock lock;

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
    private final List<ServerHook> startupHooks = new LinkedList<>();

    @Nonnull
    private final List<ServerHook> shutdownHooks = new LinkedList<>();

    @Nonnull
    private final ShutdownManager shutdownManager = new ShutdownManager();

    @Nonnull
    private final ServerFrameworkListener serverFrameworkListener = new ServerFrameworkListener();

    @Nonnull
    private final Felix felix;

    @Nonnull
    private final BytecodeWeavingHook bytecodeWeavingHook;

    @Nonnull
    private final ServiceManagerImpl serviceManager;

    @Nonnull
    private final MethodInterceptorsManager methodInterceptorsManager;

    @Nonnull
    private final ModuleManagerImpl moduleManager;

    @Nonnull
    private final ModuleWatcher moduleWatcher;

    @Nullable
    private BundleContext bundleContext;

    public ServerImpl()
    {
        this( System.getProperties() );
    }

    public ServerImpl( @Nonnull Properties properties )
    {
        // global lock
        this.lock = new ReadWriteLock( "org.mosaic", 15, SECONDS );

        // initialize home directories
        this.home = Paths.get( properties.getProperty( "org.mosaic.home", System.getProperty( "user.dir" ) ) );
        this.apps = Paths.get( properties.getProperty( "org.mosaic.home.apps", this.home.resolve( "apps" ).toString() ) );
        this.bin = Paths.get( properties.getProperty( "org.mosaic.home.bin", this.home.resolve( "bin" ).toString() ) );
        this.etc = Paths.get( properties.getProperty( "org.mosaic.home.etc", this.home.resolve( "etc" ).toString() ) );
        this.lib = Paths.get( properties.getProperty( "org.mosaic.home.lib", this.home.resolve( "lib" ).toString() ) );
        this.logs = Paths.get( properties.getProperty( "org.mosaic.home.logs", this.home.resolve( "logs" ).toString() ) );
        this.schemas = Paths.get( properties.getProperty( "org.mosaic.home.schemas", this.home.resolve( "schemas" ).toString() ) );
        this.work = Paths.get( properties.getProperty( "org.mosaic.home.work", this.home.resolve( "work" ).toString() ) );
        this.version = readVersion();

        // create home directory structure (if not existing)
        createDirectories( this.home, this.apps, this.etc, this.lib, this.logs, this.work );

        // create Felix instance
        this.felix = createFelix();

        // server hooks
        addStartupHook( this::printStartupHeader );
        addStartupHook( this::updateBundles );

        // create bytecode weaver
        this.bytecodeWeavingHook = new BytecodeWeavingHook( this );

        // create the service manager
        this.serviceManager = new ServiceManagerImpl( this );

        // create method interceptors manager
        this.methodInterceptorsManager = new MethodInterceptorsManager( this );

        // create the module manager
        this.moduleManager = new ModuleManagerImpl( this );

        // create the module watcher
        this.moduleWatcher = new ModuleWatcher( this );

        // add a listener to register core services
        //noinspection CodeBlock2Expr
        addStartupHook( bundleContext -> {
            this.serviceManager.registerService( null, Server.class, this );
        } );
        // TODO: unregister server service
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
        return this.lock.read( () -> this.version );
    }

    @Nonnull
    @Override
    public Path getHome()
    {
        return this.lock.read( () -> this.home );
    }

    @Nonnull
    @Override
    public Path getApps()
    {
        return this.lock.read( () -> this.apps );
    }

    @Nonnull
    @Override
    public Path getBin()
    {
        return this.lock.read( () -> this.bin );
    }

    @Nonnull
    @Override
    public Path getEtc()
    {
        return this.lock.read( () -> this.etc );
    }

    @Nonnull
    @Override
    public Path getLib()
    {
        return this.lock.read( () -> this.lib );
    }

    @Nonnull
    @Override
    public Path getLogs()
    {
        return this.lock.read( () -> this.logs );
    }

    @Nonnull
    @Override
    public Path getSchemas()
    {
        return this.lock.read( () -> this.schemas );
    }

    @Nonnull
    @Override
    public Path getWork()
    {
        return this.lock.read( () -> this.work );
    }

    @Override
    public void start( BundleContext context ) throws Exception
    {
        this.lock.write( () -> {

            this.bundleContext = context;

            // add OSGi listeners that emit log statements on OSGi events
            context.addFrameworkListener( this.serverFrameworkListener );

            // call our startup hooks
            for( ServerHook hook : this.startupHooks )
            {
                try
                {
                    hook.execute( context );
                }
                catch( Throwable e )
                {
                    try
                    {
                        stop( context );
                    }
                    catch( Exception ignore )
                    {
                    }
                    throw bootstrapError( "Mosaic startup hook {} failed", hook, e );
                }
            }

            // install JVM shutdown hook
            this.shutdownManager.install();
        } );
    }

    @Override
    public void stop( BundleContext context )
    {
        LOG.info( "STOPPING MOSAIC (was up for {} seconds)", Duration.between( this.startTime, now() ) );
        this.lock.write( () -> {

            for( ServerHook hook : this.shutdownHooks )
            {
                try
                {
                    hook.execute( context );
                }
                catch( Throwable e )
                {
                    LOG.error( "Mosaic shutdown hook {} failed", hook, e );
                }
            }

            // uninstall JVM shutdown hook
            this.shutdownManager.uninstall();

            // remove our framework listener
            context.removeFrameworkListener( this.serverFrameworkListener );

            this.bundleContext = null;
        } );
    }

    public void start()
    {
        try
        {
            this.felix.start();
        }
        catch( Throwable e )
        {
            stop();
            throw bootstrapError( "could not start OSGi container", e );
        }
    }

    public void stop()
    {
        try
        {
            this.felix.stop();
            this.felix.waitForStop( 1000 * 60 * 5 );
        }
        catch( Throwable e )
        {
            LOG.error( "ERROR STOPPING MOSAIC: {}", e.getMessage(), e );
        }
    }

    @Nonnull
    public ReadWriteLock getLock()
    {
        return this.lock;
    }

    @Nullable
    public BundleContext getBundleContext()
    {
        return this.bundleContext;
    }

    public void addStartupHook( @Nonnull ServerHook task )
    {
        this.lock.write( () -> this.startupHooks.add( task ) );
    }

    public void addShutdownHook( @Nonnull ServerHook task )
    {
        this.lock.write( () -> this.shutdownHooks.add( 0, task ) );
    }

    @SuppressWarnings( "UnusedDeclaration" )
    @Nonnull
    public BytecodeWeavingHook getBytecodeWeavingHook()
    {
        return this.bytecodeWeavingHook;
    }

    @Nonnull
    public ServiceManagerImpl getServiceManager()
    {
        return this.serviceManager;
    }

    @SuppressWarnings( "UnusedDeclaration" )
    @Nonnull
    public MethodInterceptorsManager getMethodInterceptorsManager()
    {
        return methodInterceptorsManager;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Nonnull
    public ModuleManagerImpl getModuleManager()
    {
        return moduleManager;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Nonnull
    public ModuleWatcher getModuleWatcher()
    {
        return moduleWatcher;
    }

    @Nonnull
    private Version readVersion()
    {
        // FIXME: read actual version
        //noinspection ConstantConditions
        if( 1 == 1 )
        {
            return Version.valueOf( "1.0.0-SNAPSHOT" );
        }

        URL versionResource = getClass().getResource( "version" );
        if( versionResource == null )
        {
            throw bootstrapError( "could not find version file of Mosaic" );
        }
        try( BufferedReader reader = new BufferedReader( new InputStreamReader( versionResource.openStream(), "UTF-8" ) ) )
        {
            return Version.valueOf( reader.readLine() );
        }
        catch( Throwable e )
        {
            throw bootstrapError( "could not read version from: " + versionResource, e );
        }
    }

    private void createDirectories( @Nonnull Path... paths )
    {
        for( Path path : paths )
        {
            try
            {
                if( isSymbolicLink( path ) )
                {
                    createDirectories( readSymbolicLink( path ) );
                }
                else if( exists( path ) )
                {
                    if( isRegularFile( path ) )
                    {
                        throw bootstrapError( "path '{}' is a file and not a directory", path );
                    }
                }
                else
                {
                    Files.createDirectories( path );
                }
            }
            catch( SystemError.BootstrapException e )
            {
                throw e;
            }
            catch( Throwable e )
            {
                throw bootstrapError( "could not create directory at '{}'", path, e );
            }
        }
    }

    @Nonnull
    private Felix createFelix()
    {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put( FRAMEWORK_STORAGE, this.work.resolve( "felix" ).toString() );   // specify work location for felix
        properties.put( CACHE_BUFSIZE_PROP, ( 1024 * 64 ) + "" );                       // buffer size for reading from storage
        properties.put( LOG_LEVEL_PROP, "0" );                                          // disable Felix logging output (we'll only log OSGi events)
        properties.put( FRAMEWORK_BUNDLE_PARENT, FRAMEWORK_BUNDLE_PARENT_EXT );         // parent class-loader of all bundles
        properties.put( FRAMEWORK_BOOTDELEGATION, "sun.*" );                            // extra packages available via classloader delegation (ie. not "Import-Package" necessary)
        properties.put( SYSTEMBUNDLE_ACTIVATORS_PROP, asList( this ) );                 // system bundle activators (this)
        try
        {
            properties.put( FRAMEWORK_SYSTEMPACKAGES_EXTRA,
                            getExportedPackagesFrom( findLibraryPath( "slf4j-api" ) ) + "," +
                            getExportedPackagesFrom( findLibraryPath( "org.mosaic.api" ) ) );
        }
        catch( Throwable e )
        {
            throw bootstrapError( "could not calculate exported system packages", e );
        }

        return new Felix( properties );
    }

    @Nonnull
    private String getExportedPackagesFrom( @Nonnull Path path ) throws IOException
    {
        Manifest manifest;

        if( isRegularFile( path ) )
        {
            try( JarFile jarFile = new JarFile( path.toFile() ) )
            {
                manifest = jarFile.getManifest();
            }
        }
        else if( isDirectory( path ) )
        {
            Path manifestPath = path.resolve( "META-INF/MANIFEST.MF" );
            if( exists( manifestPath ) )
            {
                manifest = new Manifest();
                try( InputStream inputStream = newInputStream( manifestPath ) )
                {
                    manifest.read( inputStream );
                }
            }
            else
            {
                throw new IllegalArgumentException( "path '" + path + "' does not contain a manifest" );
            }
        }
        else
        {
            throw new IllegalArgumentException( "must be a jar or a directory" );
        }

        String exportPackageValue = manifest.getMainAttributes().getValue( "Export-Package" );
        if( exportPackageValue != null )
        {
            return exportPackageValue;
        }
        else
        {
            throw new IllegalArgumentException( "JAR file '" + path + "' does not contain the 'Export-Package' manifest header" );
        }
    }

    @Nonnull
    private Path findLibraryPath( @Nonnull String name )
    {
        ClassLoader classLoader = getClass().getClassLoader();
        if( classLoader instanceof URLClassLoader )
        {
            URLClassLoader urlClassLoader = ( URLClassLoader ) classLoader;
            for( URL url : urlClassLoader.getURLs() )
            {
                String path = url.getPath();
                if( path.endsWith( name + ".jar" ) || path.contains( "/" + name + "/" ) )
                {
                    return Paths.get( path );
                }
            }
        }
        else
        {
            for( String path : ManagementFactory.getRuntimeMXBean().getClassPath().split( File.pathSeparator ) )
            {
                if( path.endsWith( name + ".jar" ) || path.contains( "/" + name + "/" ) )
                {
                    return Paths.get( path );
                }
            }
        }
        throw new IllegalStateException( "could not find org.mosaic.core in classpath" );
    }

    private void printStartupHeader( @Nonnull BundleContext bundleContext )
    {
        // load logo & create header lines
        List<String> logoLines = readLogo();
        List<String> infoLines = asList(
                "Mosaic server version...." + this.version,
                "---------------------------------------------------------------------------------------",
                "Home....................." + this.home,
                "Apps....................." + this.apps,
                "Bin......................" + this.bin,
                "Etc......................" + this.etc,
                "Lib......................" + this.lib,
                "Logs....................." + this.logs,
                "Schemas.................." + this.schemas,
                "Work....................." + this.work
        );

        // empty logo line should be replaced by this
        String emptyLogoLine = "";
        for( int i = 0; i < 34; i++ )
        {
            emptyLogoLine += ' ';
        }

        // print while merging every logo+info line
        LOG.warn( "********************************************************************************************************************" );
        Iterator<String> logoIt = logoLines.iterator(), infoIt = infoLines.iterator();
        while( logoIt.hasNext() || infoIt.hasNext() )
        {
            LOG.warn( ( logoIt.hasNext() ? logoIt.next() : emptyLogoLine ) +
                      ( infoIt.hasNext() ? infoIt.next() : "" ) );
        }
        LOG.warn( "********************************************************************************************************************" );
    }

    private void updateBundles( @Nonnull BundleContext bundleContext )
    {
        for( Bundle bundle : bundleContext.getBundles() )
        {
            String location = bundle.getLocation();
            if( location.startsWith( "file:" ) )
            {
                Path path = Paths.get( location.substring( "file:".length() ) );
                if( notExists( path ) )
                {
                    try
                    {
                        bundle.uninstall();
                    }
                    catch( Throwable e )
                    {
                        throw bootstrapError( "could not uninstall cached bundle '{}@{}[{}]'", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId(), e );
                    }
                }
                else
                {
                    FileTime bundleFileModificationTime;
                    try
                    {
                        bundleFileModificationTime = getLastModifiedTime( path );
                    }
                    catch( Throwable e )
                    {
                        throw bootstrapError( "could not obtain modification time from '{}' for cached bundle '{}@{}[{}]'",
                                              path, bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId(), e );
                    }

                    if( bundleFileModificationTime.toMillis() > bundle.getLastModified() )
                    {
                        try
                        {
                            bundle.update();
                        }
                        catch( BundleException e )
                        {
                            throw bootstrapError( "could not update cached bundle '{}@{}[{}]'", bundle.getSymbolicName(), bundle.getVersion(), bundle.getBundleId(), e );
                        }
                    }
                }
            }
        }
    }

    public static interface ServerHook
    {
        void execute( @Nonnull BundleContext bundleContext ) throws Throwable;
    }

    private class ServerFrameworkListener implements FrameworkListener
    {
        @Override
        public void frameworkEvent( @Nonnull FrameworkEvent frameworkEvent )
        {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            Throwable throwable = frameworkEvent.getThrowable();

            switch( frameworkEvent.getType() )
            {
                case FrameworkEvent.ERROR:
                    LOG.error( throwable.getMessage(), throwable );
                    break;
            }
        }
    }

    private class ShutdownManager implements Runnable
    {
        @Nullable
        private Thread thread;

        @Override
        public void run()
        {
            ServerImpl.this.stop();
        }

        private void install()
        {
            ServerImpl.this.lock.write( () -> {
                this.thread = new Thread( this, "Mosaic-Shutdown" );
                Runtime.getRuntime().addShutdownHook( this.thread );
            } );
        }

        private void uninstall()
        {
            ServerImpl.this.lock.write( () -> {
                if( this.thread != null )
                {
                    Runtime.getRuntime().removeShutdownHook( this.thread );
                }
            } );
        }
    }
}

package org.mosaic.core.modules.impl;

import com.fasterxml.classmate.ResolvedType;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.mosaic.core.components.ClassEndpoint;
import org.mosaic.core.components.Component;
import org.mosaic.core.components.Components;
import org.mosaic.core.launcher.impl.ServerImpl;
import org.mosaic.core.modules.ModuleRevision;
import org.mosaic.core.modules.ModuleType;
import org.mosaic.core.modules.ServiceProperty;
import org.mosaic.core.services.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.version.Version;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.nio.file.FileSystems.newFileSystem;
import static java.nio.file.Files.*;

/**
 * @author arik
 * @todo think about error handling during 'revisionResolved', 'revisionStarting' & 'revisionStarted' - how to mark a revision as "failed"?
 */
class ModuleRevisionImpl implements ModuleRevision
{
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger( ModuleRevisionImpl.class );

    @Nonnull
    private static final Pattern REVISION_ID_PATTERN = Pattern.compile( "\\d+\\.(\\d+)" );

    @Nonnull
    private static Path findContentRoot( @Nonnull Path path )
    {
        if( isSymbolicLink( path ) )
        {
            try
            {
                Path linkTarget = readSymbolicLink( path );
                Path resolvedTarget = path.resolveSibling( linkTarget );
                Path target = resolvedTarget.toAbsolutePath().normalize();
                return findContentRoot( target );
            }
            catch( Exception e )
            {
                throw new IllegalStateException( "could not inspect " + path, e );
            }
        }
        else if( isDirectory( path ) )
        {
            return path;
        }
        else if( path.getFileName().toString().toLowerCase().endsWith( ".jar" ) )
        {
            try
            {
                URI uri = URI.create( "jar:file:" + path.toUri().getPath() );
                return newFileSystem( uri, Collections.<String, Object>emptyMap() ).getRootDirectories().iterator().next();
            }
            catch( Exception e )
            {
                throw new IllegalStateException( "could not inspect " + path, e );
            }
        }
        else
        {
            throw new IllegalStateException( "could not inspect " + path );
        }
    }

    @Nonnull
    private static String getClassName( Path path )
    {
        String className = path.toString().replace( '/', '.' );
        if( className.startsWith( "." ) )
        {
            className = className.substring( 1 );
        }
        if( className.toLowerCase().endsWith( ".class" ) )
        {
            className = className.substring( 0, className.length() - ".class".length() );
        }
        return className;
    }

    private static long getRevisionNumber( @Nonnull BundleRevision bundleRevision )
    {
        try
        {
            return Long.parseLong( bundleRevision.toString() );
        }
        catch( NumberFormatException e )
        {
            Matcher matcher = REVISION_ID_PATTERN.matcher( bundleRevision.toString() );
            if( matcher.matches() )
            {
                return Long.parseLong( matcher.group( 1 ) );
            }
            else
            {
                throw new IllegalStateException( "could not extract bundle revision number from: " + bundleRevision.toString() );
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private static Map<String, String> getHeaders( @Nonnull BundleRevision bundleRevision )
    {
        try
        {
            Method getHeadersMethod = bundleRevision.getClass().getMethod( "getHeaders" );
            getHeadersMethod.setAccessible( true );
            Object headersMap = getHeadersMethod.invoke( bundleRevision );
            if( headersMap instanceof Map )
            {
                return Collections.unmodifiableMap( ( Map ) headersMap );
            }
        }
        catch( Exception e )
        {
            throw new IllegalStateException( "could not extract headers from bundle revision", e );
        }
        throw new IllegalStateException( "could not extract headers from bundle revision" );
    }

    @Nonnull
    private final ServerImpl server;

    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final ModuleImpl module;

    @Nonnull
    private final BundleRevision bundleRevision;

    private final long id;

    @Nonnull
    private final String name;

    @Nonnull
    private final Version version;

    @Nonnull
    private final Map<String, String> headers;

    @Nullable
    private Path root;

    @Nullable
    private Set<ModuleRevisionImplDependency> dependencies;

    @Nullable
    private Set<ModuleRevisionImplDependency> satisfiedDependencies;

    @Nullable
    private Set<ModuleRevisionImplDependency> unsatisfiedDependencies;

    @Nullable
    private Set<ServiceRegistration<?>> staticServiceRegistrations;

    @Nullable
    private Set<ServiceRegistration<?>> liveServiceRegistrations;

    @Nullable
    private Set<ServiceListenerRegistration<?>> serviceListenerRegistrations;

    @Nullable
    private Map<Class<?>, ModuleTypeImpl> types;

    private boolean activated;

    @Nullable
    private Set<ModuleComponentImpl> components;

    ModuleRevisionImpl( @Nonnull ServerImpl server, @Nonnull ModuleImpl module, @Nonnull BundleRevision bundleRevision )
    {
        this.server = server;
        this.lock = this.server.getLock();
        this.module = module;
        this.bundleRevision = bundleRevision;
        this.id = getRevisionNumber( this.bundleRevision );
        this.name = this.bundleRevision.getSymbolicName();
        this.version = Version.valueOf( this.bundleRevision.getVersion().toString() );
        this.headers = getHeaders( this.bundleRevision );
    }

    @Override
    public String toString()
    {
        return format( "%s@%s[%d.%d]",
                       this.module.getBundle().getSymbolicName(),
                       this.module.getBundle().getVersion(),
                       this.module.getId(),
                       getId() );
    }

    @Nonnull
    @Override
    public ModuleImpl getModule()
    {
        return this.server.getLock().read( () -> this.module );
    }

    @Override
    public long getId()
    {
        return this.server.getLock().read( () -> this.id );
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.server.getLock().read( () -> this.name );
    }

    @Nonnull
    @Override
    public Version getVersion()
    {
        return this.server.getLock().read( () -> this.version );
    }

    @Nonnull
    @Override
    public Map<String, String> getHeaders()
    {
        return this.server.getLock().read( () -> this.headers );
    }

    @Override
    public boolean isCurrent()
    {
        return this.server.getLock().read( () -> this.module.getCurrentRevision() == this );
    }

    @Nullable
    @Override
    public ClassLoader getClassLoader()
    {
        return this.server.getLock().read( () -> {
            BundleWiring bundleWiring = this.bundleRevision.getWiring();
            return bundleWiring == null ? null : bundleWiring.getClassLoader();
        } );
    }

    @Nullable
    @Override
    public Path findResource( @Nonnull String glob ) throws IOException
    {
        return this.server.getLock().read( () -> {
            Path root = this.root;
            if( root != null )
            {
                PathMatcher pathMatcher = root.getFileSystem().getPathMatcher( glob );
                return Files.walk( root ).filter( pathMatcher::matches ).findFirst().orElse( null );
            }
            else
            {
                return null;
            }
        } );
    }

    @Nonnull
    @Override
    public Collection<Path> findResources( @Nonnull String glob ) throws IOException
    {
        return this.lock.read( () -> {
            Path root = this.root;
            if( root != null )
            {
                PathMatcher pathMatcher = root.getFileSystem().getPathMatcher( glob );
                return Files.walk( root ).filter( pathMatcher::matches ).collect( Collectors.toList() );
            }
            else
            {
                return Collections.<Path>emptyList();
            }
        } );
    }

    @Nullable
    @Override
    public ModuleType getType( @Nonnull Class<?> type )
    {
        return this.lock.read( () -> {
            Map<Class<?>, ModuleTypeImpl> types = this.types;
            return types != null ? types.get( type ) : null;
        } );
    }

    @Nonnull
    @Override
    public <ServiceType> ServiceRegistration<ServiceType> registerService( @Nonnull Class<ServiceType> type,
                                                                           @Nonnull ServiceType service,
                                                                           @Nonnull ServiceProperty... properties )
    {
        return this.lock.write( () -> {

            Set<ServiceRegistration<?>> registrations;
            if( ClassEndpoint.class.equals( type ) )
            {
                registrations = this.staticServiceRegistrations;
                if( registrations == null )
                {
                    registrations = new HashSet<>();
                    this.staticServiceRegistrations = registrations;
                }
            }
            else
            {
                registrations = this.liveServiceRegistrations;
                if( registrations == null )
                {
                    registrations = new HashSet<>();
                    this.liveServiceRegistrations = registrations;
                }
            }

            ServiceRegistration<ServiceType> registration = this.server.getServiceManager().registerService( this, type, service, properties );
            registrations.add( registration );

            return registration;

        } );
    }

    @Override
    public <ServiceType> ServiceListenerRegistration<ServiceType> addServiceListener( @Nonnull ServiceListener<ServiceType> listener,
                                                                                      @Nonnull Class<ServiceType> type,
                                                                                      @Nonnull ServiceProperty... properties )
    {
        return this.lock.write( () -> {

            Set<ServiceListenerRegistration<?>> registrations = this.serviceListenerRegistrations;
            if( registrations == null )
            {
                registrations = new HashSet<>();
                this.serviceListenerRegistrations = registrations;
            }

            ServiceListenerRegistration<ServiceType> registration = this.server.getServiceManager().addListener( this, listener, type, properties );
            registrations.add( registration );

            return registration;

        } );
    }

    @Override
    public <ServiceType> ServiceListenerRegistration<ServiceType> addServiceListener( @Nonnull ServiceRegistrationListener<ServiceType> onRegister,
                                                                                      @Nonnull ServiceUnregistrationListener<ServiceType> onUnregister,
                                                                                      @Nonnull Class<ServiceType> type,
                                                                                      @Nonnull ServiceProperty... properties )
    {
        return this.lock.write( () -> {

            Set<ServiceListenerRegistration<?>> registrations = this.serviceListenerRegistrations;
            if( registrations == null )
            {
                registrations = new HashSet<>();
                this.serviceListenerRegistrations = registrations;
            }

            ServiceListenerRegistration<ServiceType> registration = this.server.getServiceManager().addListener( this, onRegister, onUnregister, type, properties );
            registrations.add( registration );

            return registration;

        } );
    }

    @Override
    public <ServiceType> ServiceListenerRegistration<ServiceType> addWeakServiceListener( @Nonnull ServiceListener<ServiceType> listener,
                                                                                          @Nonnull Class<ServiceType> type,
                                                                                          @Nonnull ServiceProperty... properties )
    {
        return this.lock.write( () -> {

            Set<ServiceListenerRegistration<?>> registrations = this.serviceListenerRegistrations;
            if( registrations == null )
            {
                registrations = new HashSet<>();
                this.serviceListenerRegistrations = registrations;
            }

            ServiceListenerRegistration<ServiceType> registration = this.server.getServiceManager().addWeakListener( this, listener, type, properties );
            registrations.add( registration );

            return registration;

        } );
    }

    void revisionResolved()
    {
        try
        {
            this.lock.write( () -> {
                LOG.info( "RESOLVING {}", this );

                Path path = this.module.getPath();
                this.root = path == null ? null : findContentRoot( path );

                LOG.info( "RESOLVED {}", this );

                this.server.getModuleManager().notifyModuleRevisionListeners( listener -> listener.revisionResolved( this ) );
            } );
        }
        catch( Throwable e )
        {
            LOG.error( "Error during resolve process of module revision {}: {}", this, e.getMessage(), e );
        }
    }

    void revisionStarting()
    {
        try
        {
            this.lock.write( () -> {
                LOG.info( "STARTING {}", this );

                this.staticServiceRegistrations = new HashSet<>();
                this.liveServiceRegistrations = new HashSet<>();
                this.serviceListenerRegistrations = new HashSet<>();
                this.dependencies = new HashSet<>();
                this.satisfiedDependencies = new HashSet<>();
                this.unsatisfiedDependencies = new HashSet<>();
                this.types = scanTypes();
                this.components = scanComponents();

                Set<ModuleRevisionImplDependency> dependencies = this.dependencies;
                if( dependencies != null )
                {
                    for( ModuleRevisionImplDependency dependency : dependencies )
                    {
                        LOG.debug( "Initializing dependency {}", dependency );
                        dependency.initialize();
                    }
                }
            } );
        }
        catch( Throwable e )
        {
            LOG.error( "Error while starting module revision {}: {}", this, e.getMessage(), e );
        }
    }

    void revisionStarted()
    {
        try
        {
            this.lock.write( () -> {
                LOG.info( "STARTED {}", this );

                this.server.getModuleManager().notifyModuleRevisionListeners( listener -> listener.revisionStarted( this ) );
            } );
        }
        catch( Throwable e )
        {
            LOG.error( "Error while starting module revision {}: {}", this, e.getMessage(), e );
        }
    }

    void revisionStopping()
    {
        try
        {
            this.lock.write( () -> {
                LOG.info( "STOPPING {}", this );

                Set<ModuleRevisionImplDependency> dependencies = this.dependencies;
                if( dependencies != null )
                {
                    for( ModuleRevisionImplDependency dependency : dependencies )
                    {
                        LOG.debug( "Shutting down dependency {}", dependency );
                        dependency.shutdown();
                    }
                }
            } );
        }
        catch( Throwable e )
        {
            LOG.error( "Error while stopping module revision {}: {}", this, e.getMessage(), e );
        }
    }

    void revisionStopped()
    {
        try
        {
            this.lock.write( () -> {
                this.components = null;

                Map<Class<?>, ModuleTypeImpl> types = this.types;
                if( types != null )
                {
                    for( ModuleTypeImpl moduleType : types.values() )
                    {
                        moduleType.shutdown();
                    }
                    this.types = null;
                }

                this.dependencies = null;
                this.satisfiedDependencies = null;
                this.unsatisfiedDependencies = null;
                this.serviceListenerRegistrations = null;

                Set<ServiceRegistration<?>> staticServiceRegistrations = this.staticServiceRegistrations;
                if( staticServiceRegistrations != null )
                {
                    for( ServiceRegistration<?> registration : staticServiceRegistrations )
                    {
                        registration.unregister();
                    }
                    this.staticServiceRegistrations = null;
                }

                this.liveServiceRegistrations = null;

                LOG.info( "STOPPED {}", this );

                this.server.getModuleManager().notifyModuleRevisionListeners( listener -> listener.revisionStopped( this ) );
            } );
        }
        catch( Throwable e )
        {
            LOG.error( "Error while starting module revision {}: {}", this, e.getMessage(), e );
        }
    }

    void revisionUnresolved()
    {
        this.lock.write( () -> {
            LOG.info( "UNRESOLVING {}", this );

            Path root = this.root;
            if( root != null )
            {
                FileSystem fileSystem = root.getFileSystem();
                if( "jar".equals( fileSystem.provider().getScheme() ) )
                {
                    try
                    {
                        fileSystem.close();
                    }
                    catch( Exception e )
                    {
                        LOG.warn( "Could not close JAR file-system of '{}': {}", root, e.getMessage(), e );
                    }
                }
                this.root = null;
            }

            LOG.info( "UNRESOLVED {}", this );

            this.server.getModuleManager().notifyModuleRevisionListeners( listener -> listener.revisionUnresolved( this ) );
        } );
    }

    @Nonnull
    <ServiceType> ModuleRevisionImplServiceDependency<ServiceType> getServiceDependency( @Nonnull ResolvedType serviceType,
                                                                                         int minCount,
                                                                                         @Nonnull ServiceProperty... properties )
    {
        return getServiceDependency( new ServiceKey( serviceType, minCount, properties ) );
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    <ServiceType> ModuleRevisionImplServiceDependency<ServiceType> getServiceDependency( @Nonnull ServiceKey serviceKey )
    {
        return this.lock.read( () -> {
            Set<ModuleRevisionImplDependency> dependencies = this.dependencies;
            if( dependencies != null )
            {
                for( ModuleRevisionImplDependency dependency : dependencies )
                {
                    if( dependency instanceof ModuleRevisionImplServiceDependency )
                    {
                        ModuleRevisionImplServiceDependency<?> serviceDependency = ( ModuleRevisionImplServiceDependency ) dependency;
                        if( serviceDependency.getServiceKey().equals( serviceKey ) )
                        {
                            return ( ModuleRevisionImplServiceDependency<ServiceType> ) serviceDependency;
                        }
                    }
                }
            }

            return this.lock.write( () -> {
                Set<ModuleRevisionImplDependency> dependencies2 = this.dependencies;
                if( dependencies2 == null )
                {
                    dependencies2 = new HashSet<>();
                    this.dependencies = dependencies2;
                }

                ModuleRevisionImplServiceDependency<ServiceType> dependency = new ModuleRevisionImplServiceDependency<>( this.server, this, serviceKey );
                dependencies2.add( dependency );
                if( serviceKey.getMinCount() > 0 )
                {
                    LOG.debug( "Module revision {} depends on {} instances of {} with properties {}",
                               this,
                               serviceKey.getMinCount(),
                               serviceKey.getServiceType().getErasedType().getName(),
                               serviceKey.getServiceProperties() );
                }
                else
                {
                    LOG.debug( "Module revision {} wants instances of {} with properties {}",
                               this,
                               serviceKey.getServiceType().getErasedType().getName(),
                               serviceKey.getServiceProperties() );
                }
                return dependency;
            } );
        } );
    }

    void notifySatisfaction( @Nonnull ModuleRevisionImplDependency dependency, boolean satisfied )
    {
        this.lock.write( () -> {
            Set<ModuleRevisionImplDependency> unsatisfiedDependencies = this.unsatisfiedDependencies;
            Set<ModuleRevisionImplDependency> satisfiedDependencies = this.satisfiedDependencies;
            if( satisfied )
            {
                if( unsatisfiedDependencies != null && unsatisfiedDependencies.contains( dependency ) )
                {
                    unsatisfiedDependencies.remove( dependency );
                }
                if( satisfiedDependencies != null && !satisfiedDependencies.contains( dependency ) )
                {
                    LOG.info( "DEPENDENCY {} of {} is SATISFIED", dependency, this );
                    satisfiedDependencies.add( dependency );
                }
                activate();
            }
            else
            {
                if( unsatisfiedDependencies != null && !unsatisfiedDependencies.contains( dependency ) )
                {
                    LOG.info( "DEPENDENCY {} of {} is NOT SATISFIED", dependency, this );
                    unsatisfiedDependencies.add( dependency );
                }
                if( satisfiedDependencies != null && satisfiedDependencies.contains( dependency ) )
                {
                    satisfiedDependencies.remove( dependency );
                }
                if( !canActivate() )
                {
                    deactivate();
                }
            }
        } );
    }

    @Nonnull
    private Map<Class<?>, ModuleTypeImpl> scanTypes()
    {
        ClassLoader classLoader = getClassLoader();
        if( classLoader == null )
        {
            throw new IllegalStateException( "class-loader for bundle revision " + this + " is not available" );
        }

        if( this.module.getId() <= 1 )
        {
            return Collections.emptyMap();
        }

        Collection<Path> classResources;
        try
        {
            classResources = findResources( "glob:**/*.class" );
        }
        catch( IOException e )
        {
            LOG.error( "Could not inspect classes in module revision {}: {}", this, e.getMessage(), e );
            return Collections.emptyMap();
        }

        Map<Class<?>, ModuleTypeImpl> types = new HashMap<>();
        for( Path path : classResources )
        {
            String className = getClassName( path );
            Class<?> clazz;
            try
            {
                clazz = classLoader.loadClass( className );
            }
            catch( ClassNotFoundException e )
            {
                LOG.warn( "Class '{}' not found, although its resource WAS found at '{}'", className, path, e );
                continue;
            }
            types.put( clazz, new ModuleTypeImpl( this.server, this, clazz ) );
        }
        return types;
    }

    @Nonnull
    private Set<ModuleComponentImpl> scanComponents()
    {
        Map<Class<?>, ModuleTypeImpl> types = this.types;
        if( types != null )
        {
            Set<ModuleComponentImpl> components = new HashSet<>();
            for( ModuleTypeImpl moduleType : types.values() )
            {
                List<Component> annotations = null;

                Components componentsAnn = moduleType.getType().getAnnotation( Components.class );
                if( componentsAnn != null )
                {
                    Component[] componentAnns = componentsAnn.value();
                    if( componentAnns.length > 0 )
                    {
                        annotations = new LinkedList<>();
                        annotations.addAll( Arrays.asList( componentAnns ) );
                    }
                }

                Component annotation = moduleType.getType().getAnnotation( Component.class );
                if( annotation != null )
                {
                    if( annotations == null )
                    {
                        annotations = new LinkedList<>();
                    }
                    annotations.add( annotation );
                }
                if( annotations != null )
                {
                    components.add( new ModuleComponentImpl( this.server, moduleType, annotations ) );
                }
            }
            return components;
        }
        else
        {
            return Collections.emptySet();
        }
    }

    private boolean canActivate()
    {
        return this.module.getBundle().getState() == Bundle.ACTIVE &&
               ( this.unsatisfiedDependencies == null || this.unsatisfiedDependencies.isEmpty() );
    }

    boolean isActivated()
    {
        return this.lock.read( () -> this.activated );
    }

    void activate()
    {
        this.lock.write( () -> {
            if( this.activated || !canActivate() )
            {
                return;
            }

            LOG.info( "ACTIVATING {}", this );

            Map<Class<?>, ModuleTypeImpl> types = this.types;
            if( types != null )
            {
                for( ModuleTypeImpl type : types.values() )
                {
                    try
                    {
                        type.activate();
                    }
                    catch( Throwable e )
                    {
                        LOG.error( "Error activating type {}", type, e );
                        deactivate( true );
                        return;
                    }
                }
            }

            Set<ModuleComponentImpl> components = this.components;
            if( components != null )
            {
                // we have components - activate them
                for( ModuleComponentImpl component : components )
                {
                    try
                    {
                        component.activate();
                    }
                    catch( Throwable e )
                    {
                        LOG.error( "Error activating component {}", component, e );
                        deactivate( true );
                        return;
                    }
                }
            }
            this.activated = true;

            LOG.info( "ACTIVATED {}", this );

            this.server.getModuleManager().notifyModuleRevisionListeners( listener -> listener.revisionActivated( this ) );
        } );
    }

    void deactivate()
    {
        deactivate( false );
    }

    void deactivate( boolean force )
    {
        this.lock.write( () -> {
            if( force || this.activated )
            {
                LOG.info( "DEACTIVATING {}", this );

                Set<ModuleComponentImpl> components = this.components;
                if( components != null )
                {
                    // we have components - deactivate them
                    for( ModuleComponentImpl component : components )
                    {
                        component.deactivate();
                    }
                }

                Map<Class<?>, ModuleTypeImpl> types = this.types;
                if( types != null )
                {
                    for( ModuleTypeImpl type : types.values() )
                    {
                        type.deactivate();
                    }
                }

                Set<ServiceRegistration<?>> serviceRegistrations = this.liveServiceRegistrations;
                if( serviceRegistrations != null )
                {
                    for( ServiceRegistration<?> registration : serviceRegistrations )
                    {
                        registration.unregister();
                    }
                }

                Set<ServiceListenerRegistration<?>> serviceListenerRegistrations = this.serviceListenerRegistrations;
                if( serviceListenerRegistrations != null )
                {
                    for( ServiceListenerRegistration<?> registration : serviceListenerRegistrations )
                    {
                        registration.unregister();
                    }
                }

                this.activated = false;

                LOG.info( "DEACTIVATED {}", this );

                this.server.getModuleManager().notifyModuleRevisionListeners( listener -> listener.revisionDeactivated( this ) );
            }
        } );
    }
}

package org.mosaic.core.impl.module;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mosaic.core.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.version.Version;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;

import static java.nio.file.FileSystems.newFileSystem;
import static java.nio.file.Files.*;

/**
 * @author arik
 * @todo think about error handling during 'revisionResolved', 'revisionStarting' & 'revisionStarted' - how to mark a revision as "failed"?
 */
class ModuleRevisionImpl implements ModuleRevision
{
    @Nonnull
    private static final Pattern REVISION_ID_PATTERN = Pattern.compile( "\\d+\\.(\\d+)" );

    @Nonnull
    private static Path findRoot( @Nonnull Path path )
    {
        if( isSymbolicLink( path ) )
        {
            try
            {
                Path linkTarget = readSymbolicLink( path );
                Path resolvedTarget = path.resolveSibling( linkTarget );
                Path target = resolvedTarget.toAbsolutePath().normalize();
                return findRoot( target );
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

    @SuppressWarnings( "unchecked" )
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
    private Map<Class<?>, ModuleTypeImpl> types;

    private boolean activated;

    @Nullable
    private Set<ModuleComponentImpl> components;

    ModuleRevisionImpl( @Nonnull ModuleImpl module, @Nonnull BundleRevision bundleRevision )
    {
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
        return ToStringHelper.create( this )
                             .add( "module", this.module )
                             .add( "id", this.id )
                             .toString();
    }

    @Nonnull
    @Override
    public ModuleImpl getModule()
    {
        this.module.getLock().acquireReadLock();
        try
        {
            return this.module;
        }
        finally
        {
            this.module.getLock().releaseReadLock();
        }
    }

    @Override
    public long getId()
    {
        this.module.getLock().acquireReadLock();
        try
        {
            return this.id;
        }
        finally
        {
            this.module.getLock().releaseReadLock();
        }
    }

    @Nonnull
    @Override
    public String getName()
    {
        this.module.getLock().acquireReadLock();
        try
        {
            return this.name;
        }
        finally
        {
            this.module.getLock().releaseReadLock();
        }
    }

    @Nonnull
    @Override
    public Version getVersion()
    {
        this.module.getLock().acquireReadLock();
        try
        {
            return this.version;
        }
        finally
        {
            this.module.getLock().releaseReadLock();
        }
    }

    @Nonnull
    @Override
    public Map<String, String> getHeaders()
    {
        this.module.getLock().acquireReadLock();
        try
        {
            return this.headers;
        }
        finally
        {
            this.module.getLock().releaseReadLock();
        }
    }

    @Override
    public boolean isCurrent()
    {
        this.module.getLock().acquireReadLock();
        try
        {
            return this.module.getCurrentRevision() == this;
        }
        finally
        {
            this.module.getLock().releaseReadLock();
        }
    }

    @Nullable
    @Override
    public ClassLoader getClassLoader()
    {
        this.module.getLock().acquireReadLock();
        try
        {
            return this.bundleRevision.getWiring().getClassLoader();
        }
        finally
        {
            this.module.getLock().releaseReadLock();
        }
    }

    @Nullable
    @Override
    public Path findResource( @Nonnull String glob ) throws IOException
    {
        this.module.getLock().acquireReadLock();
        try
        {
            Path root = this.root;
            if( root == null )
            {
                return null;
            }

            final PathMatcher pathMatcher = root.getFileSystem().getPathMatcher( glob );
            final AtomicReference<Path> match = new AtomicReference<>();
            Files.walkFileTree( root, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException
                {
                    if( pathMatcher.matches( file ) )
                    {
                        match.set( file );
                        return FileVisitResult.TERMINATE;
                    }
                    else
                    {
                        return FileVisitResult.CONTINUE;
                    }
                }
            } );
            return match.get();
        }
        finally
        {
            this.module.getLock().releaseReadLock();
        }
    }

    @Nonnull
    @Override
    public Collection<Path> findResources( @Nonnull String glob ) throws IOException
    {
        this.module.getLock().acquireReadLock();
        try
        {
            Path root = this.root;
            if( root == null )
            {
                return Collections.emptyList();
            }

            final PathMatcher pathMatcher = root.getFileSystem().getPathMatcher( glob );
            final Collection<Path> matches = new LinkedList<>();
            Files.walkFileTree( root, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException
                {
                    if( pathMatcher.matches( file ) )
                    {
                        matches.add( file );
                    }
                    return FileVisitResult.CONTINUE;
                }
            } );
            return matches;
        }
        finally
        {
            this.module.getLock().releaseReadLock();
        }
    }

    @Nullable
    @Override
    public ModuleType getType( @Nonnull Class<?> type )
    {
        this.module.getLock().acquireReadLock();
        try
        {
            Map<Class<?>, ModuleTypeImpl> types = this.types;
            if( types != null )
            {
                return types.get( type );
            }
            return null;
        }
        finally
        {
            this.module.getLock().releaseReadLock();
        }
    }

    void revisionResolved()
    {
        this.module.getLock().acquireWriteLock();
        try
        {
            this.module.getLogger().info( "RESOLVING {}", this );

            Path path = this.module.getPath();
            if( path == null )
            {
                this.root = null;
            }
            else
            {
                this.root = findRoot( path );
            }

            this.module.getLogger().info( "RESOLVED {}", this );
        }
        catch( Throwable e )
        {
            this.module.getLogger().error( "Error during resolve process of module revision {}: {}", this, e.getMessage(), e );
        }
        finally
        {
            this.module.getLock().releaseWriteLock();
        }
    }

    void revisionStarting()
    {
        this.module.getLock().acquireWriteLock();
        try
        {
            this.module.getLogger().info( "STARTING {}", this );

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
                    this.module.getLogger().debug( "Initializing dependency {}", dependency );
                    dependency.initialize();
                }
            }
        }
        catch( Throwable e )
        {
            this.module.getLogger().error( "Error while starting module revision {}: {}", this, e.getMessage(), e );
        }
        finally
        {
            this.module.getLock().releaseWriteLock();
        }
    }

    void revisionStarted()
    {
        this.module.getLock().acquireWriteLock();
        try
        {
            this.module.getLogger().info( "STARTED {}", this );
        }
        catch( Throwable e )
        {
            this.module.getLogger().error( "Error while starting module revision {}: {}", this, e.getMessage(), e );
        }
        finally
        {
            this.module.getLock().releaseWriteLock();
        }
    }

    void revisionStopping()
    {
        this.module.getLock().acquireWriteLock();
        try
        {
            this.module.getLogger().info( "STOPPING {}", this );

            Set<ModuleRevisionImplDependency> dependencies = this.dependencies;
            if( dependencies != null )
            {
                for( ModuleRevisionImplDependency dependency : dependencies )
                {
                    this.module.getLogger().debug( "Shutting down dependency {}", dependency );
                    dependency.shutdown();
                }
            }
        }
        catch( Throwable e )
        {
            this.module.getLogger().error( "Error while stopping module revision {}: {}", this, e.getMessage(), e );
        }
        finally
        {
            this.module.getLock().releaseWriteLock();
        }
    }

    void revisionStopped()
    {
        this.module.getLock().acquireWriteLock();
        try
        {
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

            this.module.getLogger().info( "STOPPED {}", this );
        }
        catch( Throwable e )
        {
            this.module.getLogger().error( "Error while starting module revision {}: {}", this, e.getMessage(), e );
        }
        finally
        {
            this.module.getLock().releaseWriteLock();
        }
    }

    void revisionUnresolved()
    {
        this.module.getLock().acquireWriteLock();
        try
        {
            this.module.getLogger().info( "UNRESOLVING {}", this );

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
                        this.module.getLogger().warn( "Could not close JAR file-system of '{}': {}", root, e.getMessage(), e );
                    }
                }
                this.root = null;
            }

            this.module.getLogger().info( "UNRESOLVED {}", this );
        }
        finally
        {
            this.module.getLock().releaseWriteLock();
        }
    }

    @Nonnull
    <ServiceType> ModuleRevisionImplServiceDependency<ServiceType> getServiceDependency( @Nonnull Class<ServiceType> serviceType,
                                                                                         int minCount,
                                                                                         @Nonnull Module.ServiceProperty... properties )
    {
        return getServiceDependency( new ServiceKey<>( serviceType, minCount, properties ) );
    }

    @SuppressWarnings( "unchecked" )
    @Nonnull
    <ServiceType> ModuleRevisionImplServiceDependency<ServiceType> getServiceDependency( @Nonnull ServiceKey<ServiceType> serviceKey )
    {
        this.module.getLock().acquireReadLock();
        try
        {
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

            this.module.getLock().releaseReadLock();
            this.module.getLock().acquireWriteLock();
            try
            {
                dependencies = this.dependencies;
                if( dependencies == null )
                {
                    dependencies = new HashSet<>();
                    this.dependencies = dependencies;
                }

                ModuleRevisionImplServiceDependency<ServiceType> dependency = new ModuleRevisionImplServiceDependency<>( this, serviceKey );
                dependencies.add( dependency );
                this.module.getLogger().debug( "Added dependency {} to module revision {}", dependency, this );

                return dependency;
            }
            finally
            {
                this.module.getLock().releaseWriteLock();
                this.module.getLock().acquireReadLock();
            }
        }
        finally
        {
            this.module.getLock().releaseReadLock();
        }

    }

    void notifySatisfaction( @Nonnull ModuleRevisionImplDependency dependency, boolean satisfied )
    {
        this.module.getLock().acquireWriteLock();
        try
        {
            this.module.getLogger().info( "DEPENDENCY {} of {} is {}", dependency, this, satisfied ? "SATISFIED" : "NOT SATISFIED" );

            Set<ModuleRevisionImplDependency> unsatisfiedDependencies = this.unsatisfiedDependencies;
            Set<ModuleRevisionImplDependency> satisfiedDependencies = this.satisfiedDependencies;
            if( satisfied )
            {
                if( unsatisfiedDependencies != null )
                {
                    unsatisfiedDependencies.remove( dependency );
                }
                if( satisfiedDependencies != null )
                {
                    satisfiedDependencies.add( dependency );
                }
                activate();
            }
            else
            {
                if( unsatisfiedDependencies != null )
                {
                    unsatisfiedDependencies.add( dependency );
                }
                if( satisfiedDependencies != null )
                {
                    satisfiedDependencies.remove( dependency );
                }
                if( !canActivate() )
                {
                    deactivate();
                }
            }
        }
        finally
        {
            this.module.getLock().releaseWriteLock();
        }
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
            this.module.getLogger().error( "Could not inspect classes in module revision {}: {}", this, e.getMessage(), e );
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
                this.module.getLogger().warn( "Class '{}' not found, although its resource WAS found at '{}'", className, path, e );
                continue;
            }
            types.put( clazz, new ModuleTypeImpl( this, clazz ) );
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
                    components.add( new ModuleComponentImpl( moduleType, annotations ) );
                }
            }
            return components;
        }
        else
        {
            return Collections.emptySet();
        }
    }

    @SuppressWarnings( { "SimplifiableIfStatement", "RedundantIfStatement" } )
    private boolean canActivate()
    {
        if( this.module.getBundle().getState() != Bundle.ACTIVE )
        {
            return false;
        }

        Set<ModuleRevisionImplDependency> unsatisfiedDependencies = this.unsatisfiedDependencies;
        if( unsatisfiedDependencies == null )
        {
            return false;
        }

        if( !unsatisfiedDependencies.isEmpty() )
        {
            return false;
        }

        return true;
    }

    boolean isActivated()
    {
        return this.activated;
    }

    void activate()
    {
        if( this.activated || !canActivate() )
        {
            return;
        }

        this.module.getLogger().info( "ACTIVATING {}", this );

        Set<ModuleComponentImpl> components = this.components;
        if( components != null )
        {
            // we have components - activate them
            for( ModuleComponentImpl component : components )
            {
                component.activate();
            }
        }
        this.activated = true;

        this.module.getLogger().info( "ACTIVATED {}", this );
    }

    void deactivate()
    {
        if( this.activated )
        {
            this.module.getLogger().info( "DEACTIVATING {}", this );

            Set<ModuleComponentImpl> components = this.components;
            if( components != null )
            {
                // we have components - deactivate them
                for( ModuleComponentImpl component : components )
                {
                    component.deactivate();
                }
            }
            this.activated = false;

            this.module.getLogger().info( "DEACTIVATED {}", this );
        }
    }
}

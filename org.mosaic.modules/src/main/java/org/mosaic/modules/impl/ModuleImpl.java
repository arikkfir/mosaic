package org.mosaic.modules.impl;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.joda.time.DateTime;
import org.mosaic.modules.*;
import org.mosaic.modules.ServiceReference;
import org.mosaic.modules.ServiceRegistration;
import org.mosaic.modules.TypeDescriptor;
import org.mosaic.modules.spi.ModuleActivator;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.LinkedHashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.collections.UnmodifiableMapEx;
import org.mosaic.util.osgi.FilterBuilder;
import org.mosaic.util.version.Version;
import org.osgi.framework.*;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Iterators.toArray;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * @author arik
 */
final class ModuleImpl extends Lifecycle implements Module
{
    private static final Logger LOG = LoggerFactory.getLogger( ModuleImpl.class );

    private static final Logger TYPES_LOG = LoggerFactory.getLogger( Module.class.getName() + ".components" );

    private static final Logger CLASS_LOAD_ERRORS_LOG = LoggerFactory.getLogger( Module.class.getName() + ".classloading" );

    private static final Logger MODULE_INSTALL_LOG = LoggerFactory.getLogger( Module.class.getName() + ".installed" );

    private static final Logger MODULE_RESOLVE_LOG = LoggerFactory.getLogger( Module.class.getName() + ".resolved" );

    private static final Logger MODULE_STARTING_LOG = LoggerFactory.getLogger( Module.class.getName() + ".starting" );

    private static final Logger MODULE_STARTED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".started" );

    private static final Logger MODULE_ACTIVATING_LOG = LoggerFactory.getLogger( Module.class.getName() + ".activating" );

    private static final Logger MODULE_ACTIVATED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".activated" );

    private static final Logger MODULE_DEACTIVATING_LOG = LoggerFactory.getLogger( Module.class.getName() + ".deactivating" );

    private static final Logger MODULE_DEACTIVATED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".deactivated" );

    private static final Logger MODULE_STOPPING_LOG = LoggerFactory.getLogger( Module.class.getName() + ".stopping" );

    private static final Logger MODULE_STOPPED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".stopped" );

    private static final Logger MODULE_UPDATED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".updated" );

    private static final Logger MODULE_UNRESOLVED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".unresolved" );

    private static final Logger MODULE_UNINSTALLED_LOG = LoggerFactory.getLogger( Module.class.getName() + ".uninstalled" );

    public static class ComponentDependency
    {
    }

    @Nonnull
    private final LoadingCache<String, List<Path>> resourceCache;

    @Nonnull
    private final ModuleManagerImpl moduleManager;

    @Nonnull
    private final Bundle bundle;

    @Nonnull
    private Map<Class<?>, org.mosaic.modules.impl.TypeDescriptor> types = Collections.emptyMap();

    @Nullable
    private String moduleActivatorClassName;

    @Nullable
    private ModuleActivator activator;

    ModuleImpl( @Nonnull ModuleManagerImpl moduleManager, @Nonnull Bundle bundle )
    {
        this.moduleManager = moduleManager;
        this.bundle = bundle;
        this.resourceCache = CacheBuilder.newBuilder()
                                         .concurrencyLevel( 10 )
                                         .initialCapacity( 1000 )
                                         .build( new CacheLoader<String, List<Path>>()
                                         {
                                             @Override
                                             public List<Path> load( @Nonnull String key ) throws Exception
                                             {
                                                 if( !key.startsWith( "/" ) )
                                                 {
                                                     key = "/" + key;
                                                 }
                                                 List<Path> paths = null;

                                                 Bundle bundle = ModuleImpl.this.bundle;

                                                 Enumeration<URL> entries = bundle.findEntries( "/", "*", true );
                                                 if( entries != null )
                                                 {
                                                     Path root = Paths.get( URI.create( "module://" + bundle.getBundleId() + "/" ) );
                                                     while( entries.hasMoreElements() )
                                                     {
                                                         URL entry = entries.nextElement();
                                                         String entryPath = entry.getPath();
                                                         if( Activator.getPathMatcher().matches( key, entryPath ) )
                                                         {
                                                             Path path = root.resolve( entryPath );

                                                             if( paths == null )
                                                             {
                                                                 paths = new LinkedList<>();
                                                             }
                                                             paths.add( path );
                                                         }
                                                     }
                                                 }
                                                 return paths == null ? Collections.<Path>emptyList() : paths;
                                             }
                                         } );
    }

    @Override
    public long getId()
    {
        return this.bundle.getBundleId();
    }

    @Nonnull
    @Override
    public String getName()
    {
        return this.bundle.getSymbolicName();
    }

    @Nonnull
    @Override
    public Version getVersion()
    {
        return new Version( this.bundle.getVersion().toString() );
    }

    @Nonnull
    @Override
    public Path getPath()
    {
        return Paths.get( URI.create( "module://" + this.bundle.getBundleId() + "/" ) );
    }

    @Nonnull
    @Override
    public Path getLocation()
    {
        return Paths.get( this.bundle.getLocation() );
    }

    @Nonnull
    @Override
    public MapEx<String, String> getHeaders()
    {
        LinkedHashMapEx<String, String> ex = new LinkedHashMapEx<>();

        Dictionary<String, String> headers = this.bundle.getHeaders();
        Enumeration<String> keys = headers.keys();
        while( keys.hasMoreElements() )
        {
            String headerName = keys.nextElement();
            ex.put( headerName, headers.get( headerName ) );
        }

        return ex;
    }

    @Nonnull
    @Override
    public DateTime getLastModified()
    {
        return new DateTime( this.bundle.getLastModified() );
    }

    @Nonnull
    @Override
    public ModuleState getState()
    {
        switch( this.bundle.getState() )
        {
            case Bundle.INSTALLED:
                return ModuleState.INSTALLED;
            case Bundle.RESOLVED:
                return ModuleState.RESOLVED;
            case Bundle.STARTING:
                return ModuleState.STARTING;
            case Bundle.ACTIVE:
                return isActivated() ? ModuleState.ACTIVE : ModuleState.STARTED;
            case Bundle.STOPPING:
                return isActivated() ? ModuleState.ACTIVE : ModuleState.STOPPING;
            case Bundle.UNINSTALLED:
                return ModuleState.UNINSTALLED;
            default:
                throw new IllegalStateException( "unknown OSGi bundle state: " + this.bundle.getState() );
        }
    }

    @Nonnull
    @Override
    public Optional<Path> findResource( @Nonnull String glob ) throws IOException
    {
        try
        {
            List<Path> paths = this.resourceCache.get( glob );
            return paths.isEmpty() ? Optional.<Path>absent() : Optional.of( paths.get( 0 ) );
        }
        catch( ExecutionException | UncheckedExecutionException e )
        {
            throw new IOException( "could not locate resource '" + glob + "': " + e.getMessage(), e );
        }
    }

    @Nonnull
    @Override
    public Collection<Path> findResources( @Nonnull String glob ) throws IOException
    {
        try
        {
            return this.resourceCache.get( glob );
        }
        catch( ExecutionException | UncheckedExecutionException e )
        {
            throw new IOException( "could not locate resource '" + glob + "': " + e.getMessage(), e );
        }
    }

    @Nonnull
    @Override
    public ClassLoader getClassLoader()
    {
        BundleWiring bundleWiring = requireResolvedBundle().adapt( BundleWiring.class );
        if( bundleWiring == null )
        {
            throw new IllegalStateException( "bundle for module " + this + " has no bundle wiring" );
        }

        ClassLoader classLoader = bundleWiring.getClassLoader();
        if( classLoader == null )
        {
            throw new IllegalStateException( "bundle for module " + this + " has no class loader" );
        }

        return classLoader;
    }

    @Nullable
    @Override
    public TypeDescriptor getTypeDescriptor( @Nonnull Class<?> type )
    {
        return this.types.get( type );
    }

    @Nonnull
    @Override
    public Collection<Module.PackageRequirement> getPackageRequirements()
    {
        // if our bundle is not resolved, there can be no package requirements
        int bundleState = this.bundle.getState();
        if( bundleState == Bundle.INSTALLED || bundleState == Bundle.UNINSTALLED )
        {
            return Collections.emptyList();
        }

        // discovered package requirements will be stored in these maps and later aggregated into PackageRequirement instances
        Set<String> packageNames = new LinkedHashSet<>( 100 );
        Map<String, String> packageFilters = new HashMap<>( 100 );
        Map<String, String> packageResolutions = new HashMap<>( 100 );
        Map<String, Version> packageVersions = new HashMap<>( 100 );
        Map<String, ModuleImpl> packageProviders = new HashMap<>();

        // obtain current bundle revision; we need this to discover DECLARED requirements
        BundleRevision revision = this.bundle.adapt( BundleRevision.class );
        if( revision != null )
        {
            for( BundleRequirement requirement : revision.getDeclaredRequirements( PackageNamespace.PACKAGE_NAMESPACE ) )
            {
                String packageName = getPackageNameFromRequirement( requirement );
                if( packageName != null )
                {
                    packageNames.add( packageName );

                    Object filterDirective = requirement.getDirectives().get( PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE );
                    if( filterDirective != null )
                    {
                        packageFilters.put( packageName, filterDirective.toString() );
                    }

                    Object resolutionDirective = requirement.getDirectives().get( PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE );
                    if( resolutionDirective != null )
                    {
                        packageResolutions.put( packageName, resolutionDirective.toString() );
                    }
                }
            }
        }

        // obtain current bundle revision; we need this to discover WIRED requirements
        BundleWiring wiring = bundle.adapt( BundleWiring.class );
        if( wiring != null )
        {
            List<BundleWire> requiredWires = wiring.getRequiredWires( PackageNamespace.PACKAGE_NAMESPACE );
            if( requiredWires != null )
            {
                for( BundleWire wire : requiredWires )
                {
                    BundleCapability capability = wire.getCapability();
                    BundleRequirement requirement = wire.getRequirement();

                    String packageName = capability.getAttributes().get( PackageNamespace.PACKAGE_NAMESPACE ).toString();
                    if( !packageNames.contains( packageName ) )
                    {
                        packageNames.add( packageName );

                        Object filterDirective = requirement.getDirectives().get( PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE );
                        if( filterDirective != null )
                        {
                            packageFilters.put( packageName, filterDirective.toString() );
                        }

                        Object resolutionDirective = requirement.getDirectives().get( PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE );
                        if( resolutionDirective != null )
                        {
                            packageResolutions.put( packageName, resolutionDirective.toString() );
                        }
                    }

                    packageVersions.put( packageName, new Version( capability.getAttributes().get( PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE ).toString() ) );
                    packageProviders.put( packageName, this.moduleManager.getModuleFor( wire.getProvider().getBundle() ).get() );
                }
            }
        }

        // merge maps into package requirements list
        List<Module.PackageRequirement> requirements = new LinkedList<>();
        for( String packageName : packageNames )
        {
            requirements.add(
                    new PackageRequirementImpl(
                            packageName,
                            packageFilters.get( packageName ),
                            packageResolutions.get( packageName ),
                            packageProviders.get( packageName ),
                            packageVersions.get( packageName ) )
            );
        }
        return unmodifiableList( requirements );
    }

    @Nonnull
    @Override
    public Collection<Module.PackageCapability> getPackageCapabilities()
    {
        // if our bundle is not resolved, there can be no package requirements
        int bundleState = this.bundle.getState();
        if( bundleState == Bundle.INSTALLED || bundleState == Bundle.UNINSTALLED )
        {
            return Collections.emptyList();
        }

        // discovered package capabilities will be stored in these maps and later aggregated into PackageCapability instances
        Set<String> packageNames = new LinkedHashSet<>( 100 );
        Map<String, Version> packageVersions = new HashMap<>( 100 );
        Map<String, Set<Module>> packageConsumers = new HashMap<>();

        // obtain current bundle revision; we need this to discover DECLARED capabilities
        BundleRevision revision = this.bundle.adapt( BundleRevision.class );
        if( revision != null )
        {
            for( BundleCapability capability : revision.getDeclaredCapabilities( PackageNamespace.PACKAGE_NAMESPACE ) )
            {
                String packageName = capability.getAttributes().get( PackageNamespace.PACKAGE_NAMESPACE ).toString();
                packageNames.add( packageName );

                Object versionAttribute = capability.getAttributes().get( PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE );
                if( versionAttribute != null )
                {
                    packageVersions.put( packageName, new Version( versionAttribute.toString() ) );
                }
            }
        }

        // obtain current bundle revision; we need this to discover WIRED capabilities
        BundleWiring wiring = bundle.adapt( BundleWiring.class );
        if( wiring != null )
        {
            List<BundleWire> providedWires = wiring.getProvidedWires( PackageNamespace.PACKAGE_NAMESPACE );
            if( providedWires != null )
            {
                for( BundleWire wire : providedWires )
                {
                    BundleCapability capability = wire.getCapability();

                    String packageName = capability.getAttributes().get( PackageNamespace.PACKAGE_NAMESPACE ).toString();
                    if( !packageNames.contains( packageName ) )
                    {
                        packageNames.add( packageName );

                        Object versionAttribute = capability.getAttributes().get( PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE );
                        if( versionAttribute != null )
                        {
                            packageVersions.put( packageName, new Version( versionAttribute.toString() ) );
                        }
                    }

                    Set<Module> consumers = packageConsumers.get( packageName );
                    if( consumers == null )
                    {
                        consumers = new LinkedHashSet<>();
                        packageConsumers.put( packageName, consumers );
                    }
                    consumers.add( this.moduleManager.getModuleFor( wire.getRequirer().getBundle() ).get() );
                }
            }
        }

        // merge maps into package requirements list
        List<Module.PackageCapability> capabilities = new LinkedList<>();
        for( String packageName : packageNames )
        {
            Set<Module> consumers = packageConsumers.get( packageName );
            capabilities.add(
                    new PackageCapabilityImpl(
                            packageName,
                            packageVersions.get( packageName ),
                            consumers == null ? Collections.<Module>emptyList() : consumers )
            );
        }
        return unmodifiableList( capabilities );
    }

    @Nonnull
    @Override
    public Collection<Module.ServiceRequirement> getServiceRequirements()
    {
        return getChildren( Module.ServiceRequirement.class, true );
    }

    @Nonnull
    @Override
    public Collection<Module.ServiceCapability> getServiceCapabilities()
    {
        List<Module.ServiceCapability> serviceCapabilities = new LinkedList<>();
        for( ServiceCapabilityProvider serviceCapabilityProvider : getChildren( ServiceCapabilityProvider.class, true ) )
        {
            serviceCapabilities.addAll( serviceCapabilityProvider.getServiceCapabilities() );
        }
        return serviceCapabilities;
    }

    @Nullable
    @Override
    public <T> org.mosaic.modules.ServiceReference<T> findService( @Nonnull Class<T> type,
                                                                   @Nonnull Property... properties )
    {
        BundleContext bundleContext = this.bundle.getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "module '" + this + "' not active" );
        }

        FilterBuilder filterBuilder = null;
        for( Property property : properties )
        {
            if( filterBuilder == null )
            {
                filterBuilder = new FilterBuilder();
            }
            filterBuilder.addEquals( property.getKey(), Objects.toString( property.getValue(), "null" ) );
        }

        org.osgi.framework.ServiceReference<T> ref = null;
        if( filterBuilder != null )
        {
            try
            {
                Collection<org.osgi.framework.ServiceReference<T>> references = bundleContext.getServiceReferences( type, filterBuilder.toString() );
                if( references != null && !references.isEmpty() )
                {
                    ref = references.iterator().next();
                }
            }
            catch( InvalidSyntaxException e )
            {
                throw new IllegalArgumentException( "could not build service filter '" + filterBuilder + "': " + e.getMessage(), e );
            }
        }
        else
        {
            ref = bundleContext.getServiceReference( type );
        }

        if( ref == null )
        {
            return null;
        }
        else
        {
            return new ServiceReferenceImpl<>( ref, type );
        }
    }

    @Nonnull
    @Override
    public <T> ServiceRegistration<T> register( @Nonnull Class<T> type,
                                                @Nonnull T service,
                                                @Nonnull Property... properties )
    {
        if( getState() != ModuleState.ACTIVE )
        {
            throw new IllegalStateException( "module " + this + " is not active" );
        }

        BundleContext bundleContext = this.bundle.getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "module " + this + " has no bundle context" );
        }

        Dictionary<String, Object> dict = new Hashtable<>();
        for( Property property : properties )
        {
            dict.put( property.getKey(), property.getValue() );
        }
        org.osgi.framework.ServiceRegistration<T> registration = bundleContext.registerService( type, service, dict );
        return new ServiceRegistrationImpl<>( registration, type );
    }

    @Override
    public void startModule() throws ModuleStartException
    {
        try
        {
            this.bundle.start();
        }
        catch( Exception e )
        {
            throw new ModuleStartException( e, this );
        }
    }

    @Override
    public void stopModule() throws ModuleStopException
    {
        try
        {
            this.bundle.stop();
        }
        catch( Exception e )
        {
            throw new ModuleStopException( e, this );
        }
    }

    @Override
    public String toString()
    {
        return getName() + "@" + getVersion() + "[" + getId() + "]";
    }

    @Nonnull
    Bundle getBundle()
    {
        return this.bundle;
    }

    @Override
    protected synchronized void onBeforeStart()
    {
        MODULE_STARTING_LOG.info( "STARTING {}", this );
        postModuleEvent( ModuleEventType.STARTING );

        // create a map of all components in the module
        // then, create a cache of component descriptors for component type (including non-concrete keys)
        // then, create a directed graph mapping dependencies between loaded components
        Map<Class<?>, org.mosaic.modules.impl.TypeDescriptor> types = createComponentsMap();
        SimpleDirectedGraph<org.mosaic.modules.impl.TypeDescriptor, ComponentDependency> componentsGraph = createComponentsGraph( types );

        // find the module activator, if any
        String moduleActivatorClassName = this.bundle.getHeaders().get( "Module-Activator" );

        // clear old components
        clearChildren();

        // iterate descriptors, starting from dependent descriptors, then the requiring descriptors
        // ensures that if A uses B, we first create/activate B and only then A
        List<Lifecycle> children = asList( toArray( new TopologicalOrderIterator<>( componentsGraph ), Lifecycle.class ) );
        if( !children.isEmpty() )
        {
            // we want dependANT first (eg. a depends on b, then we should add b and then a)
            Collections.reverse( children );

            TYPES_LOG.debug( "Components for {}:\n    -> {}", this, Joiner.on( "\n    -> " ).join( children ) );
            for( Lifecycle child : children )
            {
                addChild( child );
            }
        }

        // save
        this.types = types;
        this.moduleActivatorClassName = moduleActivatorClassName;
    }

    @Override
    protected synchronized void onAfterStart()
    {
        MODULE_STARTED_LOG.info( "STARTED {}", this );
        postModuleEvent( ModuleEventType.STARTED );

        if( this.moduleActivatorClassName != null )
        {
            Class<?> clazz;
            try
            {
                clazz = this.bundle.loadClass( this.moduleActivatorClassName );
            }
            catch( Throwable e )
            {
                throw new IllegalStateException( "could not load activator class '" + this.moduleActivatorClassName + "' for module '" + this + "': " + e.getMessage(), e );
            }

            try
            {
                Class<? extends ModuleActivator> activatorClass = clazz.asSubclass( ModuleActivator.class );
                Constructor<? extends ModuleActivator> ctor = activatorClass.getDeclaredConstructor();
                ctor.setAccessible( true );
                this.activator = ctor.newInstance();
            }
            catch( Throwable e )
            {
                throw new IllegalStateException( "could not create activator '" + this.moduleActivatorClassName + "' for module '" + this + "': " + e.getMessage(), e );
            }
            // TODO: should we do this here or in ModuleImpl.onBeforeActivate?
            this.activator.onBeforeActivate( this );
        }
    }

    @Override
    protected synchronized void onBeforeStop()
    {
        MODULE_STOPPING_LOG.info( "STOPPING {}", this );
        postModuleEvent( ModuleEventType.STOPPING );
    }

    @Override
    protected synchronized void onAfterStop()
    {
        MODULE_STOPPED_LOG.info( "STOPPED {}", this );
        postModuleEvent( ModuleEventType.STOPPED );
        this.types = Collections.emptyMap();
    }

    @Override
    protected synchronized void onBeforeActivate()
    {
        MODULE_ACTIVATING_LOG.info( "ACTIVATING {}", this );
        postModuleEvent( ModuleEventType.ACTIVATING );
    }

    @Override
    protected synchronized void onAfterActivate()
    {
        MODULE_ACTIVATED_LOG.info( "ACTIVATED {}", this );
        postModuleEvent( ModuleEventType.ACTIVATED );

        if( this.activator != null )
        {
            try
            {
                this.activator.onAfterDeactivate( this );
            }
            catch( Throwable e )
            {
                TYPES_LOG.error( "Activator '{}' for module '{}' threw exception: {}", this.activator, this, e.getMessage(), e );
            }
            finally
            {
                this.activator = null;
            }
        }
    }

    @Override
    protected synchronized void onBeforeDeactivate()
    {
        MODULE_DEACTIVATING_LOG.info( "DEACTIVATING {}", this );
        postModuleEvent( ModuleEventType.DEACTIVATING );
    }

    @Override
    protected synchronized void onAfterDeactivate()
    {
        MODULE_DEACTIVATED_LOG.info( "DEACTIVATED {}", this );
        postModuleEvent( ModuleEventType.DEACTIVATED );
    }

    synchronized void onBundleInstalled()
    {
        MODULE_INSTALL_LOG.info( "INSTALLED {}", this );
        postModuleEvent( ModuleEventType.INSTALLED );
    }

    synchronized void onBundleResolved()
    {
        MODULE_RESOLVE_LOG.info( "RESOLVED {}", this );
        this.resourceCache.invalidateAll();
        postModuleEvent( ModuleEventType.RESOLVED );
    }

    synchronized void onBundleStarting()
    {
        try
        {
            start();
        }
        catch( Exception e )
        {
            LOG.error( "Cannot start {}: {}", this, e.getMessage(), e );
        }
    }

    synchronized void onBundleStarted()
    {
        try
        {
            activate();
        }
        catch( Exception e )
        {
            LOG.error( "Cannot activate {}: {}", this, e.getMessage(), e );
        }
    }

    synchronized void onBundleStopping()
    {
        deactivate();
        stop();
    }

    synchronized void onBundleStopped()
    {
    }

    synchronized void onBundleUpdated()
    {
        MODULE_UPDATED_LOG.info( "UPDATED {}", this );
        postModuleEvent( ModuleEventType.UPDATED );
    }

    synchronized void onBundleUnresolved()
    {
        MODULE_UNRESOLVED_LOG.info( "UNRESOLVED {}", this );
        this.resourceCache.invalidateAll();
        postModuleEvent( ModuleEventType.UNRESOLVED );
    }

    synchronized void onBundleUninstalled()
    {
        MODULE_UNINSTALLED_LOG.info( "UNINSTALLED {}", this );
        postModuleEvent( ModuleEventType.UNINSTALLED );
    }

    @Nonnull
    List<org.mosaic.modules.impl.TypeDescriptor> getTypeDescriptors( @Nonnull Class<?> type )
    {
        // TODO: cache result (clear cache on restart)

        List<org.mosaic.modules.impl.TypeDescriptor> typeDescriptors = null;
        for( org.mosaic.modules.impl.TypeDescriptor typeDescriptor : this.types.values() )
        {
            if( type.isAssignableFrom( typeDescriptor.getType() ) )
            {
                if( typeDescriptors == null )
                {
                    typeDescriptors = new LinkedList<>();
                }
                typeDescriptors.add( typeDescriptor );
            }
        }
        return typeDescriptors == null ? Collections.<org.mosaic.modules.impl.TypeDescriptor>emptyList() : typeDescriptors;
    }

    private void postModuleEvent( @Nonnull ModuleEventType eventType )
    {
        Bundle modulesBundle = FrameworkUtil.getBundle( getClass() );
        if( modulesBundle == null )
        {
            throw new IllegalStateException();
        }

        BundleContext modulesBundleContext = modulesBundle.getBundleContext();
        if( modulesBundleContext == null )
        {
            throw new IllegalStateException();
        }

        org.osgi.framework.ServiceReference<EventAdmin> eventAdminReference = modulesBundleContext.getServiceReference( EventAdmin.class );
        if( eventAdminReference == null )
        {
            LOG.warn( "Event admin service not found - cannot post module event {} to listeners", eventType );
            return;
        }

        EventAdmin eventAdmin = modulesBundleContext.getService( eventAdminReference );
        if( eventAdmin != null )
        {
            try
            {
                Dictionary<String, Object> dict = new Hashtable<>();
                dict.put( "mosaicEvent", new ModuleEvent( this, eventType ) );
                eventAdmin.postEvent( new Event( "org/mosaic/modules/ModuleEvent", dict ) );
            }
            catch( Throwable e )
            {
                LOG.warn( "Posting of module event {} failed: {}", eventType, e.getMessage(), e );
            }
            finally
            {
                modulesBundleContext.ungetService( eventAdminReference );
            }
        }
    }

    @Nonnull
    private Bundle requireResolvedBundle()
    {
        if( this.bundle.getState() == Bundle.INSTALLED )
        {
            throw new IllegalStateException( "cannot search for resources in module " + this + " while it is in the INSTALLED state" );
        }
        else if( this.bundle.getState() == Bundle.UNINSTALLED )
        {
            throw new IllegalStateException( "cannot search for resources in module " + this + " while it is in the UNINSTALLED state" );
        }
        else
        {
            return this.bundle;
        }
    }

    @Nonnull
    private Map<Class<?>, org.mosaic.modules.impl.TypeDescriptor> createComponentsMap()
    {
        Map<Class<?>, org.mosaic.modules.impl.TypeDescriptor> componentDescriptors = new ConcurrentHashMap<>();

        Collection<Path> resources;
        try
        {
            resources = findResources( "**/*.class" );
        }
        catch( IOException e )
        {
            throw new IllegalStateException( "could not find class resources in '" + this + "': " + e.getMessage(), e );
        }

        for( Path path : resources )
        {
            String className = path.toString().replace( "/", "." );
            className = className.substring( 0, className.length() - ".class".length() );
            if( className.startsWith( "." ) )
            {
                className = className.substring( 1 );
            }

            Class<?> clazz;
            try
            {
                clazz = this.bundle.loadClass( className );
            }
            catch( Throwable e )
            {
                CLASS_LOAD_ERRORS_LOG.trace( "Could not load class '{}' from module {}: {}", className, this, e.getMessage(), e );
                continue;
            }

            try
            {
                org.mosaic.modules.impl.TypeDescriptor componentDescriptor = new org.mosaic.modules.impl.TypeDescriptor( this, clazz );
                componentDescriptors.put( clazz, componentDescriptor );
            }
            catch( NoClassDefFoundError | TypeNotPresentException e )
            {
                CLASS_LOAD_ERRORS_LOG.trace( "Could not load class '{}' from module {}: {}", className, this, e.getMessage(), e );
            }
            catch( ComponentDefinitionException e )
            {
                throw e;
            }
            catch( Throwable e )
            {
                throw new ComponentDefinitionException( e, clazz, this );
            }
        }
        return componentDescriptors;
    }

    @Nonnull
    private SimpleDirectedGraph<org.mosaic.modules.impl.TypeDescriptor, ComponentDependency> createComponentsGraph( @Nonnull Map<Class<?>, org.mosaic.modules.impl.TypeDescriptor> types )
    {
        SimpleDirectedGraph<org.mosaic.modules.impl.TypeDescriptor, ComponentDependency> componentsGraph = new SimpleDirectedGraph<>( ComponentDependency.class );
        for( org.mosaic.modules.impl.TypeDescriptor typeDescriptor : types.values() )
        {
            componentsGraph.addVertex( typeDescriptor );

            // a component by definition requires all its superclasses as components too
            Class<?> type = typeDescriptor.getType().getSuperclass();
            while( type != null )
            {
                org.mosaic.modules.impl.TypeDescriptor superClassComponent = types.get( type );
                if( superClassComponent != null )
                {
                    componentsGraph.addVertex( superClassComponent );
                    componentsGraph.addEdge( typeDescriptor, superClassComponent );
                }
                type = type.getSuperclass();
            }

            // a component requires wired field components too
            for( TypeDescriptorFieldComponent field : typeDescriptor.getChildren( TypeDescriptorFieldComponent.class, false ) )
            {
                Class<?> requiredComponentType = field.getFieldType();

                // check that component doesn't require itself
                if( requiredComponentType.isAssignableFrom( typeDescriptor.getType() ) )
                {
                    String msg = "type " + typeDescriptor + " requires itself";
                    throw new ComponentDefinitionException( msg, typeDescriptor.getType(), this );
                }

                // make this component depend on all other components matching current field
                for( org.mosaic.modules.impl.TypeDescriptor candidateDescriptor : types.values() )
                {
                    if( candidateDescriptor != typeDescriptor )
                    {
                        if( requiredComponentType.isAssignableFrom( candidateDescriptor.getType() ) )
                        {
                            componentsGraph.addVertex( candidateDescriptor );
                            componentsGraph.addEdge( typeDescriptor, candidateDescriptor );
                        }
                    }
                }
            }

            // a component requires wired field components too
            for( TypeDescriptorFieldComponentList field : typeDescriptor.getChildren( TypeDescriptorFieldComponentList.class, false ) )
            {
                Class<?> requiredComponentType = field.getFieldListItemType();

                // check that component doesn't require itself
                if( requiredComponentType.isAssignableFrom( typeDescriptor.getType() ) )
                {
                    String msg = "type " + typeDescriptor + " requires itself";
                    throw new ComponentDefinitionException( msg, typeDescriptor.getType(), this );
                }

                // make this component depend on all other components matching current field
                for( org.mosaic.modules.impl.TypeDescriptor candidateDescriptor : types.values() )
                {
                    if( candidateDescriptor != typeDescriptor )
                    {
                        if( requiredComponentType.isAssignableFrom( candidateDescriptor.getType() ) )
                        {
                            componentsGraph.addVertex( candidateDescriptor );
                            componentsGraph.addEdge( typeDescriptor, candidateDescriptor );
                        }
                    }
                }
            }
        }
        return componentsGraph;
    }

    @Nullable
    private String getPackageNameFromRequirement( @Nonnull BundleRequirement requirement )
    {
        // reflection code here depends on Felix "BundleRequirementImpl.getFilter()" method
        try
        {
            Object filter = requirement.getClass().getMethod( "getFilter" ).invoke( requirement );
            String packageName = getPackageNameFromFilter( filter );
            if( packageName != null )
            {
                return packageName;
            }
            else
            {
                LOG.warn( "Unable to extract package name from bundle requirement '{}' of module {}", requirement, this );
                return null;
            }
        }
        catch( Exception e )
        {
            LOG.warn( "Unable to extract package name from bundle requirement '{}' of module {}: {}", requirement, this, e.getMessage(), e );
            return null;
        }
    }

    @Nullable
    private String getPackageNameFromFilter( @Nonnull Object filter )
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        // reflection code here depends on Felix "SimpleFilter.getName()" and "SimpleFilter.getValue()" methods
        // in particular, it depends on SimpleFilter's behavior such that when 'name' is null, its value is a List

        Object name = filter.getClass().getMethod( "getName" ).invoke( filter );
        Object value = filter.getClass().getMethod( "getValue" ).invoke( filter );

        if( name == null )
        {
            // value must be a list
            List filters = ( List ) value;
            for( Object subFilter : filters )
            {
                String packageName = getPackageNameFromFilter( subFilter );
                if( packageName != null )
                {
                    return packageName;
                }
            }
            return null;
        }
        else
        {
            return PackageNamespace.PACKAGE_NAMESPACE.equals( name.toString() ) ? value.toString() : null;
        }
    }

    private class PackageRequirementImpl implements PackageRequirement
    {
        @Nonnull
        private final String packageName;

        @Nullable
        private final String filter;

        @Nullable
        private final String resolution;

        @Nullable
        private final ModuleImpl provider;

        @Nullable
        private final Version version;

        private PackageRequirementImpl( @Nonnull String packageName,
                                        @Nullable String filter,
                                        @Nullable String resolution,
                                        @Nullable ModuleImpl provider,
                                        @Nullable Version version )
        {
            this.packageName = packageName;
            this.filter = filter;
            this.resolution = resolution;
            this.provider = provider;
            this.version = version;
        }

        @Nonnull
        @Override
        public Module getConsumer()
        {
            return ModuleImpl.this;
        }

        @Nonnull
        @Override
        public String getPackageName()
        {
            return this.packageName;
        }

        @Nullable
        @Override
        public String getFilter()
        {
            return this.filter;
        }

        @Override
        public boolean isOptional()
        {
            return "optional".equals( this.resolution );
        }

        @Nullable
        @Override
        public Module getProvider()
        {
            return this.provider;
        }

        @Nullable
        @Override
        public Version getVersion()
        {
            return this.version;
        }
    }

    private class PackageCapabilityImpl implements Module.PackageCapability
    {
        @Nonnull
        private final String packageName;

        @Nonnull
        private final Version version;

        @Nonnull
        private final Collection<Module> consumers;

        private PackageCapabilityImpl( @Nonnull String packageName,
                                       @Nonnull Version version,
                                       @Nonnull Collection<Module> consumers )
        {
            this.packageName = packageName;
            this.version = version;
            this.consumers = consumers;
        }

        @Nonnull
        @Override
        public Module getProvider()
        {
            return ModuleImpl.this;
        }

        @Nonnull
        @Override
        public String getPackageName()
        {
            return this.packageName;
        }

        @Nonnull
        @Override
        public Version getVersion()
        {
            return this.version;
        }

        @Nonnull
        @Override
        public Collection<Module> getConsumers()
        {
            return this.consumers;
        }
    }

    private class ServiceRegistrationImpl<Type> implements ServiceRegistration<Type>
    {
        @Nonnull
        private final org.osgi.framework.ServiceRegistration<Type> registration;

        @Nonnull
        private final Class<Type> type;

        private ServiceRegistrationImpl( @Nonnull org.osgi.framework.ServiceRegistration<Type> registration,
                                         @Nonnull Class<Type> type )
        {
            this.registration = registration;
            this.type = type;
        }

        @Nonnull
        @Override
        public Module getProvider()
        {
            return ModuleImpl.this;
        }

        @Nonnull
        @Override
        public Class<Type> getType()
        {
            return this.type;
        }

        @Nonnull
        @Override
        public MapEx<String, Object> getProperties()
        {
            org.osgi.framework.ServiceReference<Type> reference = this.registration.getReference();

            MapEx<String, Object> properties = new HashMapEx<>();
            for( String key : reference.getPropertyKeys() )
            {
                properties.put( key, reference.getProperty( key ) );
            }
            return UnmodifiableMapEx.of( properties );
        }

        @Override
        public void setProperties( @Nonnull Map<String, Object> properties )
        {
            this.registration.setProperties( new Hashtable<>( properties ) );
        }

        @Override
        public void setProperty( @Nonnull String name, @Nullable Object value )
        {
            Map<String, Object> properties = new HashMap<>( getProperties() );
            properties.put( name, value );
            this.registration.setProperties( new Hashtable<>( properties ) );
        }

        @Override
        public void setProperty( @Nonnull Property property )
        {
            setProperty( property.getKey(), property.getValue() );
        }

        @Override
        public void removeProperty( @Nonnull String name )
        {
            Map<String, Object> properties = new HashMap<>( getProperties() );
            properties.remove( name );
            this.registration.setProperties( new Hashtable<>( properties ) );
        }

        @Override
        public boolean isRegistered()
        {
            try
            {
                this.registration.getReference();
                return true;
            }
            catch( IllegalStateException e )
            {
                return false;
            }
        }

        @Override
        public void unregister()
        {
            try
            {
                this.registration.unregister();
            }
            catch( Exception ignore )
            {
            }
        }
    }

    private class ServiceReferenceImpl<T> implements ServiceReference<T>
    {
        @Nonnull
        private final org.osgi.framework.ServiceReference<T> reference;

        @Nonnull
        private final Class<T> serviceType;

        @Nonnull
        private final WeakReference<T> service;

        private ServiceReferenceImpl( @Nonnull org.osgi.framework.ServiceReference<T> reference,
                                      @Nonnull Class<T> serviceType )
        {
            this.reference = reference;
            this.serviceType = serviceType;

            BundleContext bundleContext = ModuleImpl.this.bundle.getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "module not active" );
            }
            this.service = new WeakReference<>( bundleContext.getService( this.reference ) );
        }

        @Override
        public long getId()
        {
            return ( Long ) this.reference.getProperty( Constants.SERVICE_ID );
        }

        @Nonnull
        @Override
        public Class<? extends T> getType()
        {
            return this.serviceType;
        }

        @Nonnull
        @Override
        public MapEx<String, Object> getProperties()
        {
            MapEx<String, Object> properties = new HashMapEx<>();
            for( String name : this.reference.getPropertyKeys() )
            {
                properties.put( name, this.reference.getProperty( name ) );
            }
            return UnmodifiableMapEx.of( properties );
        }

        @Nullable
        @Override
        public Module getProvider()
        {
            return ModuleImpl.this.moduleManager.getModule( this.reference.getBundle().getBundleId() ).orNull();
        }

        @Nonnull
        @Override
        public Optional<T> service()
        {
            return Optional.fromNullable( this.service.get() );
        }
    }
}

package org.mosaic.lifecycle.impl;

import com.google.common.collect.ComparisonChain;
import com.google.common.reflect.TypeToken;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.*;
import org.mosaic.util.reflection.MethodHandleFactory;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

/**
 * @author arik
 */
public class ModuleImpl implements Module
{
    private static final Logger LOG = LoggerFactory.getLogger( ModuleImpl.class );

    private static final Comparator<PackageExport> PACKAGE_EXPORT_COMPARATOR = new Comparator<PackageExport>()
    {
        @Override
        public int compare( PackageExport o1, PackageExport o2 )
        {
            return ComparisonChain
                    .start()
                    .compare( o1.getPackageName(), o2.getPackageName() )
                    .compare( o1.getVersion(), o2.getVersion() )
                    .result();
        }
    };

    @Nonnull
    private final ModuleHelper helper;

    @Nonnull
    private final ModuleManagerImpl moduleManager;

    @Nonnull
    private final Bundle bundle;

    @Nonnull
    private final Path path;

    @Nullable
    private Collection<String> resources;

    @Nonnull
    private ModuleState state = ModuleState.UNKNOWN;

    @Nullable
    private ModuleApplicationContext applicationContext;

    @Nullable
    private MetricsImpl metrics;

    public ModuleImpl( @Nonnull ModuleManagerImpl moduleManager,
                       @Nonnull MethodHandleFactory methodHandleFactory,
                       @Nonnull Bundle bundle )
    {
        this.helper = new ModuleHelper( this, methodHandleFactory );
        this.moduleManager = moduleManager;
        this.bundle = bundle;
        this.path = Paths.get( bundle.getLocation() );
    }

    @Nonnull
    public Bundle getBundle()
    {
        return this.bundle;
    }

    @Nullable
    public BundleContext getBundleContext()
    {
        return this.bundle.getBundleContext();
    }

    @Nullable
    public ClassLoader getClassLoader()
    {
        BundleWiring bundleWiring = this.bundle.adapt( BundleWiring.class );
        if( bundleWiring != null )
        {
            ClassLoader classLoader = bundleWiring.getClassLoader();
            if( classLoader != null )
            {
                return classLoader;
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public Path getPath()
    {
        return this.path;
    }

    @Nonnull
    @Override
    public Map<String, String> getHeaders()
    {
        Map<String, String> headers = new HashMap<>();

        Dictionary<String, String> dictionary = this.bundle.getHeaders();
        Enumeration<String> keyesEnumeration = dictionary.keys();
        while( keyesEnumeration.hasMoreElements() )
        {
            String headerName = keyesEnumeration.nextElement();
            headers.put( headerName, dictionary.get( headerName ) );
        }
        return headers;
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
    public String getVersion()
    {
        return this.bundle.getVersion().toString();
    }

    @Nonnull
    @Override
    public ModuleState getState()
    {
        return this.state;
    }

    @Override
    public long getLastModified()
    {
        return this.bundle.getLastModified();
    }

    @Nonnull
    @Override
    public Collection<String> getResources()
    {
        if( this.resources == null )
        {
            return Collections.emptyList();
        }
        else
        {
            return this.resources;
        }
    }

    @Nonnull
    @Override
    public Collection<URL> getResourceUrls( @Nonnull String prefix )
    {
        // only provide resource URLs if this module loaded its resources on resolve
        if( this.resources != null )
        {
            Set<URL> urls = new HashSet<>( 1000 );
            Enumeration<URL> entries = this.bundle.findEntries( prefix, "*", true );
            if( entries != null )
            {
                while( entries.hasMoreElements() )
                {
                    URL url = entries.nextElement();
                    if( !url.getPath().endsWith( "/" ) )
                    {
                        urls.add( url );
                    }
                }
            }
            return urls;
        }
        else
        {
            return Collections.emptySet();
        }
    }

    @Nonnull
    @Override
    public Collection<ServiceExport> getExportedServices()
    {
        ServiceReference<?>[] registeredServices = this.bundle.getRegisteredServices();
        if( registeredServices == null )
        {
            return Collections.emptyList();
        }

        Collection<ServiceExport> serviceExports = new LinkedList<>();
        for( ServiceReference<?> serviceReference : registeredServices )
        {
            addServiceExportsFromServiceReference( serviceExports, serviceReference );
        }
        return serviceExports;
    }

    @Nonnull
    @Override
    public Collection<ServiceExport> getImportedServices()
    {
        ServiceReference<?>[] importedServices = this.bundle.getServicesInUse();
        if( importedServices == null )
        {
            return Collections.emptyList();
        }

        Collection<ServiceExport> serviceImports = new LinkedList<>();
        for( ServiceReference<?> serviceReference : importedServices )
        {
            addServiceExportsFromServiceReference( serviceImports, serviceReference );
        }
        return serviceImports;
    }

    @Nonnull
    @Override
    public Collection<PackageExport> getExportedPackages()
    {
        BundleRevision revision = this.bundle.adapt( BundleRevision.class );
        List<BundleCapability> packageCapabilities = revision.getDeclaredCapabilities( PACKAGE_NAMESPACE );
        if( packageCapabilities.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<PackageExport> packageExports = new LinkedList<>();
        for( BundleCapability capability : packageCapabilities )
        {
            packageExports.add( new PackageExportImpl( this.moduleManager, capability ) );
        }
        Collections.sort( packageExports, PACKAGE_EXPORT_COMPARATOR );
        return packageExports;
    }

    @Nonnull
    @Override
    public Collection<PackageExport> getImportedPackages()
    {
        BundleRevision revision = this.bundle.adapt( BundleRevision.class );
        List<BundleWire> importedPackageWires = revision.getWiring().getRequiredWires( PACKAGE_NAMESPACE );
        if( importedPackageWires.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<PackageExport> packageImports = new LinkedList<>();
        for( BundleWire wire : importedPackageWires )
        {
            packageImports.add( new PackageExportImpl( this.moduleManager, wire.getCapability() ) );
        }
        Collections.sort( packageImports, PACKAGE_EXPORT_COMPARATOR );
        return packageImports;
    }

    @Nonnull
    @Override
    public <T> ServiceExport exportService( @Nonnull Class<? super T> type, T service, @Nonnull DP... properties )
    {
        // prepare properties dictionary
        Dictionary<String, Object> dict = null;
        if( properties.length > 0 )
        {
            dict = new Hashtable<>( properties.length );
            for( DP dp : properties )
            {
                dict.put( dp.getKey(), dp.getValue() );
            }
        }

        // register & return
        ServiceRegistration<? super T> sr = this.bundle.getBundleContext().registerService( type, service, dict );
        return new ServiceExportImpl( this.moduleManager, this, TypeToken.of( type ), sr );
    }

    @Nullable
    @Override
    public URL getResource( @Nonnull String name )
    {
        return this.bundle.getEntry( name );
    }

    @Override
    public void activate() throws ModuleStartException
    {
        if( this.bundle.getState() == Bundle.ACTIVE )
        {
            try
            {
                activateInternal();
            }
            catch( ModuleStartException e )
            {
                throw e;
            }
            catch( Exception e )
            {
                throw new ModuleStartException( this, e.getMessage(), e );
            }
        }
        else
        {
            try
            {
                this.bundle.start();
            }
            catch( BundleException e )
            {
                if( e.getType() == BundleException.RESOLVE_ERROR )
                {
                    throw new ModuleResolveException( this, e );
                }
                else
                {
                    throw new ModuleStartException( this, e.getMessage(), e );
                }
            }
            catch( Exception e )
            {
                throw new ModuleStartException( this, e.getMessage(), e );
            }
        }
    }

    @Override
    public void deactivate() throws ModuleStopException
    {
        // TODO: prevent stopping the whole server (throw error here instead... catch other mosaic modules too)
        if( this.bundle.getBundleId() > 0 )
        {
            try
            {
                if( this.bundle.getState() != Bundle.STOPPING )
                {
                    this.bundle.stop( 0 );
                }
            }
            catch( Exception e )
            {
                throw new ModuleStopException( this, e.getMessage(), e );
            }
        }
    }

    @Nonnull
    @Override
    public <T> T getBean( @Nonnull Class<? extends T> type )
    {
        ModuleApplicationContext applicationContext = this.applicationContext;
        if( applicationContext == null )
        {
            throw new IllegalStateException( "Module '" + this + "' has no context or has not been activated" );
        }
        else
        {
            return this.applicationContext.findModuleBean( type );
        }
    }

    @Nonnull
    @Override
    public <T> T getBean( @Nonnull String beanName, @Nonnull Class<? extends T> type )
    {
        ModuleApplicationContext applicationContext = this.applicationContext;
        if( applicationContext == null )
        {
            throw new IllegalStateException( "Module '" + this + "' has no context" );
        }
        else
        {
            return this.applicationContext.findModuleBean( beanName, type );
        }
    }

    @Nullable
    @Override
    public Metrics getMetrics()
    {
        return this.metrics;
    }

    public synchronized void handleBundleEvent( BundleEvent event )
    {
        switch( event.getType() )
        {
            case BundleEvent.INSTALLED:
                onBundleInstall();
                break;

            case BundleEvent.RESOLVED:
                onBundleResolved();
                break;

            case BundleEvent.STARTING:
                onBundleStarting();
                break;

            case BundleEvent.STARTED:
                onBundleStarted();
                break;

            case BundleEvent.STOPPING:
                onBundleStopping();
                break;

            case BundleEvent.STOPPED:
                onBundleStopped();
                break;

            case BundleEvent.UNRESOLVED:
                onBundleUnresolved();
                break;

            case BundleEvent.UPDATED:
                onBundleUpdated();
                break;

            case BundleEvent.UNINSTALLED:
                onBundleUninstalled();
                break;
        }
    }

    @Nullable
    public Object getBean( @Nonnull String beanName )
    {
        ModuleApplicationContext applicationContext = this.applicationContext;
        if( applicationContext != null )
        {
            return applicationContext.getBean( beanName );
        }
        else
        {
            return null;
        }
    }

    public void beanCreated( @Nonnull Object bean, @Nonnull String beanName )
    {
        this.helper.onBeansCreated( bean, beanName );
    }

    public void beanInitialized( @Nonnull Object bean, @Nonnull String beanName )
    {
        this.helper.onBeanInitialized( bean, beanName );
    }

    public void onDependencySatisfied()
    {
        try
        {
            activateInternal();
        }
        catch( ModuleStartException ignore )
        {
        }
        catch( Exception e )
        {
            LOG.warn( e.getMessage(), e );
        }
    }

    public void onDependencyUnsatisfied()
    {
        deactivateInternal();
    }

    public boolean canBeActivated()
    {
        return !this.helper.hasUnsatisfiedDependencies();
    }

    @Override
    public Collection<Dependency> getDependencies()
    {
        return this.helper.getDependencies();
    }

    @Override
    public Collection<Dependency> getUnsatisfiedDependencies()
    {
        return this.helper.getUnsatisfiedDependencies();
    }

    @Override
    public String toString()
    {
        return "Module[" + this.bundle.getSymbolicName() + "-" + this.bundle.getVersion() + " -> " + this.bundle.getBundleId() + " | " + getState() + "]";
    }

    private synchronized void activateInternal() throws ModuleStartException
    {
        if( this.applicationContext != null )
        {
            return;
        }
        else if( this.state == ModuleState.ACTIVE || this.state == ModuleState.ACTIVATING )
        {
            return;
        }
        else if( !canBeActivated() )
        {
            throw new ModuleStartException( this, "has unsatisfied dependencies" );
        }

        // switch to ACTIVATING state
        this.state = ModuleState.ACTIVATING;

        // create application context
        ModuleApplicationContext moduleApplicationContext;
        try
        {
            moduleApplicationContext = this.helper.createApplicationContext();
        }
        catch( Throwable e )
        {
            this.state = ModuleState.STARTED;
            throw new ModuleStartException( this, e.getMessage(), e );
        }
        this.applicationContext = moduleApplicationContext;

        // register all declared services
        this.helper.registerDependencies();

        // there! we're activated!
        this.state = ModuleState.ACTIVE;

        LOG.info( "Module {} has been ACTIVATED", getName() );
        this.moduleManager.notifyModuleActivated( this );
    }

    private synchronized void deactivateInternal()
    {
        if( this.applicationContext != null && this.state != ModuleState.DEACTIVATING )
        {
            this.state = ModuleState.DEACTIVATING;

            // unregister all declared services
            this.helper.unregisterDependencies();

            // stop all dependencies
            this.helper.stopDependencies();

            // stop application context
            try
            {
                this.applicationContext.close();
            }
            catch( Exception e )
            {
                LOG.warn( "An error occurred while deactivating {}: {}", this, e.getMessage(), e );
            }

            // remove reference to the application context to allow garbage collection of beans
            this.applicationContext = null;

            this.state = ModuleState.STARTED;
            LOG.info( "Module {} has been DEACTIVATED", getName() );

            this.moduleManager.notifyModuleDeactivated( this );
        }
    }

    private void addServiceExportsFromServiceReference( @Nonnull Collection<ServiceExport> serviceExports,
                                                        @Nonnull ServiceReference<?> serviceReference )
    {
        String[] classNames = ( String[] ) serviceReference.getProperty( Constants.OBJECTCLASS );
        for( String className : classNames )
        {
            final TypeToken<?> type;
            try
            {
                if( className.equals( MethodEndpoint.class.getName() ) )
                {
                    type = TypeToken.of( MethodEndpoint.class );
                }
                else
                {
                    type = TypeToken.of( this.bundle.loadClass( className ) );
                }
                serviceExports.add( new ServiceExportImpl( this.moduleManager, this, type, serviceReference ) );
            }
            catch( ClassNotFoundException e )
            {
                throw new IllegalStateException( "Could not load service type '" + className + "': " + e.getMessage(), e );
            }
        }
    }

    private void refreshResourcesCache()
    {
        List<String> resources = new ArrayList<>();

        Enumeration<URL> entries = this.bundle.findEntries( "/", "*", true );
        if( entries != null )
        {
            while( entries.hasMoreElements() )
            {
                String path = entries.nextElement().getPath();
                if( !path.endsWith( "/" ) )
                {
                    resources.add( path );
                }
            }
        }
        Collections.sort( resources );
        this.resources = Collections.unmodifiableCollection( resources );
    }

    private void onBundleInstall()
    {
        this.state = ModuleState.INSTALLED;
        LOG.debug( "Module {} has been INSTALLED", getName() );
        this.moduleManager.notifyModuleInstalled( this );
    }

    private void onBundleResolved()
    {
        refreshResourcesCache();
        this.metrics = new MetricsImpl( this );
        this.state = ModuleState.RESOLVED;
        LOG.debug( "Module {} has been RESOLVED", getName() );
    }

    private void onBundleStarting()
    {
        LOG.debug( "Bootstrapping module '{}'", getName() );
        this.state = ModuleState.STARTING;

        // add beans to application context, and also populate our dependencies during so
        this.helper.refreshModuleComponents();
    }

    private void onBundleStarted()
    {
        this.helper.startDependencies();
        this.state = ModuleState.STARTED;

        try
        {
            activateInternal();
        }
        catch( ModuleStartException e )
        {
            LOG.warn( e.getMessage(), e );
            for( Dependency dependency : getUnsatisfiedDependencies() )
            {
                LOG.warn( "    -> {}", dependency );
            }
        }
        catch( Exception e )
        {
            LOG.warn( "Could NOT activate {}: {}", this, e.getMessage(), e );
            for( Dependency dependency : getUnsatisfiedDependencies() )
            {
                LOG.warn( "    -> {}", dependency );
            }
        }
    }

    private void onBundleStopping()
    {
        LOG.debug( "Module {} is STOPPING", getName() );

        // deactivate
        deactivateInternal();

        this.state = ModuleState.STOPPING;

        // clear our registrars, dependencies and component classes to avoid keeping stuff in memory (in particular the class loader)
        this.helper.discardModuleComponents();
    }

    private void onBundleStopped()
    {
        this.state = ModuleState.RESOLVED;
        LOG.info( "Module {} has been STOPPED", getName() );
    }

    private void onBundleUpdated()
    {
        refreshResourcesCache();
        LOG.debug( "Module {} has been UPDATED", getName() );
    }

    private void onBundleUnresolved()
    {
        this.resources = null;
        if( this.metrics != null )
        {
            this.metrics.shutdown();
        }
        this.metrics = null;
        this.state = ModuleState.INSTALLED;
        LOG.debug( "Module {} has been UNRESOLVED", getName() );
    }

    private void onBundleUninstalled()
    {
        this.state = ModuleState.UNINSTALLED;
        LOG.info( "Module {} has been UNINSTALLED", getName() );
        this.moduleManager.notifyModuleUninstalled( this );
    }
}

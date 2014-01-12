package org.mosaic.modules.impl;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.*;
import org.mosaic.server.Version;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.collections.UnmodifiableMapEx;
import org.mosaic.util.osgi.FilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.unmodifiableList;

/**
 * @author arik
 */
final class ModuleWiringImpl extends Lifecycle implements ModuleWiring
{
    private static final Logger LOG = LoggerFactory.getLogger( ModuleWiringImpl.class );

    @Nonnull
    private final ModuleImpl module;

    ModuleWiringImpl( @Nonnull ModuleImpl module )
    {
        this.module = module;
    }

    @Override
    public String toString()
    {
        return "ModuleWiring[" + this.module + "]";
    }

    @Nonnull
    @Override
    public Module getModule()
    {
        return this.module;
    }

    @Nonnull
    @Override
    public Collection<PackageRequirement> getPackageRequirements()
    {
        Bundle bundle = this.module.getBundle();

        // if our bundle is not resolved, there can be no package requirements
        int bundleState = bundle.getState();
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
        BundleRevision revision = bundle.adapt( BundleRevision.class );
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
                    packageProviders.put( packageName, this.module.getModuleManager().getModuleFor( wire.getProvider().getBundle() ) );
                }
            }
        }

        // merge maps into package requirements list
        List<PackageRequirement> requirements = new LinkedList<>();
        for( String packageName : packageNames )
        {
            requirements.add(
                    new PackageRequirementImpl(
                            packageName,
                            packageFilters.get( packageName ),
                            packageResolutions.get( packageName ),
                            packageProviders.get( packageName ),
                            packageVersions.get( packageName ) ) );
        }
        return unmodifiableList( requirements );
    }

    @Nonnull
    @Override
    public Collection<PackageCapability> getPackageCapabilities()
    {
        Bundle bundle = this.module.getBundle();

        // if our bundle is not resolved, there can be no package requirements
        int bundleState = bundle.getState();
        if( bundleState == Bundle.INSTALLED || bundleState == Bundle.UNINSTALLED )
        {
            return Collections.emptyList();
        }

        // discovered package capabilities will be stored in these maps and later aggregated into PackageCapability instances
        Set<String> packageNames = new LinkedHashSet<>( 100 );
        Map<String, Version> packageVersions = new HashMap<>( 100 );
        Map<String, Set<Module>> packageConsumers = new HashMap<>();

        // obtain current bundle revision; we need this to discover DECLARED capabilities
        BundleRevision revision = bundle.adapt( BundleRevision.class );
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
                    consumers.add( this.module.getModuleManager().getModuleFor( wire.getRequirer().getBundle() ) );
                }
            }
        }

        // merge maps into package requirements list
        List<PackageCapability> capabilities = new LinkedList<>();
        for( String packageName : packageNames )
        {
            Set<Module> consumers = packageConsumers.get( packageName );
            capabilities.add(
                    new PackageCapabilityImpl(
                            packageName,
                            packageVersions.get( packageName ),
                            consumers == null ? Collections.<Module>emptyList() : consumers ) );
        }
        return unmodifiableList( capabilities );
    }

    @Nonnull
    @Override
    public Collection<ServiceRequirement> getServiceRequirements()
    {
        return this.module.getChildren( ServiceRequirement.class, true );
    }

    @Nonnull
    @Override
    public Collection<ServiceCapability> getServiceCapabilities()
    {
        List<ServiceCapability> serviceCapabilities = new LinkedList<>();
        for( ServiceCapabilityProvider serviceCapabilityProvider : this.module.getChildren( ServiceCapabilityProvider.class, true ) )
        {
            serviceCapabilities.addAll( serviceCapabilityProvider.getServiceCapabilities() );
        }
        return serviceCapabilities;
    }

    @Nullable
    @Override
    public <T> ServiceReference<T> findService( @Nonnull Class<T> type, @Nonnull Property... properties )
    {
        BundleContext bundleContext = this.module.getBundle().getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "module '" + this.module + "' not active" );
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
        if( this.module.getState() != ModuleState.ACTIVE )
        {
            throw new IllegalStateException( "module " + this.module + " is not active" );
        }

        BundleContext bundleContext = this.module.getBundle().getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "module " + this.module + " has no bundle context" );
        }

        Dictionary<String, Object> dict = new Hashtable<>();
        for( Property property : properties )
        {
            dict.put( property.getKey(), property.getValue() );
        }
        org.osgi.framework.ServiceRegistration<T> registration = bundleContext.registerService( type, service, dict );
        return new ServiceRegistrationImpl<>( registration, type );
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
            return ModuleWiringImpl.this.module;
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

    private class PackageCapabilityImpl implements PackageCapability
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
            return ModuleWiringImpl.this.module;
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
            return ModuleWiringImpl.this.module;
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
            this.registration.unregister();
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

            BundleContext bundleContext = ModuleWiringImpl.this.module.getBundle().getBundleContext();
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
            return Activator.getModuleManager().getModule( this.reference.getBundle().getBundleId() );
        }

        @Nullable
        @Override
        public T get()
        {
            return this.service.get();
        }

        @Nonnull
        @Override
        public T require()
        {
            T service = get();
            if( service == null )
            {
                throw new IllegalStateException( "service '" + this.serviceType.getName() + "' not available" );
            }
            return service;
        }
    }
}

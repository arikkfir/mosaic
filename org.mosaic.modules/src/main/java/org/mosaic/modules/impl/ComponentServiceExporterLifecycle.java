package org.mosaic.modules.impl;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.*;
import org.mosaic.util.collections.EmptyMapEx;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.osgi.framework.*;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class ComponentServiceExporterLifecycle extends Lifecycle implements ServiceCapabilityProvider
{
    @Nonnull
    private final ComponentDescriptorImpl<?> componentDescriptor;

    @Nonnull
    private final Class<?>[] serviceTypes;

    @Nonnull
    private final Map<String, String> serviceProperties;

    @Nullable
    private List<ServiceRegistration<?>> serviceRegistrations;

    ComponentServiceExporterLifecycle( @Nonnull ComponentDescriptorImpl<?> componentDescriptor )
    {
        this.componentDescriptor = componentDescriptor;

        Service serviceAnn = this.componentDescriptor.getComponentType().getAnnotation( Service.class );

        List<Class<?>> interfaces = new LinkedList<>();

        // add interfaces declared in @Service annotation to the service types list
        interfaces.addAll( asList( serviceAnn.value() ) );

        // if none were declared in the annotation, add all interfaces implemented by the component
        if( interfaces.isEmpty() )
        {
            interfaces.addAll( asList( componentDescriptor.getComponentType().getInterfaces() ) );
        }

        // if no interfaces are implemented by the component and no interfaces declared in the annotation, fail
        if( interfaces.isEmpty() )
        {
            String msg = "@Service component must declare service interface (via 'implements' clause or in the @Service annotation)";
            throw new ComponentDefinitionException( msg,
                                                    this.componentDescriptor.getComponentType(),
                                                    this.componentDescriptor.getModule() );
        }
        this.serviceTypes = interfaces.toArray( new Class[ interfaces.size() ] );

        this.serviceProperties = new LinkedHashMap<>();
        for( Service.P property : serviceAnn.properties() )
        {
            this.serviceProperties.put( property.key(), property.value() );
        }
    }

    @Override
    public final String toString()
    {
        return "ComponentServiceExporter[" + this.componentDescriptor + " as " + asList( this.serviceTypes ) + "]";
    }

    @Nonnull
    @Override
    public List<ModuleWiring.ServiceCapability> getServiceCapabilities()
    {
        List<ModuleWiring.ServiceCapability> capabilities = new LinkedList<>();
        if( this.serviceRegistrations != null )
        {
            for( ServiceRegistration<?> registration : this.serviceRegistrations )
            {
                capabilities.add( new ServiceCapabilityImpl( registration ) );
            }
        }
        return capabilities;
    }

    @Override
    protected synchronized void onAfterActivate()
    {
        Dictionary<String, Object> properties = new Hashtable<>();
        for( Map.Entry<String, String> entry : this.serviceProperties.entrySet() )
        {
            properties.put( entry.getKey(), entry.getValue() );
        }

        ComponentSingletonLifecycle singletonLifecycle = this.componentDescriptor.requireChild( ComponentSingletonLifecycle.class );
        Object instance = singletonLifecycle.getInstance();
        if( instance instanceof ServicePropertiesProvider )
        {
            ( ( ServicePropertiesProvider ) instance ).addProperties( properties );
        }

        this.serviceRegistrations = new LinkedList<>();
        for( Class serviceType : this.serviceTypes )
        {
            BundleContext bundleContext = this.componentDescriptor.getModule().getBundle().getBundleContext();
            if( bundleContext == null )
            {
                throw new IllegalStateException( "no bundle context for module " + componentDescriptor.getModule() );
            }
            ServiceRegistration<?> registration = bundleContext.registerService( serviceType, instance, properties );
            this.serviceRegistrations.add( registration );
        }
    }

    @Override
    protected synchronized void onBeforeDeactivate()
    {
        if( this.serviceRegistrations != null )
        {
            for( ServiceRegistration<?> registration : this.serviceRegistrations )
            {
                try
                {
                    registration.unregister();
                }
                catch( Throwable ignore )
                {
                }
            }
            this.serviceRegistrations = null;
        }
    }

    private class ServiceCapabilityImpl implements ModuleWiring.ServiceCapability
    {
        @Nonnull
        private final ServiceRegistration registration;

        private ServiceCapabilityImpl( @Nonnull ServiceRegistration registration )
        {
            this.registration = registration;
        }

        @Override
        public long getId()
        {
            org.osgi.framework.ServiceReference reference = this.registration.getReference();
            if( reference != null )
            {
                Long id = ( Long ) reference.getProperty( Constants.SERVICE_ID );
                if( id != null )
                {
                    return id;
                }
            }
            throw new IllegalStateException( "id not found" );
        }

        @Nonnull
        @Override
        public Module getProvider()
        {
            return ComponentServiceExporterLifecycle.this.componentDescriptor.getModule();
        }

        @Nonnull
        @Override
        public Class<?> getType()
        {
            ServiceReference reference = this.registration.getReference();
            if( reference != null )
            {
                String[] objectClass = ( String[] ) reference.getProperty( Constants.OBJECTCLASS );
                ModuleImpl module = ComponentServiceExporterLifecycle.this.componentDescriptor.getModule();
                try
                {
                    return module.getModuleResources().loadClass( objectClass[ 0 ] );
                }
                catch( Throwable e )
                {
                    throw new IllegalStateException( "service type not found", e );
                }
            }
            throw new IllegalStateException( "service type not found" );
        }

        @Nonnull
        @Override
        public MapEx<String, Object> getProperties()
        {
            org.osgi.framework.ServiceReference reference = this.registration.getReference();
            if( reference != null )
            {
                MapEx<String, Object> properties = new HashMapEx<>();
                for( String key : reference.getPropertyKeys() )
                {
                    properties.put( key, reference.getProperty( key ) );
                }
                return properties;
            }
            return EmptyMapEx.emptyMapEx();
        }

        @Nonnull
        @Override
        public Collection<Module> getConsumers()
        {
            org.osgi.framework.ServiceReference<?> reference = this.registration.getReference();
            if( reference != null )
            {
                Bundle[] usingBundles = reference.getUsingBundles();
                if( usingBundles != null )
                {
                    List<Module> consumers = new LinkedList<>();
                    for( Bundle bundle : usingBundles )
                    {
                        consumers.add( Activator.getModuleManager().getModule( bundle.getBundleId() ) );
                    }
                    return consumers;
                }
            }
            return Collections.emptyList();
        }
    }
}

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;

/**
 * @author arik
 */
@SuppressWarnings("unchecked")
final class ComponentServiceExporter extends Lifecycle implements ModuleServiceCapabilityProvider
{
    private static final Logger LOG = LoggerFactory.getLogger( ComponentServiceExporter.class );

    @Nonnull
    private final Component component;

    @Nonnull
    private final Class<?>[] serviceTypes;

    @Nonnull
    private final Map<String, Object> serviceProperties;

    @Nullable
    private Collection<ServiceRegistration<?>> serviceRegistrations;

    ComponentServiceExporter( @Nonnull Component component )
    {
        this.component = component;
        this.serviceTypes = getServiceInterfaces();
        this.serviceProperties = getServiceProperties();
    }

    @Override
    public final String toString()
    {
        return "ComponentServiceExporter[" + this.component + " as " + asList( this.serviceTypes ) + "]";
    }

    @Nonnull
    @Override
    public List<Module.ServiceCapability> getServiceCapabilities()
    {
        List<Module.ServiceCapability> capabilities = new LinkedList<>();
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
        Object instance = this.component.getInstance();
        if( instance == null )
        {
            LOG.warn( "Could not export {} - instance not created yet", this );
            return;
        }

        Dictionary<String, Object> properties = new Hashtable<>();
        for( Map.Entry<String, Object> entry : this.serviceProperties.entrySet() )
        {
            properties.put( entry.getKey(), entry.getValue() );
        }

        if( instance instanceof ServicePropertiesProvider )
        {
            ( ( ServicePropertiesProvider ) instance ).addProperties( properties );
        }

        BundleContext bundleContext = this.component.getModule().getBundle().getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "no bundle context for module " + this.component.getModule() );
        }

        this.serviceRegistrations = new LinkedList<>();
        for( Class serviceType : this.serviceTypes )
        {
            this.serviceRegistrations.add( bundleContext.registerService( serviceType, instance, properties ) );
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

    @Nonnull
    private Class<?>[] getServiceInterfaces()
    {
        Class<?> type = this.component.getType();

        List<Class<?>> interfaces = new LinkedList<>();

        // add interfaces declared in @Service annotation to the service types list
        interfaces.addAll( asList( this.component.getType().getAnnotation( Service.class ).value() ) );

        // if none were declared in the annotation, add all interfaces implemented by the component
        if( interfaces.isEmpty() )
        {
            interfaces.addAll( asList( type.getInterfaces() ) );
        }

        // if no interfaces are implemented by the component and no interfaces declared in the annotation, fail
        if( interfaces.isEmpty() )
        {
            String msg = "@Service component must declare service interface (via 'implements' clause or in the @Service annotation)";
            throw new ComponentDefinitionException( msg, type, this.component.getModule() );
        }

        return interfaces.toArray( new Class[ interfaces.size() ] );
    }

    @Nonnull
    private Map<String, Object> getServiceProperties()
    {
        Map<String, Object> properties = new LinkedHashMap<>();
        for( Service.P property : this.component.getType().getAnnotation( Service.class ).properties() )
        {
            properties.put( property.key(), property.value() );
        }

        Ranking rankingAnn = this.component.getType().getAnnotation( Ranking.class );
        if( rankingAnn != null )
        {
            properties.put( Constants.SERVICE_RANKING, rankingAnn.value() );
        }

        return unmodifiableMap( properties );
    }

    private class ServiceCapabilityImpl implements Module.ServiceCapability
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
            return ComponentServiceExporter.this.component.getModule();
        }

        @Nonnull
        @Override
        public Class<?> getType()
        {
            ServiceReference reference = this.registration.getReference();
            if( reference != null )
            {
                String[] objectClass = ( String[] ) reference.getProperty( Constants.OBJECTCLASS );
                ModuleImpl module = ComponentServiceExporter.this.component.getModule();
                try
                {
                    return module.getClassLoader().loadClass( objectClass[ 0 ] );
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
                        consumers.add( Activator.getModuleManager().getModule( bundle.getBundleId() ).get() );
                    }
                    return consumers;
                }
            }
            return Collections.emptyList();
        }
    }
}

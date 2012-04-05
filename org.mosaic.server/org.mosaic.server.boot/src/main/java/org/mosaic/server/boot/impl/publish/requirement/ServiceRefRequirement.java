package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.osgi.framework.ServiceReference;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class ServiceRefRequirement extends ServiceRequirement {

    private final boolean required;

    private ServiceCache cache;

    public ServiceRefRequirement( BundlePublisher publisher,
                                  Class<?> serviceType,
                                  String additionalFilter,
                                  boolean required,
                                  String beanName,
                                  Method targetMethod ) {
        super( publisher, serviceType, additionalFilter, beanName, targetMethod );
        this.required = required;
    }

    @Override
    public Object addingService( ServiceReference<Object> serviceReference ) {
        Object newService = super.addingService( serviceReference );

        if( this.cache == null ) {

            // this is the first service we've found to match our requirement - cache it
            this.cache = new ServiceCache( newService, serviceReference );

            // notify published that we're satisfied - publish if needed
            markAsSatisfied( newService );

        }

        return newService;
    }

    @Override
    public void removedService( ServiceReference<Object> serviceReference, Object service ) {

        if( this.cache.getService() == service ) {

            // obtain a suitable replacement
            ServiceReference<Object> newServiceReference = this.tracker.getServiceReference();
            Object newService = this.publisher.getBundleContext().getService( newServiceReference );
            if( newService != null ) {

                // a replacement was found, cache it
                this.cache = new ServiceCache( newService, newServiceReference );
                markAsSatisfied( newService );

            } else if( this.required ) {

                // no replacement was found - clear reference cache and inform publisher to un-publish bundle
                this.cache = null;
                markAsUnsatisfied();

            } else {

                // no replacement was found - clear our reference cache
                this.cache = null;
                markAsSatisfied( null );

            }
        }
    }

    @Override
    public void apply( ApplicationContext applicationContext, Object state ) throws Exception {
        targetMethod.invoke( applicationContext.getBean( this.beanName ), state );
    }

    @Override
    public void applyInitial( ApplicationContext applicationContext ) throws Exception {
        Object service = this.tracker.getService();
        if( service == null ) {
            throw new IllegalStateException( "Service is null even though requirement was satisfied" );
        } else {
            targetMethod.invoke( applicationContext.getBean( this.beanName ), service );
        }
    }

    @Override
    public void revert() throws Exception {
        // no-op
    }

    private static class ServiceCache {

        private final Object service;

        private final Map<String, Object> properties;

        private ServiceCache( Object service, ServiceReference<Object> serviceReference ) {
            this.service = service;

            Map<String, Object> properties = new HashMap<>();
            for( String propertyKey : serviceReference.getPropertyKeys() ) {
                properties.put( propertyKey, serviceReference.getProperty( propertyKey ) );
            }
            this.properties = properties;
        }

        public Object getService() {
            return service;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }
    }
}

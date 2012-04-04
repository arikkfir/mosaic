package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.mosaic.server.boot.impl.publish.BundlePublishException;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author arik
 */
public class ServiceRefRequirement extends ServiceRequirement {

    private final boolean required;

    private ServiceCache cache;

    public ServiceRefRequirement( BundleContext bundleContext,
                                  BundlePublisher publisher,
                                  Class<?> serviceType,
                                  String additionalFilter,
                                  boolean required,
                                  String beanName,
                                  Method targetMethod ) {
        super( bundleContext, publisher, serviceType, additionalFilter, beanName, targetMethod );
        this.required = required;
    }

    public void inject( BeanFactory beanFactory ) throws BundlePublishException {
        Object bean = beanFactory.getBean( this.beanName );
        try {
            this.targetMethod.invoke( bean, this.cache.getService() );
        } catch( Exception e ) {
            throw new BundlePublishException( "Could not inject service '" + this.cache.getService() + "' to method '" + this.targetMethod.getName() + "' of bean '" + this.beanName + "': " + e.getMessage(), e );
        }
    }

    @Override
    public Object addingService( ServiceReference<Object> serviceReference ) {
        Object newService = bundleContext.getService( serviceReference );

        if( this.cache == null ) {

            // this is the first service we've found to match our requirement - cache it
            this.cache = new ServiceCache( newService, serviceReference );

            // if published:
            //      TODO: inject cache reference to our bean
            // else
            //      TODO: trigger publish attempt

        }

        return newService;
    }

    @Override
    public void removedService( ServiceReference<Object> serviceReference, Object service ) {

        if( this.cache.getService() == service ) {

            // obtain a suitable replacement
            ServiceReference<Object> newServiceReference = this.tracker.getServiceReference();
            Object newService = this.bundleContext.getService( newServiceReference );
            if( newService != null ) {

                // a replacement was found, cache it
                this.cache = new ServiceCache( newService, newServiceReference );

                // if published:
                //      TODO: inject cache reference to our bean
                // else
                //      TODO: trigger publish attempt

            } else if( this.required ) {

                // no replacement was found - clear reference cache and inform publisher to un-publish bundle
                this.cache = null;

                // if published:
                //      TODO: trigger un-publish

            } else {

                // no replacement was found - clear our reference cache
                this.cache = null;

                // if published:
                //      TODO: inject null to our bean

            }
        }
    }

    public static class ServiceCache {

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

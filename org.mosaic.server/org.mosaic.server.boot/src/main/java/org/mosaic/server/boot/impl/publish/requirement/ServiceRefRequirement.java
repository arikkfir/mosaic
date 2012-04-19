package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.mosaic.osgi.util.ServiceUtils;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractTrackerRequirement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class ServiceRefRequirement extends AbstractTrackerRequirement {

    private final boolean required;

    private ServiceCache cache;

    public ServiceRefRequirement( BundleTracker tracker,
                                  Class<?> serviceType,
                                  String additionalFilter,
                                  boolean required,
                                  String beanName,
                                  Method targetMethod ) {
        super( tracker, serviceType, additionalFilter, beanName, targetMethod );
        this.required = required;
    }

    @Override
    public String toString() {
        return "ServiceRef[" +
               "type=" + getServiceType().getSimpleName() + ", " +
               "target-method=" + getTargetMethod().getName() + ", " +
               "target-bean=" + getBeanName() + ", " +
               "filter=" + getAdditionalFilter() +
               "]";
    }

    @Override
    public int getPriority() {
        return SERVICE_REF_PRIORITY;
    }

    @Override
    public String toShortString() {
        return getServiceType().getSimpleName();
    }

    @Override
    public Object addingService( ServiceReference<Object> serviceReference ) {
        Object newService = super.addingService( serviceReference );

        if( this.cache == null && newService != null ) {

            // this is the first service we've found to match our requirement - cache it
            this.cache = new ServiceCache( newService, serviceReference );

            // notify published that we're satisfied - publish if needed
            markAsSatisfied();

        }

        return newService;
    }

    @Override
    public void modifiedService( ServiceReference<Object> serviceReference, Object service ) {
        if( this.cache != null ) {

            if( this.cache.serviceReference.compareTo( serviceReference ) == 0 && this.cache.service == service ) {

                // re-wire; the bean already has it, but since the service is updated, we want to re-inject it to signal this
                markAsSatisfied();

            }

        }
    }

    @Override
    public void removedService( ServiceReference<Object> serviceReference, Object service ) {

        if( this.cache != null && this.cache.service == service ) {

            // obtain a suitable replacement
            ServiceReference<Object> newServiceReference = getTracker().getServiceReference();
            BundleContext bundleContext = getBundleContext();
            if( bundleContext == null ) {
                this.cache = null;
                return;
            }

            Object newService = newServiceReference == null ? null : bundleContext.getService( newServiceReference );
            if( newService != null ) {

                // a replacement was found, cache it
                this.cache = new ServiceCache( newService, newServiceReference );
                markAsSatisfied();

            } else {

                // no replacement was found - clear our reference cache
                this.cache = null;
                if( this.required ) {

                    // inform publisher to un-publish bundle since this is a required dependency
                    markAsUnsatisfied();

                } else {

                    // we're not required - just apply null to our bean
                    markAsSatisfied();

                }

            }
        }
    }

    @Override
    protected boolean trackInternal() throws Exception {
        super.trackInternal();
        return !this.required || this.cache != null;
    }

    @Override
    protected void onSatisfyInternal( ApplicationContext applicationContext, Object... state ) throws Exception {
        invoke( getBean( applicationContext ), getServiceMethodArgs() );
    }

    @Override
    protected void onInitBeanInternal( Object bean ) throws Exception {
        invoke( bean, getServiceMethodArgs() );
    }

    protected Object[] getServiceMethodArgs() {
        Method method = getTargetMethod();

        List<Object> values = new LinkedList<>();
        for( Class<?> type : method.getParameterTypes() ) {
            if( this.cache == null ) {
                values.add( null );
            } else if( type.isAssignableFrom( Map.class ) ) {
                values.add( ServiceUtils.getServiceProperties( this.cache.serviceReference ) );
            } else if( type.isAssignableFrom( ServiceReference.class ) ) {
                values.add( this.cache.serviceReference );
            } else if( type.isAssignableFrom( getServiceType() ) ) {
                values.add( this.cache.service );
            } else {
                throw new IllegalStateException( "Unsupported argument type ('" + type.getSimpleName() + "') in method '" + method.getName() + "' of bean '" + getBeanName() + "'" );
            }
        }
        return values.toArray();
    }

    private static class ServiceCache {

        private final ServiceReference<?> serviceReference;

        private final Object service;

        private ServiceCache( Object service, ServiceReference<?> serviceReference ) {
            this.serviceReference = serviceReference;
            this.service = service;
        }
    }
}

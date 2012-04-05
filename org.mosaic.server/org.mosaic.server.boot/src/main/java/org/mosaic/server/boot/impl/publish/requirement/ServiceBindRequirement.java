package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.osgi.framework.ServiceReference;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class ServiceBindRequirement extends ServiceRequirement {

    public ServiceBindRequirement( BundlePublisher publisher,
                                   Class<?> serviceType,
                                   String additionalFilter,
                                   String beanName,
                                   Method targetMethod ) {
        super( publisher, serviceType, additionalFilter, beanName, targetMethod );
    }

    @Override
    public Object addingService( ServiceReference<Object> serviceReference ) {
        Object service = super.addingService( serviceReference );
        if( service != null ) {
            markAsSatisfied( service );
        }
        return service;
    }

    @Override
    public void apply( ApplicationContext applicationContext, Object state ) throws Exception {
        targetMethod.invoke( applicationContext.getBean( this.beanName ), state );
    }

    @Override
    public void applyInitial( ApplicationContext applicationContext ) throws Exception {
        Object[] services = this.tracker.getServices();
        if( services != null ) {
            Object bean = applicationContext.getBean( this.beanName );
            for( Object service : services ) {
                targetMethod.invoke( bean, service );
            }
        }
    }

    @Override
    public void revert() throws Exception {
        // no-op
    }
}

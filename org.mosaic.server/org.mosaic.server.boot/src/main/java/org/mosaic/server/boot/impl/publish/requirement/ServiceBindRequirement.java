package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractTrackerRequirement;
import org.osgi.framework.ServiceReference;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class ServiceBindRequirement extends AbstractTrackerRequirement {

    public ServiceBindRequirement( BundlePublisher publisher,
                                   Class<?> serviceType,
                                   String additionalFilter,
                                   String beanName,
                                   Method targetMethod ) {
        super( publisher, serviceType, additionalFilter, beanName, targetMethod );
    }

    @Override
    public boolean open() {
        super.open();
        return true;
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
        invoke( applicationContext, state );
    }

    @Override
    public void applyInitial( ApplicationContext applicationContext ) throws Exception {
        Object[] services = getTracker().getServices();
        if( services != null ) {
            for( Object service : services ) {
                invoke( applicationContext, service );
            }
        }
    }
}

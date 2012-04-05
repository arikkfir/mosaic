package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractTrackerRequirement;
import org.osgi.framework.ServiceReference;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class ServiceUnbindRequirement extends AbstractTrackerRequirement {

    public ServiceUnbindRequirement( BundlePublisher publisher,
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
    public void removedService( ServiceReference<Object> serviceReference, Object service ) {
        // yes it's weird that in 'removedService' we're calling 'markAsSatisfied' but if you think about it - that's
        // the true meaning: we are still satisfied, and want to inform the publisher about it so we can inject to our bean
        markAsSatisfied( service );
    }

    @Override
    public void apply( ApplicationContext applicationContext, Object state ) throws Exception {
        invoke( applicationContext, state );
    }
}

package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author arik
 */
public class ServiceBindRequirement extends ServiceRequirement {

    public ServiceBindRequirement( BundleContext bundleContext,
                                   BundlePublisher publisher,
                                   Class<?> serviceType,
                                   String additionalFilter,
                                   String beanName,
                                   Method targetMethod ) {
        super( bundleContext, publisher, serviceType, additionalFilter, beanName, targetMethod );
    }

    @Override
    public Object addingService( ServiceReference<Object> serviceReference ) {
        // if published:
        //      TODO: inject reference to our bean
        return super.addingService( serviceReference );
    }
}

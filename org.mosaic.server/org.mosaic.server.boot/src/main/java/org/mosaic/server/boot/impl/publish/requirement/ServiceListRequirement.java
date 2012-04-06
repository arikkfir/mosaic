package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractTrackerRequirement;
import org.osgi.framework.ServiceReference;

/**
 * @author arik
 */
public class ServiceListRequirement extends AbstractTrackerRequirement {

    private final List<Object> cachedReferences = new CopyOnWriteArrayList<>();

    public ServiceListRequirement( BundlePublisher publisher,
                                   Class<?> serviceType,
                                   String additionalFilter,
                                   String beanName,
                                   Method targetMethod ) {
        super( publisher, serviceType, additionalFilter, beanName, targetMethod );
    }

    @Override
    public Object addingService( ServiceReference<Object> serviceReference ) {
        Object service = super.addingService( serviceReference );
        this.cachedReferences.add( service );
        return service;
    }

    @Override
    public void removedService( ServiceReference<Object> serviceReference, Object service ) {
        this.cachedReferences.remove( service );
    }

    @Override
    public boolean open() {
        super.open();
        return true;
    }

    @Override
    protected void onInitBeanInternal( Object bean ) throws Exception {
        invoke( bean, this.cachedReferences );
    }
}

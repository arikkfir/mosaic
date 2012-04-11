package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractTrackerRequirement;
import org.osgi.framework.ServiceReference;

/**
 * @author arik
 */
public class ServiceListRequirement extends AbstractTrackerRequirement {

    private final List<Object> cachedReferences = new CopyOnWriteArrayList<>();

    public ServiceListRequirement( BundleTracker tracker,
                                   Class<?> serviceType,
                                   String additionalFilter,
                                   String beanName,
                                   Method targetMethod ) {
        super( tracker, serviceType, additionalFilter, beanName, targetMethod );
        Class<?>[] parameterTypes = targetMethod.getParameterTypes();
        if( parameterTypes.length != 1 || !parameterTypes[ 0 ].isAssignableFrom( List.class ) ) {
            throw new IllegalArgumentException( "Method '" + getTargetMethod().getName() + "' is bean '" + beanName + "' has an illegal signature: must be single List parameter" );
        }
    }

    @Override
    public String toString() {
        return "ServiceList[" + getServiceType().getSimpleName() + "/" + getTargetMethod().getName() + "/" + getBeanName() + "]";
    }

    @Override
    public int getPriority() {
        return SERVICE_LIST_PRIORITY;
    }

    @Override
    public String toShortString() {
        return "List<" + getServiceType().getSimpleName() + ">";
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
    protected boolean trackInternal() throws Exception {
        super.trackInternal();
        return true;
    }

    @Override
    protected void onInitBeanInternal( Object bean ) throws Exception {
        invoke( bean, this.cachedReferences );
    }
}

package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.mosaic.server.boot.impl.publish.BundlePublishException;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author arik
 */
public class ServiceListRequirement extends ServiceRequirement {

    private final List<Object> cachedReferences = new CopyOnWriteArrayList<>();

    public ServiceListRequirement( BundleContext bundleContext,
                                   BundlePublisher publisher,
                                   Class<?> serviceType,
                                   String additionalFilter,
                                   String beanName,
                                   Method targetMethod ) {
        super( bundleContext, publisher, serviceType, additionalFilter, beanName, targetMethod );
    }

    public void inject( BeanFactory beanFactory ) throws BundlePublishException {
        Object bean = beanFactory.getBean( this.beanName );
        try {
            this.targetMethod.invoke( bean, this.cachedReferences );
        } catch( Exception e ) {
            throw new BundlePublishException( "Could not inject service list '" + this.cachedReferences + "' to method '" + this.targetMethod.getName() + "' of bean '" + this.beanName + "': " + e.getMessage(), e );
        }
    }

    @Override
    public Object addingService( ServiceReference<Object> serviceReference ) {
        Object newService = bundleContext.getService( serviceReference );
        this.cachedReferences.add( newService );
        return newService;
    }

    @Override
    public void removedService( ServiceReference<Object> serviceReference, Object service ) {
        this.cachedReferences.remove( service );
    }
}

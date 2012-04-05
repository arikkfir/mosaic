package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.osgi.framework.ServiceReference;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class ServiceListRequirement extends ServiceRequirement {

    private final List<Object> cachedReferences = new CopyOnWriteArrayList<>();

    private volatile boolean satisfied;

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
        if( !satisfied ) {
            this.satisfied = true;
            markAsSatisfied( service );
        }
        return super.addingService( serviceReference );
    }

    @Override
    public void removedService( ServiceReference<Object> serviceReference, Object service ) {
        this.cachedReferences.remove( service );
    }

    @Override
    public void apply( ApplicationContext applicationContext, Object state ) throws Exception {
        // no-op
    }

    @Override
    public void applyInitial( ApplicationContext applicationContext ) throws Exception {
        this.targetMethod.invoke( applicationContext.getBean( this.beanName ), this.cachedReferences );
    }

    @Override
    public void revert() throws Exception {
        // no-op
    }
}

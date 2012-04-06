package org.mosaic.server.boot.impl.publish.requirement.support;

import java.lang.reflect.Method;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import static org.osgi.framework.Constants.OBJECTCLASS;

/**
 * @author arik
 */
public abstract class AbstractTrackerRequirement extends AbstractMethodRequirement
        implements ServiceTrackerCustomizer<Object, Object> {

    private final ServiceTracker<Object, Object> tracker;

    private final Class<?> serviceType;

    public AbstractTrackerRequirement( BundlePublisher publisher,
                                       Class<?> serviceType,
                                       String additionalFilter,
                                       String beanName,
                                       Method targetMethod ) {
        super( publisher, beanName, targetMethod );
        this.serviceType = serviceType;

        Filter filter = createFilter( this.serviceType, additionalFilter );
        this.tracker = new ServiceTracker<>( getPublisher().getBundleContext(), filter, this );
    }

    @Override
    public boolean open() {
        this.tracker.open();
        return false;
    }

    @Override
    public void close() {
        this.tracker.close();
    }

    @Override
    public Object addingService( ServiceReference<Object> serviceReference ) {
        // no-op
        return getBundleContext().getService( serviceReference );
    }

    @Override
    public void modifiedService( ServiceReference<Object> serviceReference, Object service ) {
        // no-op
    }

    @Override
    public void removedService( ServiceReference<Object> serviceReference, Object service ) {
        // no-op
    }

    protected ServiceTracker<Object, Object> getTracker() {
        return tracker;
    }

    protected Class<?> getServiceType() {
        return serviceType;
    }

    private Filter createFilter( Class<?> serviceType, String additionalFilter ) {
        String classFilter = "(" + OBJECTCLASS + "=" + serviceType.getName() + ")";
        String filterString;
        if( additionalFilter != null && additionalFilter.trim().length() > 0 ) {
            filterString = "(&" + classFilter + "(" + additionalFilter + "))";
        } else {
            filterString = classFilter;
        }

        Filter filter;
        try {
            filter = FrameworkUtil.createFilter( filterString );
        } catch( InvalidSyntaxException e ) {
            throw new IllegalArgumentException( "Illegal filter: " + filterString, e );
        }
        return filter;
    }
}

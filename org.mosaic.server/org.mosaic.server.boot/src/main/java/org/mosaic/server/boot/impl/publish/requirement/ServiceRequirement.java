package org.mosaic.server.boot.impl.publish.requirement;

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
public abstract class ServiceRequirement implements Requirement, ServiceTrackerCustomizer<Object, Object> {

    protected final BundlePublisher publisher;

    protected final String beanName;

    protected final Method targetMethod;

    protected final ServiceTracker<Object, Object> tracker;

    public ServiceRequirement( BundlePublisher publisher,
                               Class<?> serviceType,
                               String additionalFilter,
                               String beanName,
                               Method targetMethod ) {
        this.publisher = publisher;
        this.beanName = beanName;
        this.targetMethod = targetMethod;
        this.tracker = new ServiceTracker<>( this.publisher.getBundleContext(), createFilter( serviceType, additionalFilter ), this );
    }

    @Override
    public void open() {
        this.tracker.open();
    }

    @Override
    public void close() {
        this.tracker.close();
    }

    @Override
    public Object addingService( ServiceReference<Object> serviceReference ) {
        // no-op
        return this.publisher.getBundleContext().getService( serviceReference );
    }

    @Override
    public void modifiedService( ServiceReference<Object> serviceReference, Object service ) {
        // no-op
    }

    @Override
    public void removedService( ServiceReference<Object> serviceReference, Object service ) {
        // no-op
    }

    protected void markAsSatisfied( Object state ) {
        this.publisher.markAsSatisfied( this, state );
    }

    protected void markAsUnsatisfied() {
        this.publisher.markAsUnsatisfied( this );
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

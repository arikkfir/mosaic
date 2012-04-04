package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import static org.osgi.framework.Constants.OBJECTCLASS;

/**
 * @author arik
 */
public abstract class ServiceRequirement implements ServiceTrackerCustomizer<Object, Object> {

    protected final BundleContext bundleContext;

    protected final BundlePublisher publisher;

    protected final String beanName;

    protected final Method targetMethod;

    protected final ServiceTracker<Object, Object> tracker;

    public ServiceRequirement( BundleContext bundleContext,
                               BundlePublisher publisher,
                               Class<?> serviceType,
                               String additionalFilter,
                               String beanName,
                               Method targetMethod ) {
        this.bundleContext = bundleContext;
        this.publisher = publisher;
        this.beanName = beanName;
        this.targetMethod = targetMethod;
        this.tracker = new ServiceTracker<>( this.bundleContext, createFilter( serviceType, additionalFilter ), this );
    }

    public void open() {
        this.tracker.open();
    }

    @Override
    public Object addingService( ServiceReference<Object> serviceReference ) {
        // no-op
        return this.bundleContext.getService( serviceReference );
    }

    @Override
    public void modifiedService( ServiceReference<Object> serviceReference, Object service ) {
        // no-op
    }

    @Override
    public void removedService( ServiceReference<Object> serviceReference, Object service ) {
        // no-op
    }

    public void close() {
        this.tracker.close();
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

package org.mosaic.server.boot.impl.publish.requirement.support;

import java.lang.reflect.Method;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.osgi.framework.*;
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

    private final String additionalFilter;

    public AbstractTrackerRequirement( BundleTracker tracker,
                                       Class<?> serviceType,
                                       String additionalFilter,
                                       String beanName,
                                       Method targetMethod ) {
        super( tracker, beanName, targetMethod );
        this.serviceType = serviceType;
        this.additionalFilter = additionalFilter;

        Filter filter = createFilter( this.serviceType, additionalFilter );
        this.tracker = new ServiceTracker<>( getBundleContext(), filter, this );
    }

    @Override
    protected boolean trackInternal() throws Exception {
        this.tracker.open();
        return false;
    }

    @Override
    protected void untrackInternal() throws Exception {
        this.tracker.close();
    }

    @Override
    public Object addingService( ServiceReference<Object> serviceReference ) {
        // no-op
        BundleContext bundleContext = getBundleContext();
        return bundleContext == null ? null : bundleContext.getService( serviceReference );
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

    protected String getAdditionalFilter() {
        return additionalFilter;
    }

    private Filter createFilter( Class<?> serviceType, String additionalFilter ) {
        String classFilter = "(" + OBJECTCLASS + "=" + serviceType.getName() + ")";
        String filterString;
        if( additionalFilter != null && additionalFilter.trim().length() > 0 ) {
            if( !additionalFilter.startsWith( "(" ) ) {
                additionalFilter = "(" + additionalFilter + ")";
            }
            filterString = "(&" + classFilter + additionalFilter + ")";
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

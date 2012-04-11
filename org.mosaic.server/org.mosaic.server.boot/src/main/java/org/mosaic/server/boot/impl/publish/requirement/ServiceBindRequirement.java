package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.mosaic.logging.Logger;
import org.mosaic.logging.LoggerFactory;
import org.mosaic.osgi.util.ServiceUtils;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractTrackerRequirement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class ServiceBindRequirement extends AbstractTrackerRequirement {

    private static final Logger LOG = LoggerFactory.getBundleLogger( ServiceBindRequirement.class );

    public ServiceBindRequirement( BundleTracker tracker,
                                   Class<?> serviceType,
                                   String additionalFilter,
                                   String beanName,
                                   Method targetMethod ) {
        super( tracker, serviceType, additionalFilter, beanName, targetMethod );
    }

    @Override
    public String toString() {
        return "ServiceBind[" + getServiceType().getSimpleName() + "/" + getTargetMethod().getName() + "/" + getBeanName() + "]";
    }

    @Override
    public int getPriority() {
        return SERVICE_BIND_PRIORITY;
    }

    @Override
    public String toShortString() {
        return "Bind '" + getServiceType().getSimpleName() + "'";
    }

    @Override
    public Object addingService( ServiceReference<Object> serviceReference ) {
        Object service = super.addingService( serviceReference );
        if( service != null ) {
            markAsSatisfied( serviceReference, service );
        }
        return service;
    }

    @Override
    protected boolean trackInternal() throws Exception {
        super.trackInternal();
        return true;
    }

    @Override
    protected void onSatisfyInternal( ApplicationContext applicationContext, Object... state ) throws Exception {
        Object bean = getBean( applicationContext );
        ServiceReference<?> serviceReference = ( ServiceReference<?> ) state[ 0 ];
        Object service = state[ 1 ];
        invoke( bean, getServiceMethodArgs( serviceReference, service ) );
    }

    @Override
    protected void onInitBeanInternal( Object bean ) throws Exception {
        ServiceReference<Object>[] serviceReferences = getTracker().getServiceReferences();
        if( serviceReferences != null ) {
            for( ServiceReference<Object> serviceReference : serviceReferences ) {
                Object[] args = getServiceMethodArgs( serviceReference );
                if( args != null ) {
                    invoke( bean, args );
                } else {
                    LOG.warn( "Initializing bean when bundle is not active?? For bundle: {}", getBundleName() );
                }
            }
        }
    }

    protected Object[] getServiceMethodArgs( ServiceReference<?> sr ) {
        BundleContext bundleContext = getBundleContext();
        return bundleContext == null ? null : getServiceMethodArgs( sr, bundleContext.getService( sr ) );
    }

    protected Object[] getServiceMethodArgs( ServiceReference<?> sr, Object service ) {
        Method method = getTargetMethod();

        List<Object> values = new LinkedList<>();
        for( Class<?> type : method.getParameterTypes() ) {
            if( type.isAssignableFrom( Map.class ) ) {
                values.add( ServiceUtils.getServiceProperties( sr ) );
            } else if( type.isAssignableFrom( ServiceReference.class ) ) {
                values.add( sr );
            } else if( type.isAssignableFrom( getServiceType() ) ) {
                values.add( service );
            } else {
                throw new IllegalStateException( "Unsupported argument type ('" + type.getSimpleName() + "') in method '" + method.getName() + "' of bean '" + getBeanName() + "'" );
            }
        }
        return values.toArray();
    }
}

package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.mosaic.osgi.util.ServiceUtils;
import org.mosaic.server.boot.impl.publish.BundlePublisher;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractTrackerRequirement;
import org.osgi.framework.ServiceReference;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class ServiceBindRequirement extends AbstractTrackerRequirement {

    public ServiceBindRequirement( BundlePublisher publisher,
                                   Class<?> serviceType,
                                   String additionalFilter,
                                   String beanName,
                                   Method targetMethod ) {
        super( publisher, serviceType, additionalFilter, beanName, targetMethod );
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
    public boolean open() {
        super.open();
        return true;
    }

    @Override
    public void onSatisfy( ApplicationContext applicationContext, Object... state ) throws Exception {
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
                invoke( bean, getServiceMethodArgs( serviceReference ) );
            }
        }
    }

    protected Object[] getServiceMethodArgs( ServiceReference<?> sr ) {
        return getServiceMethodArgs( sr, getBundleContext().getService( sr ) );
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

package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractTrackerRequirement;
import org.mosaic.server.osgi.util.ServiceUtils;
import org.osgi.framework.ServiceReference;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class ServiceUnbindRequirement extends AbstractTrackerRequirement
{

    public ServiceUnbindRequirement( BundleTracker tracker,
                                     Class<?> serviceType,
                                     String additionalFilter,
                                     String beanName,
                                     Method targetMethod )
    {
        super( tracker, serviceType, additionalFilter, beanName, targetMethod );
    }

    @Override
    public String toString( )
    {
        return "ServiceUnbind[" +
               getServiceType( ).getSimpleName( ) +
               "/" +
               getTargetMethod( ).getName( ) +
               "/" +
               getBeanName( ) +
               "]";
    }

    @Override
    public int getPriority( )
    {
        return SERVICE_UNBIND_PRIORITY;
    }

    @Override
    public String toShortString( )
    {
        return "Unbind of '" + getServiceType( ).getSimpleName( ) + "'";
    }

    @Override
    public void removedService( ServiceReference<Object> serviceReference, Object service )
    {
        // yes it's weird that in 'removedService' we're calling 'markAsSatisfied' but if you think about it - that's
        // the true meaning: we are still satisfied, and want to inform the publisher about it so we can inject to our bean
        markAsSatisfied( serviceReference, service );
    }

    @Override
    protected boolean trackInternal( ) throws Exception
    {
        super.trackInternal( );
        return true;
    }

    @Override
    protected void onSatisfyInternal( ApplicationContext applicationContext, Object... state ) throws Exception
    {
        Object bean = getBean( applicationContext );
        ServiceReference<?> serviceReference = ( ServiceReference<?> ) state[ 0 ];
        Object service = state[ 1 ];
        invoke( bean, getServiceMethodArgs( serviceReference, service ) );
    }

    protected Object[] getServiceMethodArgs( ServiceReference<?> sr, Object service )
    {
        Method method = getTargetMethod( );

        List<Object> values = new LinkedList<>( );
        for( Class<?> type : method.getParameterTypes( ) )
        {
            if( type.isAssignableFrom( Map.class ) )
            {
                values.add( ServiceUtils.getServiceProperties( sr ) );
            }
            else if( type.isAssignableFrom( ServiceReference.class ) )
            {
                values.add( sr );
            }
            else if( type.isAssignableFrom( getServiceType( ) ) )
            {
                values.add( service );
            }
            else
            {
                throw new IllegalStateException( "Unsupported argument type ('" +
                                                 type.getSimpleName( ) +
                                                 "') in method '" +
                                                 method.getName( ) +
                                                 "' of bean '" +
                                                 getBeanName( ) +
                                                 "'" );
            }
        }
        return values.toArray( );
    }
}

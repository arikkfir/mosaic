package org.mosaic.server.boot.impl.publish.requirement;

import java.lang.reflect.Method;
import org.mosaic.server.boot.impl.publish.BundleTracker;
import org.mosaic.server.boot.impl.publish.requirement.support.AbstractMethodRequirement;
import org.springframework.context.ApplicationContext;

/**
 * @author arik
 */
public class BundleContextRequirement extends AbstractMethodRequirement
{

    public BundleContextRequirement( BundleTracker tracker, String beanName, Method targetMethod )
    {
        super( tracker, beanName, targetMethod );
    }

    @Override
    public String toString( )
    {
        return "BundleContextBind[" + getTargetMethod( ).getName( ) + "/" + getBeanName( ) + "]";
    }

    @Override
    public int getPriority( )
    {
        return BUNDLE_CONTEXT_PRIORITY;
    }

    @Override
    public String toShortString( )
    {
        return "Bind BundleContext";
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
        invoke( getBean( applicationContext ), getBundleContext( ) );
    }

    @Override
    protected void onInitBeanInternal( Object bean ) throws Exception
    {
        invoke( bean, getBundleContext( ) );
    }
}

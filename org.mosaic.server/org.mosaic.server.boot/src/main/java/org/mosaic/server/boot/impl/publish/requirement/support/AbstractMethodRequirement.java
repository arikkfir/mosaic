package org.mosaic.server.boot.impl.publish.requirement.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.mosaic.server.boot.impl.publish.BundleTracker;

/**
 * @author arik
 */
public abstract class AbstractMethodRequirement extends AbstractBeanRequirement
{

    private final Method targetMethod;

    protected AbstractMethodRequirement( BundleTracker tracker, String beanName, Method targetMethod )
    {
        super( tracker, beanName );
        this.targetMethod = targetMethod;
    }

    protected Method getTargetMethod( )
    {
        return targetMethod;
    }

    protected void invoke( Object bean, Object... args ) throws Exception
    {
        try
        {
            this.targetMethod.invoke( bean, args );
        }
        catch( InvocationTargetException e )
        {
            Throwable cause = e.getCause( );
            if( cause instanceof Exception )
            {
                throw ( Exception ) cause;
            }
            else
            {
                throw e;
            }
        }
    }
}

package org.mosaic.lifecycle.impl.metrics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleManager;
import org.mosaic.lifecycle.annotation.BeanRef;
import org.mosaic.lifecycle.annotation.Measure;
import org.mosaic.lifecycle.annotation.Rank;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.util.weaving.MethodInterceptor;
import org.mosaic.util.weaving.MethodInvocation;

/**
 * @author arik
 */
@Service( MethodInterceptor.class )
@Rank( Integer.MIN_VALUE )
public class MetricsMethodInterceptor implements MethodInterceptor
{
    private ModuleManager moduleManager;

    @BeanRef
    public void setModuleManager( ModuleManager moduleManager )
    {
        this.moduleManager = moduleManager;
    }

    @Nullable
    @Override
    public Object intercept( @Nonnull MethodInvocation invocation ) throws Exception
    {
        Measure measure = invocation.getMethodHandle().getAnnotation( Measure.class );
        if( measure != null )
        {
            Object object = invocation.getObject();
            if( object == null )
            {
                object = invocation.getMethodHandle().getDeclaringClass();
            }

            Module module = this.moduleManager.getModuleFor( object );
            if( module != null )
            {
                Module.Metrics metrics = module.getMetrics();
                if( metrics != null )
                {
                    Module.MetricsTimer timer = metrics.getTimer( invocation.getMethodHandle() );
                    timer.startTimer();
                    try
                    {
                        return invocation.proceed();
                    }
                    finally
                    {
                        timer.stopTimer();
                    }
                }
            }
        }
        return invocation.proceed();
    }
}

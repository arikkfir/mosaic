package org.mosaic.metrics.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.Module;
import org.mosaic.lifecycle.ModuleManager;
import org.mosaic.lifecycle.annotation.Measure;
import org.mosaic.lifecycle.annotation.Service;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.weaving.MethodInterceptor;

/**
 * @author arik
 */
@Service(MethodInterceptor.class)
public class MetricsMethodInterceptor implements MethodInterceptor
{
    @Nonnull
    private ModuleManager moduleManager;

    @ServiceRef
    public void setModuleManager( @Nonnull ModuleManager moduleManager )
    {
        this.moduleManager = moduleManager;
    }

    @Nullable
    @Override
    public Object intercept( @Nonnull MethodInvocation invocation ) throws Throwable
    {
        MethodHandle methodHandle = invocation.getMethodHandle();
        Class<?> declaringClass = methodHandle.getDeclaringClass();

        if( methodHandle.hasAnnotation( Measure.class ) )
        {
            Module module = this.moduleManager.getModuleFor( declaringClass );
            if( module == null )
            {
                throw new IllegalStateException( "Type '" + declaringClass + "' was not defined from a Mosaic module" );
            }

            Module.Metrics metrics = module.getMetrics();
            if( metrics == null )
            {
                return invocation.proceed();
            }

            Module.MetricsTimer timer = metrics.getTimer( declaringClass.getPackage().getName(), declaringClass.getSimpleName(), methodHandle.getName() );
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
        return invocation.proceed();
    }
}

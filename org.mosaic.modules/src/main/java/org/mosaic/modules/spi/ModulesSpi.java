package org.mosaic.modules.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.ComponentCreateException;
import org.mosaic.modules.ComponentDescriptor;
import org.mosaic.modules.Module;
import org.mosaic.modules.impl.Activator;

/**
 * @author arik
 */
public final class ModulesSpi
{
    @Nonnull
    private static final MethodInterceptorManager methodInterceptorManager = new MethodInterceptorManager();

    @Nullable
    public static Object getValueForField( long moduleId, @Nonnull Class<?> componentType, @Nonnull String fieldName )
    {
        Module module = Activator.getModuleManager().getModule( moduleId );
        if( module == null )
        {
            throw new IllegalArgumentException( "no module with id " + moduleId + " found" );
        }

        ComponentDescriptor<?> componentDescriptor = module.getModuleComponents().getComponentDescriptor( componentType );
        if( componentDescriptor == null )
        {
            throw new ComponentCreateException( "could not find component of type '" + componentType.getName() + "'", componentType, module );
        }
        else
        {
            return componentDescriptor.getValueForField( fieldName );
        }
    }

    public static boolean beforeInvocation( long id, @Nullable Object object, @Nonnull Object[] arguments )
            throws Throwable
    {
        return ModulesSpi.methodInterceptorManager.beforeInvocation( id, object, arguments );
    }

    @Nullable
    public static Object afterAbortedInvocation() throws Throwable
    {
        return ModulesSpi.methodInterceptorManager.afterAbortedInvocation();
    }

    @Nullable
    public static Object afterSuccessfulInvocation( @Nullable Object returnValue ) throws Throwable
    {
        return ModulesSpi.methodInterceptorManager.afterSuccessfulInvocation( returnValue );
    }

    @Nullable
    public static Object afterThrowable( @Nonnull Throwable throwable ) throws Throwable
    {
        return ModulesSpi.methodInterceptorManager.afterThrowable( throwable );
    }

    public static void cleanup( long id ) throws Throwable
    {
        ModulesSpi.methodInterceptorManager.cleanup( id );
    }

    private ModulesSpi()
    {
    }
}

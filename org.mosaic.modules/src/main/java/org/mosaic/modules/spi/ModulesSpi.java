package org.mosaic.modules.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.ComponentCreateException;
import org.mosaic.modules.Module;
import org.mosaic.modules.TypeDescriptor;
import org.mosaic.modules.impl.Activator;

/**
 * @author arik
 */
public final class ModulesSpi
{
    @Nonnull
    private static final MethodInterceptorManager methodInterceptorManager = new MethodInterceptorManager();

    @Nullable
    public static Object getValueForField( long moduleId, @Nonnull Class<?> type, @Nonnull String fieldName )
    {
        Module module = Activator.getModuleManager().getModule( moduleId ).get();
        TypeDescriptor typeDescriptor = module.getTypeDescriptor( type );
        if( typeDescriptor == null )
        {
            throw new ComponentCreateException( "could not find descriptor for type '" + type.getName() + "' in module " + module, type, module );
        }
        else
        {
            return typeDescriptor.getValueForField( fieldName );
        }
    }

    public static boolean beforeInvocation( @Nonnull MethodEntry methodEntry,
                                            @Nullable Object object,
                                            @Nonnull Object[] arguments )
            throws Throwable
    {
        return ModulesSpi.methodInterceptorManager.beforeInvocation( methodEntry, object, arguments );
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

    public static void cleanup( @Nonnull MethodEntry methodEntry ) throws Throwable
    {
        ModulesSpi.methodInterceptorManager.cleanup( methodEntry );
    }

    private ModulesSpi()
    {
    }
}

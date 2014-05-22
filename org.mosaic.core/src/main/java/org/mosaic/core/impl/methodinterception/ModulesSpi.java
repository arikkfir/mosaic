package org.mosaic.core.impl.methodinterception;

import org.mosaic.core.Module;
import org.mosaic.core.ModuleManager;
import org.mosaic.core.ModuleRevision;
import org.mosaic.core.ModuleType;
import org.mosaic.core.impl.Activator;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

/**
 * @author arik
 */
public final class ModulesSpi
{
    @SuppressWarnings({ "UnusedDeclaration", "unchecked" })
    @Nullable
    public static <T> T getInstanceFieldValue( long moduleId,
                                               long revisionId,
                                               @Nonnull Class<T> declaringType,
                                               @Nonnull String fieldName )
    {
        Module module = findModuleManager().getModule( moduleId );
        if( module == null )
        {
            throw new IllegalStateException( "could not find module " + moduleId + " for type " + declaringType.getName() );
        }

        ModuleRevision moduleRevision = module.getRevision( revisionId );
        if( moduleRevision == null )
        {
            throw new IllegalStateException( "could not find module revision " + moduleId + "." + revisionId + " for type " + declaringType.getName() );
        }

        ModuleType moduleType = moduleRevision.getType( declaringType );
        if( moduleType == null )
        {
            throw new IllegalStateException( "could not find type " + declaringType.getName() + " in revision " + moduleId + "." + revisionId );
        }

        return ( T ) moduleType.getInstanceFieldValue( fieldName );
    }

    @SuppressWarnings("UnusedDeclaration")
    public static boolean beforeInvocation( @Nonnull MethodEntry methodEntry,
                                            @Nullable Object object,
                                            @Nonnull Object[] arguments )
            throws Throwable
    {
        return findMethodInterceptorsManager().beforeInvocation( methodEntry, object, arguments );
    }

    @SuppressWarnings("UnusedDeclaration")
    @Nullable
    public static Object afterAbortedInvocation() throws Throwable
    {
        return findMethodInterceptorsManager().afterAbortedInvocation();
    }

    @SuppressWarnings( "UnusedDeclaration" )
    @Nullable
    public static Object afterSuccessfulInvocation( @Nullable Object returnValue ) throws Throwable
    {
        return findMethodInterceptorsManager().afterSuccessfulInvocation( returnValue );
    }

    @SuppressWarnings("UnusedDeclaration")
    @Nullable
    public static Object afterThrowable( @Nonnull Throwable throwable ) throws Throwable
    {
        return findMethodInterceptorsManager().afterThrowable( throwable );
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void cleanup( @Nonnull MethodEntry methodEntry ) throws Throwable
    {
        findMethodInterceptorsManager().cleanup( methodEntry );
    }

    @Nonnull
    private static ModuleManager findModuleManager()
    {
        ModuleManager moduleManager = Activator.getModuleManager();
        if( moduleManager == null )
        {
            throw new IllegalStateException( "Mosaic server is not available" );
        }
        else
        {
            return moduleManager;
        }
    }

    @Nonnull
    private static MethodInterceptorsManager findMethodInterceptorsManager()
    {
        MethodInterceptorsManager methodInterceptorsManager = Activator.getMethodInterceptorsManager();
        if( methodInterceptorsManager == null )
        {
            throw new IllegalStateException( "Mosaic server is not available" );
        }
        else
        {
            return methodInterceptorsManager;
        }
    }

    private ModulesSpi()
    {
    }
}

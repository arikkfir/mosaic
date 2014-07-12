package org.mosaic.core.weaving;

import org.mosaic.core.modules.Module;
import org.mosaic.core.modules.ModuleManager;
import org.mosaic.core.modules.ModuleRevision;
import org.mosaic.core.modules.ModuleType;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * @author arik
 */
public final class WeavingSpi
{
    @Nullable
    private static ModuleManager moduleManager;

    @Nullable
    private static MethodInterceptorsManager methodInterceptorsManager;

    public static void setModuleManager( @Nullable ModuleManager moduleManager )
    {
        WeavingSpi.moduleManager = moduleManager;
    }

    public static void setMethodInterceptorsManager( @Nullable MethodInterceptorsManager methodInterceptorsManager )
    {
        WeavingSpi.methodInterceptorsManager = methodInterceptorsManager;
    }

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
                                            @Nonnull Object... arguments )
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

    @SuppressWarnings("UnusedDeclaration")
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
        return requireNonNull( WeavingSpi.moduleManager, "server not started" );
    }

    @Nonnull
    private static MethodInterceptorsManager findMethodInterceptorsManager()
    {
        return requireNonNull( WeavingSpi.methodInterceptorsManager, "server not started" );
    }

    private WeavingSpi()
    {
    }
}

package org.mosaic.util.weaving;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.reflection.MethodHandle;

/**
 * @author arik
 */
public interface MethodInvocation
{
    @Nonnull
    MethodHandle getMethodHandle();

    @Nullable
    Object getObject();

    @Nonnull
    Object[] getArguments();

    @Nullable
    Object proceed() throws Exception;

    @Nullable
    Object proceed( @Nonnull Object[] arguments ) throws Exception;
}

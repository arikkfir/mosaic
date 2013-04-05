package org.mosaic.util.weaving;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface MethodInterceptor
{
    @Nullable
    Object intercept( @Nonnull MethodInvocation invocation ) throws Exception;
}

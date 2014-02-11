package org.mosaic.web.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface RequestInterceptor
{
    boolean canHandle( @Nonnull WebInvocation request, @Nonnull RequestHandler requestHandler );

    @Nullable
    Object handle( @Nonnull WebInvocation invocation, @Nonnull InterceptorChain interceptorChain ) throws Throwable;
}

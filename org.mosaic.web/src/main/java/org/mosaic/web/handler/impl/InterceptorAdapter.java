package org.mosaic.web.handler.impl;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.web.handler.InterceptorChain;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.request.WebInvocation;

/**
 * @author arik
 */
abstract class InterceptorAdapter
{
    @Nonnull
    abstract Set<String> getHttpMethods();

    abstract boolean canHandle( @Nonnull WebInvocation request, @Nonnull RequestHandler requestHandler );

    @Nullable
    abstract Object handle( @Nonnull WebInvocation request, @Nonnull InterceptorChain interceptorChain )
            throws Throwable;
}

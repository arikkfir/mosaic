package org.mosaic.web.server.impl;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.web.server.InterceptorChain;
import org.mosaic.web.server.RequestHandler;
import org.mosaic.web.server.WebInvocation;

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

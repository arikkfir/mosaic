package org.mosaic.web.impl;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.web.handler.InterceptorChain;
import org.mosaic.web.handler.RequestHandler;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
abstract class InterceptorAdapter
{
    @Nonnull
    abstract Set<String> getHttpMethods();

    abstract boolean canHandle( @Nonnull WebRequest request, @Nonnull RequestHandler requestHandler );

    @Nullable
    abstract Object handle( @Nonnull WebRequest request, @Nonnull InterceptorChain interceptorChain ) throws Throwable;
}

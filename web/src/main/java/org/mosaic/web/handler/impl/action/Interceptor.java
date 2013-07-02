package org.mosaic.web.handler.impl.action;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.web.handler.InterceptorChain;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public interface Interceptor extends Participator
{
    @Nullable
    Object handle( @Nonnull WebRequest request,
                   @Nonnull InterceptorChain interceptorChain,
                   @Nonnull MapEx<String, Object> context ) throws Exception;
}

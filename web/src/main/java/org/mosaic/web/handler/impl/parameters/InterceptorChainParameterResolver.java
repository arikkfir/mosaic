package org.mosaic.web.handler.impl.parameters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.handler.InterceptorChain;

/**
 * @author arik
 */
public class InterceptorChainParameterResolver extends CheckedParameterResolver<InterceptorChain>
{
    @Nullable
    @Override
    protected InterceptorChain resolveToType( @Nonnull MethodParameter parameter,
                                              @Nonnull MapEx<String, Object> context ) throws Exception
    {
        return context.require( "interceptorChain", InterceptorChain.class );
    }
}

package org.mosaic.server.web.dispatcher.impl.handler.parameters;

import org.mosaic.web.HttpRequest;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class HttpRequestResolver implements MethodParameterResolver, MethodParameterResolver.ResolvedParameter
{

    @Override
    public Object resolve( HttpRequest request )
    {
        return request;
    }

    @Override
    public ResolvedParameter resolve( MethodParameterInfo methodParameterInfo )
    {
        return this;
    }
}

package org.mosaic.server.web.dispatcher.impl.handler.parameters;

import org.mosaic.web.HttpRequest;
import org.springframework.core.MethodParameter;
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
    public ResolvedParameter resolve( MethodParameter methodParameter )
    {
        if( methodParameter.getParameterType( ).isAssignableFrom( HttpRequest.class ) )
        {
            return this;
        }
        else
        {
            return null;
        }
    }
}

package org.mosaic.server.web.dispatcher.impl.handler.parameters;

import org.mosaic.web.HttpRequest;
import org.springframework.core.MethodParameter;

/**
 * @author arik
 */
public interface MethodParameterResolver
{

    interface ResolvedParameter
    {

        Object resolve( HttpRequest request );

    }

    ResolvedParameter resolve( MethodParameter methodParameter );

}

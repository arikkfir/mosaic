package org.mosaic.server.web.dispatcher.impl.handler.parameters;

import org.mosaic.server.web.dispatcher.impl.RequestExecutionPlan;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.InterceptorChain;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class RequestExecutionPlanResolver implements MethodParameterResolver, MethodParameterResolver.ResolvedParameter
{
    @Override
    public ResolvedParameter resolve( MethodParameter methodParameter )
    {
        if( methodParameter.getParameterType( ).isAssignableFrom( InterceptorChain.class ) )
        {
            return this;
        }
        else
        {
            return null;
        }
    }

    @Override
    public Object resolve( HttpRequest request )
    {
        return request.getValueAs( RequestExecutionPlan.class.getName( ), RequestExecutionPlan.class );
    }
}

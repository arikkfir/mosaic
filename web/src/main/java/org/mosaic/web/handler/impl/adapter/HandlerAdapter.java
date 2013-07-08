package org.mosaic.web.handler.impl.adapter;

import javax.annotation.Nonnull;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.web.handler.annotation.Method;
import org.mosaic.web.handler.annotation.Secured;
import org.mosaic.web.handler.annotation.WebAppFilter;
import org.mosaic.web.handler.impl.action.Handler;
import org.mosaic.web.handler.impl.action.MethodEndpointHandler;
import org.mosaic.web.handler.impl.action.SecuredHandler;

/**
 * @author arik
 */
public class HandlerAdapter extends RequestAdapter
{
    public HandlerAdapter( @Nonnull ConversionService conversionService,
                           @Nonnull ExpressionParser expressionParser,
                           long id,
                           int rank,
                           @Nonnull MethodEndpoint endpoint )
    {
        super( conversionService, id );
        setRank( rank );

        addHttpMethodFilter( endpoint.getAnnotation( Method.class ) );
        addWebAppFilter( endpoint.getAnnotation( WebAppFilter.class ) );
        addPathFilter( endpoint.getType(), false );

        Handler handler = new MethodEndpointHandler( endpoint, conversionService );
        Secured securedAnn = endpoint.getAnnotation( Secured.class );
        if( securedAnn != null )
        {
            String securityExpressionString = securedAnn.value();
            if( !securityExpressionString.trim().isEmpty() )
            {
                handler = new SecuredHandler( handler, expressionParser.parseExpression( securityExpressionString ) );
            }
        }
        setParticipator( handler );
    }
}

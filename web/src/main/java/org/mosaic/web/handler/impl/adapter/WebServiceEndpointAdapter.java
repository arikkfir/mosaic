package org.mosaic.web.handler.impl.adapter;

import java.lang.reflect.InvocationTargetException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.ExpressionParser;

/**
 * @author arik
 */
public class WebServiceEndpointAdapter extends RequestHandlerEndpointAdapter
{
    public WebServiceEndpointAdapter( long id,
                                      int rank,
                                      @Nonnull MethodEndpoint endpoint,
                                      @Nonnull ExpressionParser expressionParser,
                                      @Nonnull ConversionService conversionService )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        super( id, rank, endpoint, expressionParser, conversionService );
    }

    @Nullable
    public Object handle( @Nonnull HandlerContext context ) throws Exception
    {
        // TODO arik: wrap return value with a service response
        return super.handle( context );
    }
}

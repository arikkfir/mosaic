package org.mosaic.web.handler.impl.adapter;

import java.lang.reflect.InvocationTargetException;
import javax.annotation.Nonnull;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.ExpressionParser;

/**
 * @author arik
 */
public class ControllerEndpointAdapter extends RequestHandlerEndpointAdapter
{
    public ControllerEndpointAdapter( long id,
                                      int rank,
                                      @Nonnull MethodEndpoint endpoint,
                                      @Nonnull ExpressionParser expressionParser,
                                      @Nonnull ConversionService conversionService )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        super( id, rank, endpoint, expressionParser, conversionService );
    }
}

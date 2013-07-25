package org.mosaic.web.handler.impl.adapter;

import javax.annotation.Nonnull;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.web.handler.annotation.Method;
import org.mosaic.web.handler.annotation.WebAppFilter;
import org.mosaic.web.handler.impl.action.MethodEndpointInterceptor;

/**
 * @author arik
 */
public class InterceptorAdapter extends RequestAdapter
{
    public InterceptorAdapter( @Nonnull ConversionService conversionService,
                               long id,
                               int rank,
                               @Nonnull MethodEndpoint endpoint )
    {
        super( conversionService, id );
        setRank( rank );
        addHttpMethodFilter( endpoint.getAnnotation( Method.class, true, true ) );
        addWebAppFilter( endpoint.getAnnotation( WebAppFilter.class, true, true ) );
        addPathFilter( endpoint.getType(), false );
        setParticipator( new MethodEndpointInterceptor( endpoint, conversionService ) );
    }
}

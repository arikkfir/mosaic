package org.mosaic.server.web.dispatcher.impl.handler;

import java.util.List;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.server.web.dispatcher.impl.handler.parameters.MethodParameterResolver;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.web.handler.annotation.Service;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class InterceptorsManager
{

    private static final Logger LOG = LoggerFactory.getLogger( InterceptorsManager.class );

    private ConversionService conversionService;

    private List<MethodParameterResolver> methodParameterResolvers;

    @Autowired
    public void setConversionService( ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @Autowired
    public void setMethodParameterResolvers( List<MethodParameterResolver> methodParameterResolvers )
    {
        this.methodParameterResolvers = methodParameterResolvers;
    }

    @ServiceBind( filter = "methodEndpointShortType=Interceptor" )
    public void addInterceptor( ServiceReference<?> ref, MethodEndpointInfo endpointInfo )
    {
        this.interceptors.put( ref, new MethodEndpointHandler( endpointInfo, Service.class ) );
    }

    @ServiceUnbind( filter = "methodEndpointShortType=Interceptor" )
    public void removeInterceptor( ServiceReference<?> ref )
    {
        this.interceptors.remove( ref );
    }

}

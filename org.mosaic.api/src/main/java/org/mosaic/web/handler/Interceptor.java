package org.mosaic.web.handler;

import org.mosaic.web.HttpRequest;

/**
 * @author arik
 */
public interface Interceptor
{
    interface InterceptorMatch { }

    interface InterceptorChain
    {
        Object next( ) throws Exception;
    }

    InterceptorMatch matches( HttpRequest request );

    Object handle( HttpRequest request, InterceptorMatch match, InterceptorChain chain ) throws Exception;

}

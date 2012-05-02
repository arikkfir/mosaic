package org.mosaic.web.handler;

import org.mosaic.web.HttpRequest;

/**
 * @author arik
 */
public interface Interceptor
{

    interface InterceptorMatch
    {

    }

    InterceptorMatch matches( HttpRequest request );

    //TODO 5/1/12: add chain parameter here
    Object handle( HttpRequest request, InterceptorMatch match );

}

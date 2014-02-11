package org.mosaic.web.server.impl;

import com.google.common.base.Optional;
import java.util.*;
import javax.annotation.Nonnull;
import org.mosaic.modules.Component;
import org.mosaic.modules.Service;
import org.mosaic.modules.ServiceReference;
import org.mosaic.util.reflection.TypeTokens;
import org.mosaic.web.server.RequestHandler;
import org.mosaic.web.server.RequestInterceptor;
import org.mosaic.web.server.WebInvocation;

/**
 * @author arik
 */
@Component
final class RequestHandlersManagerImpl
{
    @Nonnull
    @Service
    private List<ServiceReference<RequestHandler>> requestHandlers;

    @Nonnull
    @Service
    private List<ServiceReference<RequestInterceptor>> interceptors;

    @Nonnull
    List<RequestHandler> findRequestHandlers( @Nonnull WebInvocation request )
    {
        List<RequestHandler> handlers = null;
        for( ServiceReference<RequestHandler> reference : this.requestHandlers )
        {
            Optional<List<String>> methodsHolder = reference.getProperties().find( "methods", TypeTokens.STRING_LIST );
            if( methodsHolder.isPresent() )
            {
                if( methodsHolder.get().contains( request.getHttpRequest().getMethod() ) )
                {
                    Optional<RequestHandler> holder = reference.service();
                    if( holder.isPresent() )
                    {
                        RequestHandler requestHandler = holder.get();
                        if( requestHandler.canHandle( request ) )
                        {
                            if( handlers == null )
                            {
                                handlers = new LinkedList<>();
                            }
                            handlers.add( requestHandler );
                        }
                    }
                }
            }
        }
        return handlers == null ? Collections.<RequestHandler>emptyList() : handlers;
    }

    @Nonnull
    Map<String, List<RequestHandler>> findRequestHandlersByMethod( @Nonnull WebInvocation request )
    {
        Map<String, List<RequestHandler>> handlers = null;
        for( ServiceReference<RequestHandler> reference : this.requestHandlers )
        {
            Optional<RequestHandler> holder = reference.service();
            if( holder.isPresent() )
            {
                RequestHandler requestHandler = holder.get();
                if( requestHandler.canHandle( request ) )
                {
                    Optional<List<String>> methodsHolder = reference.getProperties().find( "methods", TypeTokens.STRING_LIST );
                    if( methodsHolder.isPresent() )
                    {
                        List<String> methods = methodsHolder.get();
                        for( String method : methods )
                        {
                            if( handlers == null )
                            {
                                handlers = new HashMap<>();
                            }
                            List<RequestHandler> handlersForMethod = handlers.get( method.toLowerCase().trim() );
                            if( handlersForMethod == null )
                            {
                                handlersForMethod = new LinkedList<>();
                                handlers.put( method.toLowerCase().trim(), handlersForMethod );
                            }
                            handlersForMethod.add( requestHandler );
                        }
                    }
                }
            }
        }
        return handlers == null ? Collections.<String, List<RequestHandler>>emptyMap() : handlers;
    }

    @Nonnull
    List<RequestInterceptor> findInterceptors( @Nonnull WebInvocation request, @Nonnull RequestHandler requestHandler )
    {
        List<RequestInterceptor> interceptors = null;
        for( ServiceReference<RequestInterceptor> reference : this.interceptors )
        {
            Optional<List<String>> methodsHolder = reference.getProperties().find( "methods", TypeTokens.STRING_LIST );
            if( methodsHolder.isPresent() )
            {
                if( methodsHolder.get().contains( request.getHttpRequest().getMethod() ) )
                {
                    Optional<RequestInterceptor> holder = reference.service();
                    if( holder.isPresent() )
                    {
                        RequestInterceptor interceptor = holder.get();
                        if( interceptor.canHandle( request, requestHandler ) )
                        {
                            if( interceptors == null )
                            {
                                interceptors = new LinkedList<>();
                            }
                            interceptors.add( interceptor );
                        }
                    }
                }
            }
        }
        return interceptors == null ? Collections.<RequestInterceptor>emptyList() : interceptors;
    }
}

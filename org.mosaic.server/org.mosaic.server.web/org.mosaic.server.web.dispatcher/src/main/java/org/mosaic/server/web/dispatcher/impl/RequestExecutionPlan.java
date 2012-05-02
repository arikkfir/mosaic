package org.mosaic.server.web.dispatcher.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.Handler;
import org.mosaic.web.handler.Interceptor;
import org.mosaic.web.handler.InterceptorChain;

/**
 * @author arik
 */
public class RequestExecutionPlan implements InterceptorChain
{
    public static interface RequestExecutionBuilder
    {
        void contribute( RequestExecutionPlan plan );
    }

    private final HttpRequest request;

    private Handler handler;

    private Handler.HandlerMatch handlerMatch;

    private List<InterceptorEntry> interceptors = new ArrayList<>( );

    private Iterator<InterceptorEntry> executionIterator;

    public RequestExecutionPlan( HttpRequest request )
    {
        this.request = request;
    }

    public HttpRequest getRequest( )
    {
        return request;
    }

    public void setHandler( Handler handler, Handler.HandlerMatch match )
    {
        this.handler = handler;
        this.handlerMatch = match;
    }

    public void addInterceptor( int ranking, Interceptor interceptor, Interceptor.InterceptorMatch match )
    {
        this.interceptors.add( new InterceptorEntry( ranking, interceptor, match ) );
    }

    public void execute( ) throws Exception
    {
        Collections.sort( this.interceptors );
        this.executionIterator = this.interceptors.iterator( );
        next( );
    }

    @Override
    public Object next( ) throws Exception
    {
        if( this.executionIterator == null )
        {
            // not initialized - probably 'execute' has not been called or the execution has finished
            throw new IllegalStateException( "Execution plan has finished execution, or is not prepared for execution!" );
        }
        else if( this.executionIterator.hasNext( ) )
        {
            // invoke next interceptor in the chain
            InterceptorEntry interceptorEntry = this.executionIterator.next( );
            return interceptorEntry.interceptor.handle( this.request, interceptorEntry.interceptorMatch, this );
        }
        else
        {
            // last interceptor has been invoked - time to invoke the actual request handler now
            this.executionIterator = null;
            return this.handler.handle( this.request, this.handlerMatch );
        }
    }

    private class InterceptorEntry implements Comparable<InterceptorEntry>
    {
        private final int ranking;

        private final Interceptor interceptor;

        private final Interceptor.InterceptorMatch interceptorMatch;

        private InterceptorEntry( int ranking, Interceptor interceptor, Interceptor.InterceptorMatch interceptorMatch )
        {
            this.ranking = ranking;
            this.interceptor = interceptor;
            this.interceptorMatch = interceptorMatch;
        }

        @Override
        public int compareTo( InterceptorEntry o )
        {
            if( this.ranking == o.ranking )
            {
                return 0;
            }
            else if( this.ranking < o.ranking )
            {
                return -1;
            }
            else
            {
                return 1;
            }
        }
    }
}

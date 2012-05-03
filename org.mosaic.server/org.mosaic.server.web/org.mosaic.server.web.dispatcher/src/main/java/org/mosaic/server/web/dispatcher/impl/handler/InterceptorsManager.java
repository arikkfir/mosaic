package org.mosaic.server.web.dispatcher.impl.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.server.web.dispatcher.impl.RequestExecutionPlan;
import org.mosaic.server.web.dispatcher.impl.util.RegexPathMatcher;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.Interceptor;
import org.mosaic.web.handler.InterceptorChain;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class InterceptorsManager extends AbstractMethodEndpointManager
        implements RequestExecutionPlan.RequestExecutionBuilder
{
    private static final Logger LOG = LoggerFactory.getLogger( InterceptorsManager.class );

    private final Map<ServiceReference<?>, Interceptor> interceptors = new ConcurrentHashMap<>();

    @ServiceBind
    public void addInterceptor( ServiceReference<Interceptor> ref, Interceptor interceptor )
    {
        this.interceptors.put( ref, interceptor );
    }

    @ServiceUnbind
    public void removeInterceptor( ServiceReference<Interceptor> ref )
    {
        this.interceptors.remove( ref );
    }

    @ServiceBind( filter = "methodEndpointShortType=Interceptor" )
    public void addInterceptorMethod( ServiceReference<MethodEndpointInfo> ref, MethodEndpointInfo endpointInfo )
    {
        try
        {
            this.interceptors.put( ref, new MethodEndpointInterceptor( endpointInfo ) );
        }
        catch( Exception e )
        {
            LOG.warn( "Interceptor '{}' could not be added to Mosaic handlers: {}", endpointInfo, e.getMessage(), e );
        }
    }

    @ServiceUnbind( filter = "methodEndpointShortType=Interceptor" )
    public void removeInterceptorMethod( ServiceReference<MethodEndpointInfo> ref )
    {
        this.interceptors.remove( ref );
    }

    @Override
    public void contribute( RequestExecutionPlan plan )
    {
        for( Map.Entry<ServiceReference<?>, Interceptor> entry : this.interceptors.entrySet() )
        {
            ServiceReference<?> ref = entry.getKey();
            Interceptor interceptor = entry.getValue();

            Interceptor.InterceptorMatch match = interceptor.matches( plan.getRequest() );
            if( match != null )
            {
                Integer ranking = ( Integer ) ref.getProperty( Constants.SERVICE_RANKING );
                plan.addInterceptor( ranking == null ? 0 : ranking, interceptor, match );
            }
        }
    }

    private class MethodEndpointInterceptorMatch implements Interceptor.InterceptorMatch
    {
        private final RegexPathMatcher.MatchResult matchResult;

        private final Map<String, String> pathParams;

        private MethodEndpointInterceptorMatch( RegexPathMatcher.MatchResult matchResult )
        {
            this.matchResult = matchResult;
            this.pathParams = this.matchResult.getVariables();
        }

        private Map<String, String> getPathParams()
        {
            return this.pathParams;
        }
    }

    private class MethodEndpointInterceptor extends MethodEndpointWrapper implements Interceptor
    {
        private MethodEndpointInterceptor( MethodEndpointInfo methodEndpointInfo ) throws IllegalStateException
        {
            super( methodEndpointInfo, org.mosaic.web.handler.annotation.Interceptor.class );
        }

        @Override
        public InterceptorMatch matches( HttpRequest request )
        {
            if( acceptsHttpMethod( request.getMethod() ) && acceptsRequest( request ) )
            {
                RegexPathMatcher.MatchResult matchResult = acceptsPath( request.getUrl().getPath() );
                if( matchResult != null )
                {
                    return new MethodEndpointInterceptorMatch( matchResult );
                }
            }
            return null;
        }

        @Override
        public Object handle( HttpRequest request, InterceptorMatch match, InterceptorChain chain )
        throws Exception
        {
            MethodEndpointInterceptorMatch endpointMatch = ( MethodEndpointInterceptorMatch ) match;

            Map<String, String> oldPathParams = pushPathParams( request, endpointMatch.getPathParams() );
            try
            {
                return invoke( request );
            }
            finally
            {
                popPathParams( request, oldPathParams );
            }
        }
    }

}

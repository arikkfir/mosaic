package org.mosaic.server.web.dispatcher.impl.handler;

import java.util.*;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.server.lifecycle.MethodEndpointInfo;
import org.mosaic.server.web.dispatcher.impl.endpoint.AbstractMethodEndpointManager;
import org.mosaic.server.web.dispatcher.impl.endpoint.MethodParameterResolver;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.ExceptionHandler;
import org.osgi.framework.ServiceReference;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.comparator.ComparableComparator;
import org.springframework.util.comparator.NullSafeComparator;

import static java.util.Objects.compare;
import static org.osgi.framework.Constants.SERVICE_RANKING;

/**
 * @author arik
 */
@Component
public class ExceptionHandlersManager extends AbstractMethodEndpointManager
{
    private static final Logger LOG = LoggerFactory.getLogger( ExceptionHandlersManager.class );

    private static final Comparator<Integer> MARSHALLER_COMPARATOR = new NullSafeComparator<>( new ComparableComparator<Integer>(), true );

    private static final String HANDLER_RESULT_KEY = ExceptionHandler.class + "#result";

    private List<ExceptionHandlerEntry> exceptionHandlers = Collections.emptyList();

    @ServiceBind( filter = "methodEndpointShortType=ExceptionHandler" )
    public void addExceptionHandlerEndpoint( ServiceReference<MethodEndpointInfo> ref, MethodEndpointInfo endpointInfo )
    {
        addExceptionHandler( ref, new MethodEndpointMarshaller( endpointInfo ) );
    }

    @ServiceUnbind( filter = "methodEndpointShortType=ExceptionHandler" )
    public void removeExceptionHandlerEndpoint( ServiceReference<MethodEndpointInfo> ref )
    {
        removeExceptionHandler( ref );
    }

    @ServiceBind
    public void addNativeExceptionHandler( ServiceReference<ExceptionHandler> ref, ExceptionHandler marshaller )
    {
        addExceptionHandler( ref, marshaller );
    }

    @ServiceUnbind
    public void removeNativeExceptionHandler( ServiceReference<ExceptionHandler> ref )
    {
        removeExceptionHandler( ref );
    }

    public Object handleException( HttpRequest request, Exception exception )
    {
        List<ExceptionHandlerEntry> exceptionHandlers = new LinkedList<>( this.exceptionHandlers );
        Collections.reverse( exceptionHandlers );
        while( true )
        {
            ExceptionHandlerEntry exceptionHandlerEntry = findExceptionHandler( request, exception, exceptionHandlers );
            if( exceptionHandlerEntry != null )
            {
                try
                {
                    Object result = exceptionHandlerEntry.exceptionHandler.handle( request, exception );
                    if( result != null )
                    {
                        return result;
                    }
                }
                catch( Exception e )
                {
                    exception = e;
                }
            }
            else
            {
                LOG.error( "Error executing request (no exception handler was found): {}", exception.getMessage(), exception );
                request.setResponseStatus( HttpStatus.INTERNAL_SERVER_ERROR, "An internal error has occurred" );
                return null;
            }
        }
    }

    private ExceptionHandlerEntry findExceptionHandler( HttpRequest request,
                                                        Exception exception,
                                                        List<ExceptionHandlerEntry> exceptionHandlers )
    {
        for( Iterator<ExceptionHandlerEntry> iterator = exceptionHandlers.iterator(); iterator.hasNext(); )
        {
            ExceptionHandlerEntry entry = iterator.next();
            if( entry.exceptionHandler.matches( request, exception ) )
            {
                iterator.remove();
                return entry;
            }
        }
        return null;
    }

    private void addExceptionHandler( ServiceReference<?> ref, ExceptionHandler exceptionHandler )
    {
        try
        {
            List<ExceptionHandlerEntry> newExceptionHandlers = new ArrayList<>( this.exceptionHandlers );
            newExceptionHandlers.add( new ExceptionHandlerEntry( ref, exceptionHandler ) );
            Collections.sort( newExceptionHandlers );
            this.exceptionHandlers = newExceptionHandlers;
        }
        catch( Exception e )
        {
            LOG.warn( "ExceptionHandler '{}' could not be added to Mosaic exception handlers: {}", exceptionHandler, e.getMessage(), e );
        }
    }

    private void removeExceptionHandler( ServiceReference<?> ref )
    {
        List<ExceptionHandlerEntry> newExceptionHandlers = new ArrayList<>( this.exceptionHandlers );
        for( Iterator<ExceptionHandlerEntry> iterator = newExceptionHandlers.iterator(); iterator.hasNext(); )
        {
            ExceptionHandlerEntry entry = iterator.next();
            if( entry.ref.equals( ref ) )
            {
                iterator.remove();
                this.exceptionHandlers = newExceptionHandlers;
                return;
            }
        }
    }

    private class ExceptionHandlerEntry implements Comparable<ExceptionHandlerEntry>
    {
        private final ServiceReference<?> ref;

        private final ExceptionHandler exceptionHandler;

        private ExceptionHandlerEntry( ServiceReference<?> ref, ExceptionHandler exceptionHandler )
        {
            this.ref = ref;
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        public int compareTo( ExceptionHandlerEntry o )
        {
            Integer myRank = ( Integer ) this.ref.getProperty( SERVICE_RANKING );
            Integer thatRank = ( Integer ) o.ref.getProperty( SERVICE_RANKING );
            return compare( myRank, thatRank, MARSHALLER_COMPARATOR );
        }
    }

    private class MethodEndpointMarshaller extends MethodEndpointWrapper
            implements ExceptionHandler, MethodParameterResolver.ResolvedParameter
    {
        private Class<?> exceptionType;

        private MethodEndpointMarshaller( MethodEndpointInfo methodEndpointInfo )
        {
            super( methodEndpointInfo );
        }

        @Override
        public Object resolve( HttpRequest request )
        {
            return request.get( HANDLER_RESULT_KEY );
        }

        @Override
        public ResolvedParameter resolve( MethodParameter methodParameter )
        {
            if( Exception.class.isAssignableFrom( methodParameter.getParameterType() ) )
            {
                this.exceptionType = methodParameter.getParameterType();
                return this;
            }
            else
            {
                return null;
            }
        }

        @Override
        public boolean matches( HttpRequest request, Exception exception )
        {
            return acceptsHttpMethod( request.getMethod() )
                   && acceptsRequest( request )
                   && this.exceptionType != null
                   && this.exceptionType.isInstance( exception );
        }

        @Override
        public Object handle( HttpRequest request, Exception exception ) throws Exception
        {
            Object previousHandlerResult = request.put( HANDLER_RESULT_KEY, exception );
            try
            {
                return invoke( request );
            }
            finally
            {
                request.put( HANDLER_RESULT_KEY, previousHandlerResult );
            }
        }
    }
}

package org.mosaic.server.web.dispatcher.impl.marshall;

import java.util.*;
import org.mosaic.lifecycle.MethodEndpointInfo;
import org.mosaic.lifecycle.ServiceBind;
import org.mosaic.lifecycle.ServiceUnbind;
import org.mosaic.server.web.dispatcher.impl.endpoint.AbstractMethodEndpointManager;
import org.mosaic.server.web.dispatcher.impl.endpoint.MethodParameterResolver;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;
import org.mosaic.web.HttpRequest;
import org.mosaic.web.handler.Marshaller;
import org.osgi.framework.ServiceReference;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.comparator.ComparableComparator;
import org.springframework.util.comparator.NullSafeComparator;

import static java.util.Objects.compare;
import static org.osgi.framework.Constants.SERVICE_RANKING;

/**
 * @author arik
 */
@Component
public class MarshallersManager extends AbstractMethodEndpointManager
{
    private static final Logger LOG = LoggerFactory.getLogger( MarshallersManager.class );

    private static final Comparator<Integer> MARSHALLER_COMPARATOR = new NullSafeComparator<>( new ComparableComparator<Integer>(), true );

    private static final String HANDLER_RESULT_KEY = Marshaller.class + "#result";

    private List<MarshallerEntry> marshallers = Collections.emptyList();

    @ServiceBind( filter = "methodEndpointShortType=Marshaller" )
    public void addMarshallerEndpoint( ServiceReference<MethodEndpointInfo> ref, MethodEndpointInfo endpointInfo )
    {
        addMarshaller( ref, new MethodEndpointMarshaller( endpointInfo ) );
    }

    @ServiceUnbind( filter = "methodEndpointShortType=Marshaller" )
    public void removeMarshallerEndpoint( ServiceReference<MethodEndpointInfo> ref )
    {
        removeMarshaller( ref );
    }

    @ServiceBind
    public void addNativeMarshaller( ServiceReference<Marshaller> ref, Marshaller marshaller )
    {
        addMarshaller( ref, marshaller );
    }

    @ServiceUnbind
    public void removeNativeMarshaller( ServiceReference<Marshaller> ref )
    {
        removeMarshaller( ref );
    }

    public Marshaller getMarshaller( HttpRequest request, Object handlerResult )
    {
        for( int i = this.marshallers.size() - 1; i >= 0; i-- )
        {
            MarshallerEntry entry = this.marshallers.get( i );
            if( entry.marshaller.matches( request, handlerResult ) )
            {
                return entry.marshaller;
            }
        }
        return null;
    }

    private void addMarshaller( ServiceReference<?> ref, Marshaller marshaller )
    {
        try
        {
            List<MarshallerEntry> newMarshallers = new ArrayList<>( this.marshallers );
            newMarshallers.add( new MarshallerEntry( ref, marshaller ) );
            Collections.sort( newMarshallers );
            this.marshallers = newMarshallers;
        }
        catch( Exception e )
        {
            LOG.warn( "Marshaller '{}' could not be added to Mosaic marshallers: {}", marshaller, e.getMessage(), e );
        }
    }

    private void removeMarshaller( ServiceReference<?> ref )
    {
        List<MarshallerEntry> newMarshallers = new ArrayList<>( this.marshallers );
        for( Iterator<MarshallerEntry> iterator = newMarshallers.iterator(); iterator.hasNext(); )
        {
            MarshallerEntry entry = iterator.next();
            if( entry.ref.equals( ref ) )
            {
                iterator.remove();
                this.marshallers = newMarshallers;
                return;
            }
        }
    }

    private class MarshallerEntry implements Comparable<MarshallerEntry>
    {
        private final ServiceReference<?> ref;

        private final Marshaller marshaller;

        private MarshallerEntry( ServiceReference<?> ref, Marshaller marshaller )
        {
            this.ref = ref;
            this.marshaller = marshaller;
        }

        @Override
        public int compareTo( MarshallerEntry o )
        {
            Integer myRank = ( Integer ) this.ref.getProperty( SERVICE_RANKING );
            Integer thatRank = ( Integer ) o.ref.getProperty( SERVICE_RANKING );
            return compare( myRank, thatRank, MARSHALLER_COMPARATOR );
        }
    }

    private class MethodEndpointMarshaller extends AbstractMethodEndpointManager.MethodEndpointWrapper
            implements Marshaller, MethodParameterResolver.ResolvedParameter
    {
        private Class<?> marshallableType;

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
            this.marshallableType = methodParameter.getParameterType();
            return this;
        }

        @Override
        public boolean matches( HttpRequest request, Object handlerResult )
        {
            return acceptsHttpMethod( request.getMethod() )
                   && acceptsRequest( request )
                   && this.marshallableType != null
                   && this.marshallableType.isInstance( handlerResult );
        }

        @Override
        public Object marshall( HttpRequest request, Object handlerResult ) throws Exception
        {
            Object previousHandlerResult = request.put( HANDLER_RESULT_KEY, handlerResult );
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

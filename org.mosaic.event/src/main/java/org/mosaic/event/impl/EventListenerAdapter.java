package org.mosaic.event.impl;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.event.EventListener;
import org.mosaic.modules.Adapter;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.modules.Service;
import org.mosaic.modules.ServicePropertiesProvider;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.util.reflection.ParameterResolver;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Adapter( EventHandler.class )
public final class EventListenerAdapter implements EventHandler, ServicePropertiesProvider
{
    private static final Logger LOG = LoggerFactory.getLogger( EventListenerAdapter.class );

    @Nonnull
    private final MethodEndpoint<EventListener> endpoint;

    @Nonnull
    private final Map<String, Object> serviceProperties;

    @Nullable
    private final MethodEndpoint.Invoker invoker;

    @Service
    public EventListenerAdapter( @Nonnull MethodEndpoint<EventListener> endpoint )
    {
        this.endpoint = endpoint;

        List<MethodParameter> parameters = this.endpoint.getMethodHandle().getParameters();
        if( parameters.isEmpty() )
        {
            LOG.warn( "Event listener {} does not declare event type", this.endpoint );
            this.serviceProperties = Collections.emptyMap();
            this.invoker = null;
        }
        else if( parameters.size() != 1 )
        {
            LOG.warn( "Event listener {} declares more than 1 parameter (@EventListener(s) must define exactly 1 parameter which is the event type)", this.endpoint );
            this.serviceProperties = Collections.emptyMap();
            this.invoker = null;
        }
        else
        {
            Map<String, Object> properties = new HashMap<>();
            properties.put( EventConstants.EVENT_TOPIC, parameters.get( 0 ).getType().getRawType().getName().replace( '.', '/' ) );
            this.serviceProperties = Collections.unmodifiableMap( properties );
            this.invoker = this.endpoint.createInvoker( new EventParameterResolver() );
        }
    }

    @Override
    public void addProperties( @Nonnull Dictionary<String, Object> properties )
    {
        for( Map.Entry<String, Object> entry : this.serviceProperties.entrySet() )
        {
            properties.put( entry.getKey(), entry.getValue() );
        }
    }

    @Override
    public final void handleEvent( @Nonnull Event event )
    {
        if( this.invoker != null )
        {
            Map<String, Object> context = new HashMap<>( 1 );
            context.put( "event", event.getProperty( "mosaicEvent" ) );
            try
            {
                this.invoker.resolve( context ).invoke();
            }
            catch( Exception e )
            {
                LOG.warn( "Event listener {} failed with an exception: {}", this.endpoint, e.getMessage(), e );
            }
        }
    }

    private class EventParameterResolver implements ParameterResolver
    {
        @Nullable
        @Override
        public Object resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> resolveContext )
                throws Exception
        {
            return resolveContext.require( "event" );
        }
    }
}

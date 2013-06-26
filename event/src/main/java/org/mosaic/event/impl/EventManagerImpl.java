package org.mosaic.event.impl;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.mosaic.event.Event;
import org.mosaic.event.EventDeliveryCallback;
import org.mosaic.event.EventManager;
import org.mosaic.event.annotation.Subscribe;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.lifecycle.annotation.*;
import org.mosaic.util.collect.LinkedHashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Service(EventManager.class)
public class EventManagerImpl implements EventManager
{
    private static final Logger LOG = LoggerFactory.getLogger( EventManagerImpl.class );

    private static final Splitter PATTERN_TOKEN_SPLITTER = Splitter.on( "/" ).trimResults();

    @Nonnull
    private final Map<Long, EventListenerImpl> listeners = new ConcurrentHashMap<>();

    @Nonnull
    private ConversionService conversionService;

    @Nullable
    private ExecutorService executor;

    @ServiceRef
    public void setConversionService( @Nonnull ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @MethodEndpointBind(Subscribe.class)
    public void addEventSubscriber( @ServiceId long id, @Nonnull MethodEndpoint endpoint )
    {
        this.listeners.put( id, new EventListenerImpl( endpoint ) );
        LOG.info( "Added event @Subscriber {}", endpoint );
    }

    @MethodEndpointUnbind(Subscribe.class)
    public void removeEventSubscriber( @ServiceId long id, @Nonnull MethodEndpoint endpoint )
    {
        this.listeners.remove( id );
        LOG.info( "Removed event @Subscriber {}", endpoint );
    }

    @PostConstruct
    public void init()
    {
        this.executor = Executors.newFixedThreadPool( 50, new ThreadFactoryBuilder()
                .setDaemon( true )
                .setNameFormat( "mosaic-event-dispatcher-%d" )
                .setPriority( Thread.MIN_PRIORITY )
                .build()
        );
    }

    @PreDestroy
    public void destroy()
    {
        if( this.executor != null )
        {
            this.executor.shutdown();
        }
        this.executor = null;
    }

    @Nonnull
    @Override
    public Event createEvent( @Nonnull String topic )
    {
        return new EventImpl( topic, new LinkedHashMapEx<String, Object>( this.conversionService ) );
    }

    @Override
    public void postEvent( @Nonnull final Event event )
    {
        ExecutorService executor = this.executor;
        if( executor != null )
        {
            executor.submit( new EventDispatcher( event ) );
        }
        else
        {
            throw new IllegalStateException( "EventManager has been shutdown" );
        }
    }

    @Override
    public void postEvent( @Nonnull Event event, @Nonnull EventDeliveryCallback callback )
    {
        ExecutorService executor = this.executor;
        if( executor != null )
        {
            executor.submit( new EventDispatcher( event, callback ) );
        }
        else
        {
            throw new IllegalStateException( "EventManager has been shutdown" );
        }
    }

    private class EventListenerImpl implements MethodHandle.ParameterResolver
    {
        @Nonnull
        private final Pattern pattern;

        @Nonnull
        private final MethodEndpoint endpoint;

        @Nonnull
        private final MethodEndpoint.Invoker invoker;

        private EventListenerImpl( @Nonnull MethodEndpoint endpoint )
        {
            this.endpoint = endpoint;
            this.invoker = this.endpoint.createInvoker( this );

            Subscribe ann = endpoint.getAnnotation( Subscribe.class );
            if( ann == null )
            {
                throw new IllegalArgumentException( endpoint + " does not define @Subscribe" );
            }

            StringBuilder patternBuffer = new StringBuilder( ann.value().length() );
            for( String token : PATTERN_TOKEN_SPLITTER.split( ann.value() ) )
            {
                if( patternBuffer.length() > 0 )
                {
                    patternBuffer.append( "\\/" );
                }
                switch( token )
                {
                    case "*":
                        patternBuffer.append( "[^\\/]+" );
                        break;
                    case "**":
                        patternBuffer.append( ".+" );
                        break;
                    default:
                        patternBuffer.append( Pattern.quote( token ) );
                        break;
                }
            }
            this.pattern = Pattern.compile( patternBuffer.toString() );
        }

        @Nullable
        @Override
        public Object resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> resolveContext )
        {
            if( parameter.getType().isAssignableFrom( Event.class ) )
            {
                return resolveContext.require( "event" );
            }
            return SKIP;
        }

        private boolean listensTo( @Nonnull Event event )
        {
            return this.pattern.matcher( event.getTopic() ).matches();
        }

        private void handle( @Nonnull Event event )
        {
            try
            {
                Map<String, Object> context = new HashMap<>();
                context.put( "event", event );
                this.invoker.resolve( context ).invoke();
            }
            catch( Exception e )
            {
                LOG.error( "Event subscriber '{}' threw an exception: {}", this.endpoint, e.getMessage(), e );
            }
        }
    }

    private class EventImpl implements Event
    {
        @Nonnull
        private final String topic;

        @Nonnull
        private final MapEx<String, Object> properties;

        private EventImpl( @Nonnull String topic,
                           @Nonnull MapEx<String, Object> properties )
        {
            this.topic = topic;
            this.properties = properties;
        }

        @Nonnull
        @Override
        public String getTopic()
        {
            return this.topic;
        }

        @Nonnull
        @Override
        public MapEx<String, Object> getProperties()
        {
            return this.properties;
        }
    }

    private class EventDispatcher implements Runnable
    {
        @Nonnull
        private final Event event;

        @Nullable
        private final EventDeliveryCallback callback;

        public EventDispatcher( @Nonnull Event event )
        {
            this.event = event;
            this.callback = null;
        }

        private EventDispatcher( @Nonnull Event event, @Nullable EventDeliveryCallback callback )
        {
            this.event = event;
            this.callback = callback;
        }

        @Override
        public void run()
        {
            for( EventListenerImpl listener : EventManagerImpl.this.listeners.values() )
            {
                if( listener.listensTo( event ) )
                {
                    listener.handle( event );
                }
            }

            if( this.callback != null )
            {
                this.callback.eventDelivered( this.event );
            }
        }
    }
}

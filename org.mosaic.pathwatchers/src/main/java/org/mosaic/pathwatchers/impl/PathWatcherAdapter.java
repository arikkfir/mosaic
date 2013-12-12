package org.mosaic.pathwatchers.impl;

import com.google.common.base.Splitter;
import java.nio.file.Path;
import java.util.Dictionary;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.Adapter;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.modules.Service;
import org.mosaic.modules.ServicePropertiesProvider;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.util.reflection.ParameterResolver;
import org.mosaic.util.resource.PathEvent;
import org.mosaic.util.resource.PathWatcher;
import org.mosaic.util.resource.PathWatcherContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
@Adapter( PathWatcher.class )
public class PathWatcherAdapter implements PathWatcher, ServicePropertiesProvider
{
    private static final Logger LOG = LoggerFactory.getLogger( PathWatcherAdapter.class );

    @Nonnull
    private final String location;

    @Nullable
    private final String pattern;

    @Nonnull
    private final PathEvent[] events;

    @Nullable
    private final MethodEndpoint.Invoker invoker;

    @Service
    public PathWatcherAdapter( @Nonnull MethodEndpoint<org.mosaic.pathwatchers.PathWatcher> endpoint )
    {
        org.mosaic.pathwatchers.PathWatcher annotation = endpoint.getType();

        String locationAndPattern = annotation.value();
        StringBuilder location = new StringBuilder( locationAndPattern.length() );
        String pattern = null;
        for( String token : Splitter.on( '/' ).split( locationAndPattern ) )
        {
            if( !token.contains( "*" ) && !token.contains( "?" ) )
            {
                if( location.length() > 0 )
                {
                    location.append( '/' );
                }
                location.append( token );
            }
            else if( location.length() == 0 )
            {
                LOG.warn( "@PathWatcher {} defines an illegal pattern ('{}') and will NOT receive file watching events",
                          endpoint, locationAndPattern );
                this.location = "" + System.currentTimeMillis() + "/nowhere/that/exists";
                this.pattern = null;
                this.events = new PathEvent[] { PathEvent.DELETED };
                this.invoker = null;
                return;
            }
            else
            {
                pattern = locationAndPattern;
                break;
            }
        }

        this.location = location.toString();
        this.pattern = pattern;
        this.events = annotation.events();

        this.invoker = endpoint.createInvoker(
                new ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext )
                            throws Exception
                    {
                        if( parameter.getType().isAssignableFrom( PathWatcherContext.class ) )
                        {
                            return resolveContext.require( "watcherContext" );
                        }
                        return SKIP;
                    }
                },
                new ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext )
                            throws Exception
                    {
                        if( parameter.getType().isAssignableFrom( Path.class ) )
                        {
                            return resolveContext.require( "watcherContext", PathWatcherContext.class ).getFile();
                        }
                        return SKIP;
                    }
                },
                new ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext )
                            throws Exception
                    {
                        if( parameter.getType().isAssignableFrom( PathEvent.class ) )
                        {
                            return resolveContext.require( "watcherContext", PathWatcherContext.class ).getEvent();
                        }
                        return SKIP;
                    }
                }
        );
    }

    @Override
    public void addProperties( @Nonnull Dictionary<String, Object> properties )
    {
        properties.put( "location", this.location );
        properties.put( "pattern", this.pattern );
        properties.put( "events", this.events );
    }

    @Override
    public void handle( @Nonnull PathWatcherContext context ) throws Exception
    {
        if( this.invoker != null )
        {
            MapEx<String, Object> methodContext = new HashMapEx<>( 1 );
            methodContext.put( "watcherContext", context );
            this.invoker.resolve( methodContext ).invoke();
        }
    }
}

package org.mosaic.pathwatchers.impl;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Dictionary;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.modules.ServicePropertiesProvider;
import org.mosaic.util.collections.HashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.method.MethodParameter;
import org.mosaic.util.method.ParameterResolver;
import org.mosaic.util.resource.PathWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
abstract class AbstractPathWatcherAdapter<AnnType extends Annotation> implements PathWatcher, ServicePropertiesProvider
{
    @Nonnull
    private static final TypeToken<MapEx<String, Object>> CONTEXT_TYPE_TOKEN = new TypeToken<MapEx<String, Object>>()
    {
    };

    @Nonnull
    private final MethodEndpoint<AnnType> endpoint;

    @Nonnull
    private final MethodEndpoint.Invoker invoker;

    protected boolean disabled;

    protected AbstractPathWatcherAdapter( @Nonnull MethodEndpoint<AnnType> endpoint )
    {
        this.endpoint = endpoint;
        this.invoker = this.endpoint.createInvoker(
                new ParameterResolver<Path>()
                {
                    @Nonnull
                    @Override
                    public Optional<Path> resolve( @Nonnull MethodParameter parameter,
                                                   @Nonnull MapEx<String, Object> context )
                            throws Exception
                    {
                        if( parameter.getType().isAssignableFrom( Path.class ) )
                        {
                            return context.find( "path", Path.class );
                        }
                        return Optional.absent();
                    }
                },
                new ParameterResolver<MapEx<String, Object>>()
                {
                    @Nonnull
                    @Override
                    public Optional<MapEx<String, Object>> resolve( @Nonnull MethodParameter parameter,
                                                    @Nonnull MapEx<String, Object> context )
                            throws Exception
                    {
                        if( parameter.getType().isAssignableFrom( CONTEXT_TYPE_TOKEN ) )
                        {
                            return context.find( "context", CONTEXT_TYPE_TOKEN );
                        }
                        return Optional.absent();
                    }
                }
        );
    }

    @Override
    public void scanStarted( @Nonnull MapEx<String, Object> context )
    {
        // no-op
    }

    @Override
    public void pathCreated( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        // no-op
    }

    @Override
    public void pathModified( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        // no-op
    }

    @Override
    public void pathUnmodified( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        // no-op
    }

    @Override
    public void pathDeleted( @Nonnull Path path, @Nonnull MapEx<String, Object> context )
    {
        // no-op
    }

    @Override
    public void scanError( @Nullable Path path, @Nonnull MapEx<String, Object> context, @Nonnull Throwable throwable )
    {
        getEndpointLogger().warn( "Path watcher {} failed: {}", this.endpoint, endpoint.getMethodHandle(), endpoint );
    }

    @Override
    public void scanCompleted( @Nonnull MapEx<String, Object> context )
    {
        // no-op
    }

    @Override
    public void addProperties( @Nonnull Dictionary<String, Object> properties )
    {
        // assume failure until success at the end
        this.disabled = true;

        // we'll extract info from the endpoint's annotation
        Annotation annotation = this.endpoint.getType();

        // extract location and pattern
        try
        {
            Method valueMethod = annotation.getClass().getDeclaredMethod( "value" );
            String locationAndPattern = valueMethod.invoke( annotation ).toString();

            StringBuilder location = new StringBuilder( locationAndPattern.length() );
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
                    getEndpointLogger().warn( "Path watcher {} defined an illegal pattern ('{}')", endpoint, locationAndPattern );
                    return;
                }
                else
                {
                    properties.put( "pattern", locationAndPattern );
                    break;
                }
            }
            properties.put( "location", location.toString() );
        }
        catch( Exception e )
        {
            getEndpointLogger().error( "Could not extract watcher path from {} endpoint - error is: {}", this.endpoint, e.getMessage(), e );
            return;
        }

        // success!
        this.disabled = false;
    }

    protected final void invoke( @Nullable Path path, @Nonnull MapEx<String, Object> context )
    {
        MapEx<String, Object> invocationContext = new HashMapEx<>();
        invocationContext.put( "path", path );
        invocationContext.put( "context", context );
        try
        {
            this.invoker.resolve( invocationContext ).invoke();
        }
        catch( Exception e )
        {
            getEndpointLogger().error( "Path watcher '{}' threw an exception: {}", this.endpoint, e.getMessage(), e );
        }
    }

    @Nonnull
    protected Logger getEndpointLogger()
    {
        return LoggerFactory.getLogger( this.endpoint.getMethodHandle().getDeclaringClass() );
    }
}

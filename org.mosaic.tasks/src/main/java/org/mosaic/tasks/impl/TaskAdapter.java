package org.mosaic.tasks.impl;

import com.google.common.base.Optional;
import java.util.Collections;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.tasks.Task;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.method.MethodParameter;
import org.mosaic.util.method.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
final class TaskAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger( TaskAdapter.class );

    @Nonnull
    private final MethodEndpoint<Task> endpoint;

    @Nonnull
    private final MethodEndpoint.Invoker invoker;

    TaskAdapter( @Nonnull MethodEndpoint<Task> endpoint )
    {
        this.endpoint = endpoint;
        this.invoker = this.endpoint.createInvoker(
                new ParameterResolver<Object>()
                {
                    @SuppressWarnings("unchecked")
                    @Nullable
                    @Override
                    public Optional<Object> resolve( @Nonnull MethodParameter parameter,
                                                     @Nonnull MapEx<String, Object> resolveContext )
                            throws Exception
                    {
                        Optional<MapEx> optional = resolveContext.find( "properties", MapEx.class );
                        if( optional.isPresent() )
                        {
                            MapEx properties = optional.get();
                            if( properties.containsKey( parameter.getName() ) )
                            {
                                return properties.find( parameter.getName(), parameter.getType() );
                            }
                        }
                        return Optional.absent();
                    }
                }
        );
    }

    @Nonnull
    String getName()
    {
        return this.endpoint.getName();
    }

    void execute( @Nonnull MapEx<String, String> properties )
    {
        try
        {
            this.invoker.resolve( Collections.<String, Object>singletonMap( "properties", properties ) ).invoke();
        }
        catch( Throwable e )
        {
            LOG.error( "Task '{}' threw an exception: {}", this.endpoint, e.getMessage(), e );
        }
    }
}

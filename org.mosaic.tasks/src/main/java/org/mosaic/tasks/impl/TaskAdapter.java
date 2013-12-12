package org.mosaic.tasks.impl;

import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.tasks.Task;
import org.mosaic.util.collections.MapEx;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.util.reflection.ParameterResolver;
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
                new ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext )
                            throws Exception
                    {
                        Map properties = resolveContext.require( "properties", Map.class );
                        if( properties.containsKey( parameter.getName() ) )
                        {
                            return properties.get( parameter.getName() );
                        }
                        else
                        {
                            return SKIP;
                        }
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

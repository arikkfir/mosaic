package org.mosaic.tasks.impl;

import java.util.Collections;
import javax.annotation.Nonnull;
import org.mosaic.modules.MethodEndpoint;
import org.mosaic.tasks.Task;
import org.mosaic.util.collections.MapEx;
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
        this.invoker = this.endpoint.createInvoker();
    }

    @Nonnull
    String getName()
    {
        return this.endpoint.getName();
    }

    void execute( @Nonnull MapEx<String, String> properties )
    {
        // TODO: pass properties as arguments to @Task method eg '@Task void myTask( int myProperty )'
        try
        {
            this.invoker.resolve( Collections.<String, Object>emptyMap() ).invoke();
        }
        catch( Throwable e )
        {
            LOG.error( "Task '{}' threw an exception: {}", this.endpoint, e.getMessage(), e );
        }
    }
}

package org.mosaic.core.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.workflow.Workflow;

import static java.util.Objects.requireNonNull;

/**
 * @author arik
 */
public final class Dispatcher
{
    @Nonnull
    private final ReadWriteLock lock;

    @Nullable
    private ExecutorService dispatcher;

    Dispatcher( @Nonnull Workflow workflow, @Nonnull ReadWriteLock lock )
    {
        this.lock = lock;
        workflow.addAction(
                ServerStatus.STARTED,
                c -> {
                    this.dispatcher = Executors.newSingleThreadExecutor();
                },
                c -> {
                    if( this.dispatcher != null )
                    {
                        this.dispatcher.shutdown();
                        this.dispatcher = null;
                    }
                } );
        workflow.addAction(
                ServerStatus.STOPPED,
                c -> {
                    if( this.dispatcher != null )
                    {
                        this.dispatcher.shutdown();
                        this.dispatcher = null;
                    }
                } );
    }

    @Nonnull
    public Future<?> dispatch( @Nonnull Runnable runnable )
    {
        return requireNonNull( this.dispatcher ).submit( () -> this.lock.write( runnable ) );
    }

    @Nonnull
    public <T> Future<T> dispatch( @Nonnull Callable<T> runnable )
    {
        return requireNonNull( this.dispatcher ).submit( () -> this.lock.write( runnable ) );
    }
}

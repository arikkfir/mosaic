package org.mosaic.tasks.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.modules.*;
import org.mosaic.tasks.Task;

/**
 * @author arik
 */
@Component
final class TasksManagerImpl
{
    @Nonnull
    private final Map<Long, TaskAdapter> tasks = new ConcurrentHashMap<>();

    @OnServiceAdded
    void addTask( @Nonnull ServiceReference<MethodEndpoint<Task>> reference )
    {
        this.tasks.put( reference.getId(), new TaskAdapter( reference.require() ) );
    }

    @OnServiceRemoved
    void removeTask( @Nonnull ServiceReference<MethodEndpoint<Task>> reference )
    {
        this.tasks.remove( reference.getId() );
    }

    @Nullable
    TaskAdapter findTask( @Nonnull String taskName )
    {
        for( TaskAdapter adapter : this.tasks.values() )
        {
            if( taskName.equals( adapter.getName() ) )
            {
                return adapter;
            }
        }
        return null;
    }
}

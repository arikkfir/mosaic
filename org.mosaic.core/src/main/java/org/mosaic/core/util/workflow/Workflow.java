package org.mosaic.core.util.workflow;

import java.util.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.logging.Logging;
import org.slf4j.Logger;

import static java.util.Collections.unmodifiableSet;

/**
 * @author arik
 */
public class Workflow
{
    @Nonnull
    protected final Logger logger;

    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final String name;

    @Nonnull
    private final Deque<TransitionListener> listeners = new LinkedList<>();

    @Nonnull
    private final Map<Status, Map<Status, TransitionDirection>> statuses = new HashMap<>();

    @Nonnull
    private Status status;

    public Workflow( @Nonnull ReadWriteLock lock, @Nonnull String name, @Nonnull Status status )
    {
        this.lock = lock;
        this.name = name;
        this.status = status;
        this.logger = Logging.getLogger( getClass() );
    }

    public Workflow( @Nonnull ReadWriteLock lock, @Nonnull String name, @Nonnull Status status, @Nonnull Logger logger )
    {
        this.lock = lock;
        this.name = name;
        this.status = status;
        this.logger = logger;
    }

    @Override
    public String toString()
    {
        return ToStringHelper.create( this ).add( "name", this.name ).toString();
    }

    @Nonnull
    public ReadWriteLock getLock()
    {
        this.lock.acquireReadLock();
        try
        {
            return this.lock;
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    @Nonnull
    public final String getName()
    {
        this.lock.acquireReadLock();
        try
        {
            return this.name;
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    @Nonnull
    public final Set<Status> getStatuses()
    {
        this.lock.acquireReadLock();
        try
        {
            return unmodifiableSet( new HashSet<>( this.statuses.keySet() ) );
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    public final void addStatus( @Nonnull Status status )
    {
        this.lock.acquireWriteLock();
        try
        {
            if( !this.statuses.containsKey( status ) )
            {
                this.statuses.put( status, new HashMap<Status, TransitionDirection>() );
            }
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Nonnull
    public final Set<Status> getTargetStatusesFor( @Nonnull Status origin )
    {
        this.lock.acquireReadLock();
        try
        {
            Map<Status, TransitionDirection> transitions = this.statuses.get( origin );
            return transitions == null ? Collections.<Status>emptySet() : unmodifiableSet( new HashSet<Status>( transitions.keySet() ) );
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    @Nonnull
    public final Set<Status> getOriginStatusesFor( @Nonnull Status target )
    {
        this.lock.acquireReadLock();
        try
        {
            Set<Status> originStatuses = new HashSet<>();
            for( Map.Entry<Status, Map<Status, TransitionDirection>> entry : this.statuses.entrySet() )
            {
                if( entry.getValue().containsKey( target ) )
                {
                    originStatuses.add( entry.getKey() );
                }
            }
            return unmodifiableSet( new HashSet<>( originStatuses ) );
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    public final boolean isTransitionAllowed( @Nonnull Status origin, @Nonnull Status target )
    {
        this.lock.acquireReadLock();
        try
        {
            Map<Status, TransitionDirection> transitions = this.statuses.get( origin );
            return transitions != null && transitions.containsKey( target );
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    public final void addTransition( @Nonnull Status origin,
                                     @Nonnull Status target,
                                     @Nonnull TransitionDirection direction )
    {
        this.lock.acquireWriteLock();
        try
        {
            Map<Status, TransitionDirection> transitions = this.statuses.get( origin );
            if( transitions == null )
            {
                transitions = new HashMap<>();
                this.statuses.put( origin, transitions );
            }
            transitions.put( target, direction );
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Nonnull
    public final <T extends TransitionListener> T addListener( @Nonnull T listener )
    {
        this.lock.acquireWriteLock();
        try
        {
            this.listeners.add( listener );
            return listener;
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Nonnull
    public final <T extends TransitionListener> T removeListener( @Nonnull T listener )
    {
        this.lock.acquireWriteLock();
        try
        {
            this.listeners.remove( listener );
            return listener;
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    @Nonnull
    public final Status getStatus()
    {
        this.lock.acquireReadLock();
        try
        {
            return this.status;
        }
        finally
        {
            this.lock.releaseReadLock();
        }
    }

    public final void transitionTo( @Nonnull Status target, boolean vetoable )
    {
        this.lock.acquireWriteLock();
        try
        {
            // save the origin status
            Status origin = this.status;
            this.logger.debug( "Transitioning {} from {} to {}", this, origin, target );

            // if already in target state, do nothing
            if( this.status == target )
            {
                this.logger.debug( "{} is already in status {} (ignoring transition request)", this, target );
                return;
            }

            // fail if transition is not allowed
            Map<Status, TransitionDirection> transitions = this.statuses.get( origin );
            if( !transitions.containsKey( target ) )
            {
                throw new TransitionException( "no transition found for " + this + " from " + origin + " to " + target, this, origin, target );
            }
            TransitionDirection direction = transitions.get( target );
            if( direction == null )
            {
                throw new TransitionException( "no transition found for " + this + " from " + origin + " to " + target, this, origin, target );
            }

            // validate transition (validations always run in forward direction for now)
            if( vetoable )
            {
                for( TransitionListener listener : this.listeners )
                {
                    try
                    {
                        listener.validate( origin, target );
                    }
                    catch( Throwable e )
                    {
                        if( e instanceof WorkflowException )
                        {
                            throw ( WorkflowException ) e;
                        }
                        else
                        {
                            throw new TransitionException( "transition of " + this + " from " + origin + " to " + target + " was vetoed by " + listener, e, this, origin, target );
                        }
                    }
                }
            }

            // change status
            this.status = target;

            // get listeners in appropriate direction
            Iterator<TransitionListener> listenersIterator =
                    direction == TransitionDirection.FORWARD
                    ? this.listeners.iterator()
                    : this.listeners.descendingIterator();
            List<TransitionListener> executedListeners = new LinkedList<>();
            while( listenersIterator.hasNext() )
            {
                TransitionListener listener = listenersIterator.next();
                try
                {
                    // keep track of executed listeners, in reverse order of execution (this is unrelated to the transition-direction!)
                    executedListeners.add( 0, listener );

                    // execute the listener
                    listener.execute( origin, target );
                }
                catch( Throwable e )
                {
                    if( vetoable )
                    {
                        // failure to execute action - revert!
                        for( TransitionListener executedListener : executedListeners )
                        {
                            try
                            {
                                executedListener.revert( origin, target );
                            }
                            catch( Throwable revertException )
                            {
                                this.logger.warn( "Transition listener {} failed to revert the transition of {} from {} to {}",
                                                  listener, this, origin, target, revertException );
                            }
                        }

                        // revert our status
                        this.status = origin;

                        // rethrow the vetoing exception
                        if( e instanceof WorkflowException )
                        {
                            throw ( WorkflowException ) e;
                        }
                        else
                        {
                            throw new TransitionException( "Transition listener " + listener + " failed while transitioning " + this + " from " + origin + " to " + target, e, this, origin, target );
                        }
                    }
                    else
                    {
                        this.logger.warn( "Transition listener {} failed while transitioning {} from {} to {}, but transition is not vetoable",
                                          listener, this, origin, target, e );
                    }
                }
            }
        }
        finally
        {
            this.lock.releaseWriteLock();
        }
    }

    public final void acquireWriteLock()
    {
        this.lock.acquireWriteLock();
    }

    public final void releaseWriteLock()
    {
        this.lock.releaseWriteLock();
    }

    public final void acquireReadLock()
    {
        this.lock.acquireReadLock();
    }

    public final void releaseReadLock()
    {
        this.lock.releaseReadLock();
    }

    public static enum TransitionDirection
    {
        FORWARD,
        BACKWARDS
    }
}

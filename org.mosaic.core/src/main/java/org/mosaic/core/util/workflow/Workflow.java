package org.mosaic.core.util.workflow;

import java.util.*;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.core.util.base.ToStringHelper;
import org.mosaic.core.util.concurrency.ReadWriteLock;
import org.mosaic.core.util.logging.Logging;
import org.slf4j.Logger;

import static java.util.Collections.emptyIterator;
import static java.util.Collections.unmodifiableSet;

/**
 * @author arik
 */
public class Workflow
{
    private static class Action
    {
        @Nullable
        private final TransitionChangeAction execute;

        @Nullable
        private final TransitionChangeAction revert;

        private Action( @Nullable TransitionChangeAction execute, @Nullable TransitionChangeAction revert )
        {
            this.execute = execute;
            this.revert = revert;
        }
    }

    @Nonnull
    protected final Logger logger;

    @Nonnull
    private final ReadWriteLock lock;

    @Nonnull
    private final String name;

    @Nonnull
    private final Map<Status, Map<Status, TransitionDirection>> statuses = new HashMap<>();

    @Nonnull
    private final Map<Status, Deque<TransitionChangeAction>> validators = new HashMap<>();

    @Nonnull
    private final Map<Status, Deque<Action>> actions = new HashMap<>();

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
    public Logger getLogger()
    {
        return this.logger;
    }

    @Nonnull
    public ReadWriteLock getLock()
    {
        return this.lock.read( () -> this.lock );
    }

    @Nonnull
    public final String getName()
    {
        return this.lock.read( () -> this.name );
    }

    @Nonnull
    public final Set<Status> getStatuses()
    {
        return this.lock.read( () -> unmodifiableSet( new HashSet<>( this.statuses.keySet() ) ) );
    }

    public final void addStatus( @Nonnull Status status )
    {
        this.lock.write( () -> {
            if( !this.statuses.containsKey( status ) )
            {
                this.statuses.put( status, new HashMap<>() );
            }
        } );
    }

    @Nonnull
    public final Set<Status> getTargetStatusesFor( @Nonnull Status origin )
    {
        return this.lock.read( () -> {
            Map<Status, TransitionDirection> transitions = this.statuses.get( origin );
            return transitions == null ? Collections.<Status>emptySet() : unmodifiableSet( new HashSet<Status>( transitions.keySet() ) );
        } );
    }

    @Nonnull
    public final Set<Status> getOriginStatusesFor( @Nonnull Status target )
    {
        return this.lock.read( () -> {
            Set<Status> originStatuses = new HashSet<>();
            for( Map.Entry<Status, Map<Status, TransitionDirection>> entry : this.statuses.entrySet() )
            {
                if( entry.getValue().containsKey( target ) )
                {
                    originStatuses.add( entry.getKey() );
                }
            }

            Set<Status> s = new HashSet<>( originStatuses );
            return unmodifiableSet( s );
        } );
    }

    public final boolean isTransitionAllowed( @Nonnull Status origin, @Nonnull Status target )
    {
        return this.lock.read( () -> {
            Map<Status, TransitionDirection> transitions = this.statuses.get( origin );
            return transitions != null && transitions.containsKey( target );
        } );
    }

    public final void addTransition( @Nonnull Status origin,
                                     @Nonnull Status target,
                                     @Nonnull TransitionDirection direction )
    {
        this.lock.write( () -> {
            Map<Status, TransitionDirection> transitions = this.statuses.get( origin );
            if( transitions == null )
            {
                transitions = new HashMap<>();
                this.statuses.put( origin, transitions );
            }
            transitions.put( target, direction );
        } );
    }

    public final void addValidation( @Nonnull Status target, @Nonnull TransitionChangeAction validator )
    {
        this.lock.write( () -> {
            Deque<TransitionChangeAction> validators = this.validators.get( target );
            if( validators == null )
            {
                validators = new LinkedList<>();
                this.validators.put( target, validators );
            }
            validators.add( validator );
        } );
    }

    public final void addAction( @Nonnull Status target, @Nullable TransitionChangeAction action )
    {
        addAction( target, action, null );
    }

    public final void addAction( @Nonnull Status target,
                                 @Nullable TransitionChangeAction action,
                                 @Nullable TransitionChangeAction revert )
    {
        this.lock.write( () -> {
            Deque<Action> actions = this.actions.get( target );
            if( actions == null )
            {
                actions = new LinkedList<>();
                this.actions.put( target, actions );
            }
            actions.add( new Action( action, revert ) );
        } );
    }

    @Nonnull
    public final Status getStatus()
    {
        return this.lock.read( () -> this.status );
    }

    public final void transitionTo( @Nonnull Status target, boolean vetoable )
    {
        this.lock.write( () -> {

            // save the origin status
            Status origin = this.status;
            TransitionContext context = new TransitionContext( this, origin, target );
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
                Deque<TransitionChangeAction> validators = this.validators.get( target );
                if( validators != null )
                {
                    for( TransitionChangeAction action : validators )
                    {
                        try
                        {
                            action.execute( context );
                        }
                        catch( Throwable e )
                        {
                            if( e instanceof WorkflowException )
                            {
                                throw ( WorkflowException ) e;
                            }
                            else
                            {
                                throw new TransitionException( "transition of " + this + " from " + origin + " to " + target + " was vetoed by " + action, e, this, origin, target );
                            }
                        }
                    }
                }
            }

            // change status
            this.status = target;

            // get listeners in appropriate direction
            Deque<Action> actions = this.actions.get( target );
            Iterator<Action> actionIterator;
            if( actions == null )
            {
                actionIterator = emptyIterator();
            }
            else if( direction == TransitionDirection.FORWARD )
            {
                actionIterator = actions.iterator();
            }
            else
            {
                actionIterator = actions.descendingIterator();
            }
            List<Action> executedActions = new LinkedList<>();
            while( actionIterator.hasNext() )
            {
                Action action = actionIterator.next();
                try
                {
                    // keep track of executed listeners, in reverse order of execution (this is unrelated to the transition-direction!)
                    executedActions.add( 0, action );

                    // execute the listener
                    TransitionChangeAction executor = action.execute;
                    if( executor != null )
                    {
                        executor.execute( context );
                    }
                }
                catch( Throwable e )
                {
                    if( vetoable )
                    {
                        // failure to execute action - revert!
                        for( Action executedAction : executedActions )
                        {
                            TransitionChangeAction revertor = executedAction.revert;
                            if( revertor != null )
                            {
                                try
                                {
                                    revertor.execute( context );
                                }
                                catch( Throwable revertException )
                                {
                                    this.logger.warn( "Transition listener {} failed to revert the transition of {} from {} to {}",
                                                      action, this, origin, target, revertException );
                                }
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
                            throw new TransitionException( "Transition listener " + action + " failed while transitioning " + this + " from " + origin + " to " + target, e, this, origin, target );
                        }
                    }
                    else
                    {
                        this.logger.warn( "Transition listener {} failed while transitioning {} from {} to {}, but transition is not vetoable",
                                          action, this, origin, target, e );
                    }
                }
            }
        } );
    }

    public static enum TransitionDirection
    {
        FORWARD,
        BACKWARDS
    }
}

package org.mosaic.modules.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
class Lifecycle
{
    private static final Logger LOG = LoggerFactory.getLogger( Lifecycle.class );

    @Nonnull
    private final List<Lifecycle> children = new LinkedList<>();

    @Nonnull
    private final List<Lifecycle> startedChildren = new LinkedList<>();

    @Nonnull
    private final List<Lifecycle> activatedChildren = new LinkedList<>();

    private boolean started;

    private boolean activated;

    public final boolean isStarted()
    {
        return this.started;
    }

    public final boolean isActivated()
    {
        return this.activated;
    }

    public final synchronized void start()
    {
        if( this.started )
        {
            return;
        }

        onBeforeStart();

        this.startedChildren.clear();
        for( Lifecycle child : this.children )
        {
            try
            {
                child.start();
                this.startedChildren.add( child );
            }
            catch( Throwable e )
            {
                stop();
                throw e;
            }
        }
        this.started = true;

        onAfterStart();
    }

    public final synchronized void stop()
    {
        this.started = false;

        onBeforeStop();

        List<Lifecycle> reverseStartedChildren = new LinkedList<>( this.startedChildren );
        Collections.reverse( reverseStartedChildren );
        for( Lifecycle child : reverseStartedChildren )
        {
            try
            {
                child.stop();
            }
            catch( Throwable e )
            {
                LOG.error( "Could not stop child {} of parent {}: {}", child, this, e.getMessage(), e );
            }
            this.startedChildren.remove( child );
        }

        onAfterStop();
    }

    public final synchronized boolean canActivate()
    {
        return this.started && !this.activated && canActivateInternal() && canActivateChildren();
    }

    public final synchronized boolean canActivateChildren()
    {
        for( Lifecycle child : this.children )
        {
            if( !child.canActivate() )
            {
                return false;
            }
        }
        return true;
    }

    public final synchronized void activate()
    {
        if( !canActivate() )
        {
            return;
        }

        this.activated = true;
        onBeforeActivate();

        this.activatedChildren.clear();
        for( Lifecycle child : this.children )
        {
            try
            {
                child.activate();
                this.activatedChildren.add( child );
            }
            catch( Throwable e )
            {
                deactivate();
                throw e;
            }
        }

        onAfterActivate();
    }

    public final synchronized void deactivate()
    {
        if( this.activated )
        {
            onBeforeDeactivate();

            List<Lifecycle> reverseActivatedChildren = new LinkedList<>( this.activatedChildren );
            Collections.reverse( reverseActivatedChildren );
            for( Lifecycle child : reverseActivatedChildren )
            {
                try
                {
                    child.deactivate();
                }
                catch( Throwable e )
                {
                    LOG.error( "Could not deactivate child {} of parent {}: {}", child, this, e.getMessage(), e );
                }
                this.activatedChildren.remove( child );
            }

            this.activated = false;
            onAfterDeactivate();
        }
    }

    @Nonnull
    public final List<Lifecycle> getInactivatables()
    {
        List<Lifecycle> inactivatableChildren = new LinkedList<>();
        if( !canActivateInternal() )
        {
            inactivatableChildren.add( this );
        }

        for( Lifecycle child : this.children )
        {
            inactivatableChildren.addAll( child.getInactivatables() );
        }
        return inactivatableChildren;
    }

    @Nullable
    public final <T> T getChild( @Nonnull Class<T> type )
    {
        for( Lifecycle child : this.children )
        {
            if( type.isInstance( child ) )
            {
                return type.cast( child );
            }
        }
        return null;
    }

    @Nonnull
    public final <T> T requireChild( @Nonnull Class<T> type )
    {
        T child = getChild( type );
        if( child == null )
        {
            throw new IllegalArgumentException( "no child of type " + type.getName() + " found in parent " + this );
        }
        else
        {
            return child;
        }
    }

    @Nonnull
    public final <T> List<T> getChildren( @Nonnull Class<T> type, boolean includeGrandChildren )
    {
        List<T> children = null;
        for( Lifecycle child : this.children )
        {
            if( type.isInstance( child ) )
            {
                if( children == null )
                {
                    children = new LinkedList<>();
                }
                children.add( type.cast( child ) );
            }
            if( includeGrandChildren )
            {
                if( children == null )
                {
                    children = new LinkedList<>();
                }
                children.addAll( child.getChildren( type, true ) );
            }
        }
        return children == null ? Collections.<T>emptyList() : children;
    }

    protected synchronized boolean canActivateInternal()
    {
        return true;
    }

    protected synchronized void addChild( @Nonnull Lifecycle child )
    {
        if( this.started || this.activated )
        {
            throw new IllegalStateException( "lifecycle already started" );
        }
        this.children.add( child );
    }

    protected synchronized void removeChild( @Nonnull Lifecycle child )
    {
        if( this.started || this.activated )
        {
            throw new IllegalStateException( "lifecycle started, cannot remove children" );
        }
        this.children.remove( child );
    }

    protected synchronized void clearChildren()
    {
        if( this.started || this.activated )
        {
            throw new IllegalStateException( "lifecycle started, cannot remove children" );
        }
        this.children.clear();
    }

    protected synchronized void onBeforeStart()
    {
        // no-op
    }

    protected synchronized void onAfterStart()
    {
        // no-op
    }

    protected synchronized void onBeforeStop()
    {
        // no-op
    }

    protected synchronized void onAfterStop()
    {
        // no-op
    }

    protected synchronized void onBeforeActivate()
    {
        // no-op
    }

    protected synchronized void onAfterActivate()
    {
        // no-op
    }

    protected synchronized void onBeforeDeactivate()
    {
        // no-op
    }

    protected synchronized void onAfterDeactivate()
    {
        // no-op
    }

    @Nonnull
    protected final BundleContext getMyBundleContext()
    {
        Bundle modulesBundle = FrameworkUtil.getBundle( getClass() );
        if( modulesBundle == null )
        {
            throw new IllegalStateException( "could not find 'org.mosaic.modules' bundle" );
        }

        BundleContext bundleContext = modulesBundle.getBundleContext();
        if( bundleContext == null )
        {
            throw new IllegalStateException( "could not obtain bundle context for 'org.mosaic.modules' bundle" );
        }
        return bundleContext;
    }
}

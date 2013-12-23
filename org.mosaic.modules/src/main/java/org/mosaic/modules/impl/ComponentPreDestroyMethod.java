package org.mosaic.modules.impl;

import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
final class ComponentPreDestroyMethod extends Lifecycle
{
    private static final Logger LOG = LoggerFactory.getLogger( ComponentPreDestroyMethod.class );

    @Nonnull
    private final Component component;

    @Nonnull
    private final Method method;

    ComponentPreDestroyMethod( @Nonnull Component component, @Nonnull Method method )
    {
        this.component = component;
        this.method = method;
        this.method.setAccessible( true );
    }

    @Override
    public String toString()
    {
        return "PreDestroyMethod[" + this.method.getName() + " of " + this.component + "]";
    }

    @Override
    protected synchronized void onAfterDeactivate()
    {
        Object instance = this.component.getInstance();
        if( instance == null )
        {
            LOG.warn( "Could not invoke @PreDestroy method {} - instance already disposed", this );
            return;
        }

        try
        {
            this.method.invoke( instance );
        }
        catch( Throwable e )
        {
            LOG.warn( "The @PreDestroy method {} threw an exception: {}", this, e.getMessage(), e );
        }
    }
}

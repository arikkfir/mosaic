package org.mosaic.util.osgi;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public abstract class BundleActivatorAggregate implements BundleActivator
{
    private static final Logger LOG = LoggerFactory.getLogger( BundleActivatorAggregate.class );

    @Nullable
    private List<BundleActivator> targets;

    @Nullable
    private List<BundleActivator> invoked;

    @Override
    public void start( @Nonnull BundleContext context ) throws Exception
    {
        // clear the 'invoked' flags
        this.invoked = null;

        if( this.targets != null )
        {
            Throwable error = null;
            for( BundleActivator target : this.targets )
            {
                setInvoked( target );
                try
                {
                    target.start( context );
                }
                catch( Throwable e )
                {
                    error = e;
                    break;
                }
            }

            if( error != null )
            {
                LOG.error( error.getMessage(), error );
                stop( context );
            }
        }
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        if( this.invoked != null )
        {
            for( BundleActivator activator : this.invoked )
            {
                try
                {
                    activator.stop( context );
                }
                catch( Exception e )
                {
                    LOG.error( "Error while stopping activator '{}': {}", activator, e.getMessage(), e );
                }
            }
        }
        this.invoked = null;
    }

    protected void addTarget( @Nonnull BundleActivator activator )
    {
        if( this.targets == null )
        {
            this.targets = new LinkedList<>();
        }
        this.targets.add( activator );
    }

    private void setInvoked( @Nonnull BundleActivator activator )
    {
        if( this.invoked == null )
        {
            this.invoked = new LinkedList<>();
        }
        this.invoked.add( 0, activator );
    }
}

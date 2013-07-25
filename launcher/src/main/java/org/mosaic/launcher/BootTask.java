package org.mosaic.launcher;

import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public abstract class BootTask
{
    private static final Logger LOG = LoggerFactory.getLogger( BootTask.class );

    @Nonnull
    private final String name;

    private boolean executed;

    protected BootTask( @Nonnull String name )
    {
        this.name = name;
    }

    public final boolean execute()
    {
        this.executed = true;
        LOG.debug( "Executing boot phase: {}", this.name );
        try
        {
            executeInternal();
            return true;
        }
        catch( Exception e )
        {
            LOG.error( e.getMessage(), e );
            return false;
        }
    }

    public final void revert()
    {
        if( this.executed )
        {
            try
            {
                LOG.debug( "Reverting boot phase: {}", this.name );
                revertInternal();
            }
            catch( Exception e )
            {
                LOG.error( "Boot task '{}' failed reverting: {}", this.name, e.getMessage(), e );
            }
        }
    }

    @Override
    public String toString()
    {
        return "BootTask[" + this.name + "]";
    }

    @Nonnull
    public String getName()
    {
        return this.name;
    }

    protected abstract void executeInternal() throws Exception;

    protected void revertInternal() throws Exception
    {
        // no-op
    }
}

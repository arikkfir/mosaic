package org.mosaic.shell.impl.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class InputQueueThread extends Thread
{
    private static final Logger LOG = LoggerFactory.getLogger( InputQueueThread.class );

    @Nonnull
    private final InputStream in;

    @Nonnull
    private final BlockingQueue<Integer> bufferedInputQueue;

    public InputQueueThread( @Nonnull InputStream in )
    {
        super( "ShellServer InputQueue" );
        this.in = in;
        this.bufferedInputQueue = new LinkedBlockingQueue<>( 100000 );
    }

    @Nonnull
    public BlockingQueue<Integer> getBufferedInputQueue()
    {
        return bufferedInputQueue;
    }

    public void end()
    {
        this.bufferedInputQueue.offer( -1 );
    }

    @Override
    public void run()
    {
        while( !currentThread().isInterrupted() )
        {
            try
            {
                int i = this.in.read();
                this.bufferedInputQueue.put( i );

                if( i == -1 )
                {
                    LOG.debug( "EOF received from input - closing input queue thread" );
                    break;
                }
            }
            catch( InterruptedException e )
            {
                LOG.debug( "Input queue thread interrupted" );
                break;
            }
            catch( InterruptedIOException e )
            {
                LOG.debug( "Input queue thread interrupted" );
                break;
            }
            catch( IOException e )
            {
                LOG.error( "I/O error on input queue thread: {}", e.getMessage(), e );
                break;
            }
        }
    }
}

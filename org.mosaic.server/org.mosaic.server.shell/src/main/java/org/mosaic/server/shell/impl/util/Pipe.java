package org.mosaic.server.shell.impl.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.BlockingQueue;
import org.mosaic.util.logging.Logger;
import org.mosaic.util.logging.LoggerFactory;

import static java.lang.Thread.currentThread;

/**
 * @author arik
 */
public class Pipe implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger( Pipe.class );

    private final InputStream in;

    private final BlockingQueue<Integer> inputQueue;

    public Pipe( InputStream in, BlockingQueue<Integer> inputQueue ) {
        this.in = in;
        this.inputQueue = inputQueue;
    }

    @Override
    public void run() {
        while( !currentThread().isInterrupted() ) {

            try {
                int i = in.read();
                inputQueue.put( i );

                if( i == -1 ) {
                    LOG.debug( "EOF received in pipe - closing pipe thread" );
                    break;
                }

            } catch( InterruptedException | InterruptedIOException e ) {
                LOG.debug( "Pipe thread interrupted" );
                break;
            } catch( IOException e ) {
                LOG.error( "I/O error on pipe thread: {}", e.getMessage(), e );
                break;
            }

        }
    }
}

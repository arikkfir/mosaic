package org.mosaic.server.shell.impl.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

/**
 * @author arik
 */
public class PipeInputStream extends InputStream {

    private final BlockingQueue<Integer> inputQueue;

    public PipeInputStream( BlockingQueue<Integer> inputQueue ) {
        this.inputQueue = inputQueue;
    }

    @Override
    public int read() throws IOException {
        try {
            return inputQueue.take();
        } catch( InterruptedException e ) {
            return -1;
        }
    }
}

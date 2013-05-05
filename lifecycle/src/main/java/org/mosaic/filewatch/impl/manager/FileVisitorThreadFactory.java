package org.mosaic.filewatch.impl.manager;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class FileVisitorThreadFactory implements ThreadFactory
{
    private AtomicInteger index = new AtomicInteger( 0 );

    @SuppressWarnings("NullableProblems")
    @Override
    public Thread newThread( @Nonnull Runnable r )
    {
        Thread t = new Thread( r, "FileScanner-" + index.incrementAndGet() );
        t.setPriority( Thread.MIN_PRIORITY );
        t.setDaemon( true );
        return t;
    }
}

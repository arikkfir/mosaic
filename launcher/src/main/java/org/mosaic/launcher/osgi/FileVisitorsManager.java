package org.mosaic.launcher.osgi;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.getInteger;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static java.nio.file.Files.*;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.framework.FrameworkUtil.createFilter;

/**
 * @author arik
 */
public class FileVisitorsManager implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger( FileVisitorsManager.class );

    private static class FileVisitorThreadFactory implements ThreadFactory
    {
        private AtomicInteger index = new AtomicInteger( 0 );

        @SuppressWarnings("NullableProblems")
        @Override
        public Thread newThread( Runnable r )
        {
            Thread t = new Thread( r, "FileScanner-" + index.incrementAndGet() );
            t.setPriority( Thread.MIN_PRIORITY );
            t.setDaemon( true );
            return t;
        }
    }

    @Nullable
    private BundleContext bundleContext;

    @Nullable
    private ServiceTracker<FileVisitor<Path>, FileVisitorAdapter> visitorsTracker;

    @Nullable
    private ScheduledExecutorService scheduler;

    public void start( @Nonnull BundleContext bundleContext )
    {
        if( this.bundleContext != null )
        {
            throw new IllegalStateException( "Already started" );
        }

        // save our bundle context
        this.bundleContext = bundleContext;

        // create our file-visitors tracker
        try
        {
            String filter = format( "(&(objectClass=%s)(root=*))", FileVisitor.class.getName() );
            this.visitorsTracker = new ServiceTracker<>( this.bundleContext,
                                                         createFilter( filter ),
                                                         new FileVisitorCustomizer() );
        }
        catch( InvalidSyntaxException e )
        {
            throw new IllegalStateException( "Could not create a FileVisitor filter from '" + e.getFilter() + "': " + e.getMessage(), e );
        }

        // start tracking all known visitors
        this.visitorsTracker.open();

        // schedule a pass every second
        this.scheduler = newSingleThreadScheduledExecutor( new FileVisitorThreadFactory() );
        this.scheduler.scheduleWithFixedDelay( this, 1, 1, SECONDS );
    }

    public void stop()
    {
        // close our visitors tracker
        if( this.visitorsTracker != null )
        {
            this.visitorsTracker.close();
        }
        this.visitorsTracker = null;

        // shutdown our scheduler so no more scans will occur
        if( this.scheduler != null )
        {
            this.scheduler.shutdownNow();
        }
        this.scheduler = null;

        // clear our bundle context
        this.bundleContext = null;
    }

    @Override
    public void run()
    {
        if( this.visitorsTracker != null )
        {
            for( FileVisitorAdapter adapter : this.visitorsTracker.getTracked().values() )
            {
                adapter.run();
            }
        }
    }

    private class FileVisitorAdapter implements Runnable, FileVisitor<Path>
    {
        private final Path root;

        private final ServiceReference<FileVisitor<Path>> reference;

        private final Map<Path, Long> knownFiles;

        private final boolean skipSvn;

        private final boolean skipHidden;

        private FileVisitor<Path> visitor;

        private FileVisitorAdapter( ServiceReference<FileVisitor<Path>> reference )
        {
            this.reference = reference;

            // determine our root directory - or revert to null and issue a warning in the logs
            Object rootValue = reference.getProperty( "root" );
            if( rootValue != null )
            {
                this.root = Paths.get( rootValue.toString() );
            }
            else
            {
                this.root = null;
                LOG.warn( "File visitor service '{}' does not have the 'root' service property - it will not be activated", this.reference );
            }

            // determine whether service needs modification filtering
            Object modOnlyValue = reference.getProperty( "modificationsOnly" );
            if( modOnlyValue == null || parseBoolean( modOnlyValue.toString() ) )
            {
                this.knownFiles = new HashMap<>();
            }
            else
            {
                this.knownFiles = null;
            }

            // determine whether service needs modification filtering
            Object skipSvnValue = reference.getProperty( "skipSvn" );
            this.skipSvn = skipSvnValue == null || parseBoolean( skipSvnValue.toString() );

            // determine whether service needs modification filtering
            Object skipHiddenValue = reference.getProperty( "skipHidden" );
            this.skipHidden = skipHiddenValue == null || parseBoolean( skipHiddenValue.toString() );
        }

        @Override
        public void run()
        {
            if( this.root != null )
            {
                // obtain the service and save it as an instance field - its ok since we know WE ARE NOT RE-ENTRANT
                this.visitor = getFileVisitor( this.reference );
                if( this.visitor != null && exists( this.root ) && isDirectory( this.root ) && isReadable( this.root ) )
                {
                    // we have the visitor - now scan its root;
                    // we use 'this' as the actual visitor, which applies certain logic such as modifications tracking
                    try
                    {
                        // notify visitor that scanning is about to start; we do this by calling 'preVisitDirectory'
                        // with a null Path object - not the nicest trick but "does the trick" :)
                        this.visitor.preVisitDirectory( null, null );

                        // walk our root directory
                        walkFileTree( this.root, EnumSet.of( FOLLOW_LINKS ), getInteger( "maxFileScanDepth", 512 ), this );

                        // detect file deletions
                        if( this.knownFiles != null )
                        {
                            for( Iterator<Path> iterator = this.knownFiles.keySet().iterator(); iterator.hasNext(); )
                            {
                                Path file = iterator.next();
                                if( notExists( file ) )
                                {
                                    // file was deleted - remove it from our known file modifications map, and notify the visitor
                                    iterator.remove();
                                    try
                                    {
                                        this.visitor.visitFile( file, null );
                                    }
                                    catch( IOException e )
                                    {
                                        this.visitor.visitFileFailed( file, e );
                                    }
                                }
                            }
                        }

                        // notify visitor that the scanning has completed; we do this by calling 'postVisitDirectory'
                        // with a null Path object.. again - not the nicest trick but...
                        postVisitDirectory( null, null );
                    }
                    catch( IOException e )
                    {
                        // notify visitor that the scanning has completed with an error; we do this by calling
                        // 'postVisitDirectory' with a null Path object and the exception.
                        try
                        {
                            postVisitDirectory( null, e );
                        }
                        catch( IOException e1 )
                        {
                            LOG.error( "File visitor '{}' threw an error from scan-completion hook: {}", this.visitor, e1.getMessage(), e1 );
                        }
                    }
                    finally
                    {
                        // release the service
                        ungetFileVisitor( this.reference );
                        this.visitor = null;
                    }
                }
            }
        }

        @Override
        public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException
        {
            if( this.skipSvn && dir.getFileName().toString().equalsIgnoreCase( ".svn" ) )
            {
                return FileVisitResult.SKIP_SUBTREE;
            }
            else if( this.skipHidden && isHidden( dir ) )
            {
                return FileVisitResult.SKIP_SUBTREE;
            }
            else
            {
                return this.visitor.preVisitDirectory( dir, attrs );
            }
        }

        @Override
        public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException
        {
            if( this.knownFiles == null )
            {
                // this service does not need modification tracking - just delegate and return
                return this.visitor.visitFile( file, attrs );
            }

            // take the actual up-to-date modification time; ignore files modified less than 2 seconds ago
            // because they might still being written to...
            long actualModificationTime = attrs.lastModifiedTime().toMillis();
            if( actualModificationTime >= currentTimeMillis() - getInteger( "fileModificationGuard", 2000 ) )
            {
                // skip - probably file is being written to...
                return FileVisitResult.CONTINUE;
            }

            // check whether this is a new or modified file
            Long previousModificationTime = this.knownFiles.put( file, actualModificationTime );
            if( previousModificationTime == null )
            {
                // first encounter with this file
                return this.visitor.visitFile( file, attrs );
            }

            // if the actual modification time is greater than the modification time from the previous scan - file has been modified! :)
            if( previousModificationTime < actualModificationTime )
            {
                // file has been modified!
                return this.visitor.visitFile( file, attrs );
            }

            // continue to next file
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed( Path file, IOException exc ) throws IOException
        {
            return this.visitor.visitFileFailed( file, exc );
        }

        @Override
        public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException
        {
            return this.visitor.postVisitDirectory( dir, exc );
        }

        private FileVisitor<Path> getFileVisitor( ServiceReference<FileVisitor<Path>> reference )
        {
            if( bundleContext != null )
            {
                FileVisitor<Path> visitor;
                try
                {
                    visitor = bundleContext.getService( reference );
                    if( visitor != null )
                    {
                        return visitor;
                    }
                }
                catch( Exception e )
                {
                    LOG.error( "Could not obtain file visitor '{}': {}", reference, e.getMessage(), e );
                }
            }
            return null;
        }

        private void ungetFileVisitor( ServiceReference<FileVisitor<Path>> reference )
        {
            if( bundleContext != null )
            {
                try
                {
                    bundleContext.ungetService( reference );
                }
                catch( Exception ignore )
                {
                }
            }
        }
    }

    private class FileVisitorCustomizer implements ServiceTrackerCustomizer<FileVisitor<Path>, FileVisitorAdapter>
    {
        @Override
        public FileVisitorAdapter addingService( ServiceReference<FileVisitor<Path>> reference )
        {
            FileVisitorAdapter adapter = new FileVisitorAdapter( reference );
            try
            {
                adapter.run();
                return adapter;
            }
            catch( Exception e )
            {
                LOG.warn( "Could not register file visitor: {}", e.getMessage(), e );
                return null;
            }
        }

        @Override
        public void modifiedService( ServiceReference<FileVisitor<Path>> reference, FileVisitorAdapter service )
        {
            // no-op
        }

        @Override
        public void removedService( ServiceReference<FileVisitor<Path>> reference,
                                    FileVisitorAdapter service )
        {
            // no-op
        }
    }
}

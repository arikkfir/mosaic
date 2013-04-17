package org.mosaic.util.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class FileVisitorAdapter implements FileVisitor<Path>
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Nonnull
    @Override
    public final FileVisitResult preVisitDirectory( @Nullable Path dir, @Nullable BasicFileAttributes attrs )
            throws IOException
    {
        if( dir == null )
        {
            if( attrs == null )
            {
                return onScanStarted();
            }
            else
            {
                throw new IllegalArgumentException( "Attributes MUST be null when directory is null" );
            }
        }
        else if( attrs == null )
        {
            throw new IllegalArgumentException( "Attributes must NOT be null when directory is not null" );
        }
        else
        {
            return onPreDirectory( dir, attrs );
        }
    }

    @Nonnull
    @Override
    public final FileVisitResult visitFile( @Nonnull Path file, @Nullable BasicFileAttributes attrs ) throws IOException
    {
        if( attrs == null )
        {
            return onFileDeleted( file );
        }
        else
        {
            return onFileModified( file, attrs );
        }
    }

    @Nonnull
    @Override
    public final FileVisitResult visitFileFailed( @Nonnull Path file, @Nonnull IOException exc ) throws IOException
    {
        return onError( file, exc );
    }

    @Nonnull
    @Override
    public final FileVisitResult postVisitDirectory( @Nullable Path dir, @Nullable IOException exc ) throws IOException
    {
        if( dir != null )
        {
            return onPostDirectory( dir, exc );
        }
        else
        {
            return onScanCompleted( exc );
        }
    }

    protected FileVisitResult onScanStarted() throws IOException
    {
        if( this.logger.isTraceEnabled() )
        {
            this.logger.trace( "File scan started" );
        }
        return FileVisitResult.CONTINUE;
    }

    protected FileVisitResult onScanCompleted( @Nullable IOException exception ) throws IOException
    {
        if( exception != null )
        {
            if( this.logger.isTraceEnabled() )
            {
                this.logger.trace( "File scan completed with error: {}", exception.getMessage(), exception );
            }
            throw exception;
        }
        else
        {
            if( this.logger.isTraceEnabled() )
            {
                this.logger.trace( "File scan completed" );
            }
            return FileVisitResult.CONTINUE;
        }
    }

    protected FileVisitResult onPreDirectory( @Nonnull
                                              Path dir,

                                              @SuppressWarnings( "UnusedParameters" )
                                              @Nonnull
                                              BasicFileAttributes attrs ) throws IOException
    {
        if( this.logger.isTraceEnabled() )
        {
            this.logger.trace( "Entering directory '{}'", dir );
        }
        return FileVisitResult.CONTINUE;
    }

    protected FileVisitResult onPostDirectory( @Nonnull Path dir, @Nullable IOException exc )
            throws IOException
    {
        if( this.logger.isTraceEnabled() )
        {
            if( exc != null )
            {
                this.logger.trace( "Exiting directory '{}' with exception: {}", dir, exc.getMessage(), exc );
            }
            else
            {
                this.logger.trace( "Exiting directory '{}'", dir );
            }
        }
        return FileVisitResult.CONTINUE;
    }

    protected FileVisitResult onFileDeleted( @Nonnull Path file ) throws IOException
    {
        if( this.logger.isTraceEnabled() )
        {
            this.logger.trace( "File deleted: {}", file );
        }
        return FileVisitResult.CONTINUE;
    }

    protected FileVisitResult onFileModified( @Nonnull
                                              Path file,

                                              @SuppressWarnings( "UnusedParameters" )
                                              @Nonnull
                                              BasicFileAttributes attrs ) throws IOException
    {
        if( this.logger.isTraceEnabled() )
        {
            this.logger.trace( "File modified: {}", file );
        }
        return FileVisitResult.CONTINUE;
    }

    protected FileVisitResult onError( @SuppressWarnings( "UnusedParameters" )
                                       @Nonnull
                                       Path file,

                                       @Nonnull
                                       IOException exception ) throws IOException
    {
        throw exception;
    }
}

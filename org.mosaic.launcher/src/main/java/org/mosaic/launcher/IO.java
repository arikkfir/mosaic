package org.mosaic.launcher;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
final class IO
{
    public static void deletePath( @Nonnull Path path ) throws IOException
    {
        if( Files.isDirectory( path ) )
        {
            Files.walkFileTree( path, new SimpleFileVisitor<Path>()
            {
                @Nonnull
                @Override
                public FileVisitResult visitFile( @Nonnull Path file, @Nonnull BasicFileAttributes attrs ) throws IOException
                {
                    Files.delete( file );
                    return FileVisitResult.CONTINUE;
                }

                @Nonnull
                @Override
                public FileVisitResult postVisitDirectory( @Nonnull Path dir, @Nullable IOException exc ) throws IOException
                {
                    if( exc != null )
                    {
                        throw exc;
                    }
                    Files.delete( dir );
                    return FileVisitResult.CONTINUE;
                }
            } );
        }
        else
        {
            Files.delete( path );
        }
    }

    private IO()
    {
    }
}

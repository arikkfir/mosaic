package org.mosaic.util.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author arik
 */
public class FileVisitorAdapter implements FileVisitor<Path>
{
    @Override
    public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException
    {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException
    {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed( Path file, IOException exc ) throws IOException
    {
        throw exc;
    }

    @Override
    public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException
    {
        if( exc != null )
        {
            throw exc;
        }
        else
        {
            return FileVisitResult.CONTINUE;
        }
    }
}

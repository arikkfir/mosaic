package org.mosaic.launcher;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import javax.annotation.Nonnull;
import org.osgi.framework.Bundle;

/**
 * @author arik
 */
final class ModuleFileStore extends FileStore
{
    @Nonnull
    private final Bundle bundle;

    ModuleFileStore( @Nonnull Bundle bundle )
    {
        this.bundle = bundle;
    }

    @Override
    public String name()
    {
        return this.bundle.getSymbolicName();
    }

    @Override
    public String type()
    {
        return "module";
    }

    @Override
    public boolean isReadOnly()
    {
        return true;
    }

    @Override
    public long getTotalSpace() throws IOException
    {
        try
        {
            String location = this.bundle.getLocation();
            Path file = Paths.get( location );
            return Files.size( file );
        }
        catch( Exception e )
        {
            return 0;
        }
    }

    @Override
    public long getUsableSpace() throws IOException
    {
        return getTotalSpace();
    }

    @Override
    public long getUnallocatedSpace() throws IOException
    {
        return 0;
    }

    @Override
    public boolean supportsFileAttributeView( Class<? extends FileAttributeView> type )
    {
        Preconditions.checkNotNull( type, "type" );
        return type == BasicFileAttributeView.class;
    }

    @Override
    public boolean supportsFileAttributeView( String name )
    {
        return name.equals( "basic" );
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView( Class<V> type )
    {
        // that's how UnixFileStore implements this... apparently it's not needed anywhere
        Preconditions.checkNotNull( type, "type" );
        return null;
    }

    @Override
    public Object getAttribute( String attribute ) throws IOException
    {
        switch( attribute )
        {
            case "totalSpace":
                return getTotalSpace();
            case "usableSpace":
                return getUsableSpace();
            case "unallocatedSpace":
                return getUnallocatedSpace();
            default:
                throw new UnsupportedOperationException( "'" + attribute + "' not recognized" );
        }
    }

    @Override
    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        else if( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ModuleFileStore that = ( ModuleFileStore ) o;
        return this.bundle.equals( that.bundle );
    }

    @Override
    public int hashCode()
    {
        return this.bundle.hashCode();
    }
}

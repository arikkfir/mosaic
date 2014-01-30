package org.mosaic.launcher;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A file system provider for modules.
 * <p/>
 * URI syntax should be: {@code module://{moduleId}{pathInModule}}
 *
 * @author arik
 */
public class ModuleFileSystemProvider extends FileSystemProvider
{
    private static final Logger LOG = LoggerFactory.getLogger( ModuleFileSystemProvider.class );

    private static final Set<? extends OpenOption> RESTRICTED_OPEN_OPTIONS =
            Collections.unmodifiableSet(
                    Sets.newHashSet( StandardOpenOption.APPEND,
                                     StandardOpenOption.CREATE,
                                     StandardOpenOption.CREATE_NEW,
                                     StandardOpenOption.DELETE_ON_CLOSE,
                                     StandardOpenOption.DSYNC,
                                     StandardOpenOption.SPARSE,
                                     StandardOpenOption.SYNC,
                                     StandardOpenOption.TRUNCATE_EXISTING,
                                     StandardOpenOption.WRITE ) );

    @Nonnull
    private final Map<Long, ModuleFileSystem> fileSystems = new HashMap<>();

    @Nonnull
    @Override
    public String getScheme()
    {
        return "module";
    }

    @Nonnull
    @Override
    public ModuleFileSystem newFileSystem( @Nonnull URI uri, @Nonnull Map<String, ?> env ) throws IOException
    {
        synchronized( this.fileSystems )
        {
            long moduleId = getModuleId( uri );
            try
            {
                Bundle bundle = Mosaic.getBundleContext().getBundle( moduleId );
                if( bundle == null )
                {
                    throw new IllegalArgumentException( "URI '" + uri + "' specifies module ID '" + moduleId + "' but no such module found" );
                }
                else if( this.fileSystems.containsKey( moduleId ) )
                {
                    throw new FileSystemAlreadyExistsException( "File-system for module '" + moduleId + "' already opened" );
                }
                else
                {
                    ModuleFileSystem fileSystem = new ModuleFileSystem( this, bundle );
                    this.fileSystems.put( moduleId, fileSystem );
                    return fileSystem;
                }
            }
            catch( Exception e )
            {
                throw new IOException( "Could not create module file-system for '" + uri + "'", e );
            }
        }
    }

    @Nonnull
    @Override
    public FileSystem getFileSystem( @Nonnull URI uri )
    {
        synchronized( this.fileSystems )
        {
            long moduleId = getModuleId( uri );
            ModuleFileSystem fileSystem = this.fileSystems.get( moduleId );
            if( fileSystem == null )
            {
                throw new FileSystemNotFoundException( "Could not find open file-system for module '" + moduleId + "'" );
            }
            else
            {
                return fileSystem;
            }
        }
    }

    @Nonnull
    @Override
    public Path getPath( @Nonnull URI uri )
    {
        long moduleId = getModuleId( uri );
        synchronized( this.fileSystems )
        {
            ModuleFileSystem fileSystem = this.fileSystems.get( moduleId );
            if( fileSystem == null )
            {
                URI fsUri = URI.create( "module://" + moduleId );
                try
                {
                    fileSystem = newFileSystem( fsUri, Collections.<String, Object>emptyMap() );
                }
                catch( IOException e )
                {
                    FileSystemNotFoundException ex = new FileSystemNotFoundException( "Could not create file-system for module '" + moduleId + "' using '" + fsUri + "' for path URI '" + uri + "'" );
                    ex.initCause( e );
                    throw ex;
                }
            }
            return fileSystem.getPath( uri.getPath() );
        }
    }

    @Nonnull
    @Override
    public SeekableByteChannel newByteChannel( @Nonnull Path path,
                                               @Nonnull Set<? extends OpenOption> options,
                                               @Nonnull FileAttribute<?>... attrs )
            throws IOException
    {
        // cannot set attributes since we're read-only
        if( attrs.length > 0 )
        {
            throw new UnsupportedOperationException( "Read-only file-system" );
        }

        // cannot append, write, truncate files since we're read-only
        for( OpenOption option : options )
        {
            if( RESTRICTED_OPEN_OPTIONS.contains( option ) )
            {
                throw new UnsupportedOperationException( "Read-only file-system" );
            }
        }

        // find file-system and bundle
        ModuleFileSystem moduleFileSystem = ( ModuleFileSystem ) path.getFileSystem();
        ModulePath modulePath = ( ( ModulePath ) path ).toAbsolutePath();
        Bundle bundle = moduleFileSystem.getBundle();
        if( bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.UNINSTALLED )
        {
            throw new IOException( "Module '" + bundle.getSymbolicName() + "@" + bundle.getVersion() + "[" + bundle.getBundleId() + "] is not resolved" );
        }

        // find entry
        URL entry = bundle.getEntry( modulePath.getPath() );
        if( entry == null )
        {
            entry = bundle.getEntry( modulePath.getPath() + "/" );
            if( entry != null )
            {
                throw new IOException( "path '" + path + "' is a directory in module '" + bundle.getSymbolicName() + "@" + bundle.getVersion() + "[" + bundle.getBundleId() + "]" );
            }
            else
            {
                throw new IOException( "path '" + path + "' does not exist in module '" + bundle.getSymbolicName() + "@" + bundle.getVersion() + "[" + bundle.getBundleId() + "]" );
            }
        }

        // create the channel
        return new EntrySeekableByteChannel( entry );
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream( @Nonnull Path dir,
                                                     @Nonnull final DirectoryStream.Filter<? super Path> filter )
            throws IOException
    {
        // find file-system and bundle
        final ModuleFileSystem moduleFileSystem = ( ModuleFileSystem ) dir.getFileSystem();
        final ModulePath modulePath = ( ( ModulePath ) dir ).toAbsolutePath();
        final Bundle bundle = moduleFileSystem.getBundle();
        if( bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.UNINSTALLED )
        {
            throw new IOException( "Module '" + bundle.getSymbolicName() + "@" + bundle.getVersion() + "[" + bundle.getBundleId() + "] is not resolved" );
        }

        // find entry
        String entryPath = modulePath.getPath();
        URL entry = bundle.getEntry( entryPath.endsWith( "/" ) ? entryPath : entryPath + "/" );
        if( entry == null )
        {
            throw new IOException( "path '" + dir + "' does not exist or is not a directory in module '" + bundle.getSymbolicName() + "@" + bundle.getVersion() + "[" + bundle.getBundleId() + "]" );
        }

        // find all entries in this directory
        final Enumeration<URL> entries = bundle.findEntries( modulePath.getPath(), null, true );
        return new DirectoryStream<Path>()
        {
            @Nullable
            private Iterator<Path> iterator;

            @Override
            public Iterator<Path> iterator()
            {
                if( this.iterator != null )
                {
                    throw new IllegalStateException();
                }

                this.iterator = new Iterator<Path>()
                {
                    @Nullable
                    private ModulePath next;

                    @Override
                    public boolean hasNext()
                    {
                        if( this.next != null )
                        {
                            return true;
                        }
                        while( entries.hasMoreElements() )
                        {
                            URL url = entries.nextElement();
                            ModulePath path = moduleFileSystem.getPath( url.getPath() );
                            try
                            {
                                if( filter.accept( path ) )
                                {
                                    this.next = path;
                                    return true;
                                }
                            }
                            catch( IOException e )
                            {
                                throw new DirectoryIteratorException( e );
                            }
                        }
                        return false;
                    }

                    @Override
                    public Path next()
                    {
                        if( hasNext() )
                        {
                            ModulePath next = this.next;
                            this.next = null;
                            return next;
                        }
                        else
                        {
                            throw new NoSuchElementException();
                        }
                    }

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
                return this.iterator;
            }

            @Override
            public void close() throws IOException
            {
                // no-op
            }
        };
    }

    @Override
    public void createDirectory( @Nonnull Path dir, @Nonnull FileAttribute<?>... attrs ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete( @Nonnull Path path ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copy( @Nonnull Path source, @Nonnull Path target, @Nonnull CopyOption... options ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move( @Nonnull Path source, @Nonnull Path target, @Nonnull CopyOption... options ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSameFile( @Nonnull Path path, @Nonnull Path path2 ) throws IOException
    {
        return path.equals( path2 );
    }

    @Override
    public boolean isHidden( @Nonnull Path path ) throws IOException
    {
        return false;
    }

    @Nonnull
    @Override
    public FileStore getFileStore( @Nonnull Path path ) throws IOException
    {
        ModuleFileSystem moduleFileSystem = ( ModuleFileSystem ) path.getFileSystem();
        return moduleFileSystem.getFileStores().iterator().next();
    }

    @Override
    public void checkAccess( @Nonnull Path path, @Nonnull AccessMode... modes ) throws IOException
    {
        // find file-system and bundle
        ModuleFileSystem moduleFileSystem = ( ModuleFileSystem ) path.getFileSystem();
        ModulePath modulePath = ( ( ModulePath ) path ).toAbsolutePath();
        Bundle bundle = moduleFileSystem.getBundle();
        if( bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.UNINSTALLED )
        {
            throw new IOException( "Module '" + bundle.getSymbolicName() + "@" + bundle.getVersion() + "[" + bundle.getBundleId() + "] is not resolved" );
        }

        // find entry
        URL entry = bundle.getEntry( modulePath.getPath() );
        if( entry == null )
        {
            entry = bundle.getEntry( modulePath.getPath() + "/" );
            if( entry == null )
            {
                throw new IOException( "path '" + path + "' does not exist in module '" + bundle.getSymbolicName() + "@" + bundle.getVersion() + "[" + bundle.getBundleId() + "]" );
            }
        }

        for( AccessMode mode : modes )
        {
            if( mode != AccessMode.READ )
            {
                throw new AccessDeniedException( "Cannot execute or write to '" + path + "'" );
            }
        }
    }

    @Nullable
    @Override
    public <V extends FileAttributeView> V getFileAttributeView( @Nonnull Path path,
                                                                 @Nonnull Class<V> type,
                                                                 @Nonnull LinkOption... options )
    {
        if( type == BasicFileAttributeView.class )
        {
            try
            {
                return type.cast( readAttributes( path, BasicFileAttributes.class ) );
            }
            catch( IOException e )
            {
                LOG.warn( "Could not obtain file attributes for '{}': {}", path, e.getMessage(), e );
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public <A extends BasicFileAttributes> A readAttributes( @Nonnull Path path,
                                                             @Nonnull Class<A> type,
                                                             @Nonnull LinkOption... options )
            throws IOException
    {
        ModuleFileSystem moduleFileSystem = ( ModuleFileSystem ) path.getFileSystem();
        ModulePath modulePath = ( ( ModulePath ) path ).toAbsolutePath();
        final Bundle bundle = moduleFileSystem.getBundle();

        final boolean regularFile, directory;

        URL url = bundle.getEntry( modulePath.getPath() );
        final URL entry;
        if( url == null )
        {
            url = bundle.getEntry( modulePath.getPath() + "/" );
            if( url != null )
            {
                directory = true;
            }
            else
            {
                throw new IOException( "path '" + path + "' could not be found in module '" + bundle.getSymbolicName() + "@" + bundle.getVersion() + "[" + bundle.getBundleId() + "]" );
            }
        }
        else
        {
            directory = modulePath.getPath().endsWith( "/" );
        }
        regularFile = !directory;
        entry = url;

        if( type == BasicFileAttributes.class )
        {
            return type.cast( new BasicFileAttributes()
            {
                @Override
                public FileTime lastModifiedTime()
                {
                    return FileTime.fromMillis( bundle.getLastModified() );
                }

                @Override
                public FileTime lastAccessTime()
                {
                    return FileTime.fromMillis( 0 );
                }

                @Override
                public FileTime creationTime()
                {
                    return FileTime.fromMillis( 0 );
                }

                @Override
                public boolean isRegularFile()
                {
                    return regularFile;
                }

                @Override
                public boolean isDirectory()
                {
                    return directory;
                }

                @Override
                public boolean isSymbolicLink()
                {
                    return false;
                }

                @Override
                public boolean isOther()
                {
                    return false;
                }

                @Override
                public long size()
                {
                    try( InputStream inputStream = entry.openStream() )
                    {
                        return ByteStreams.toByteArray( inputStream ).length;
                    }
                    catch( IOException e )
                    {
                        return -1;
                    }
                }

                @Override
                public Object fileKey()
                {
                    return null;
                }
            } );
        }
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public Map<String, Object> readAttributes( @Nonnull Path path,
                                               @Nonnull String attributes,
                                               @Nonnull LinkOption... options ) throws IOException
    {
        BasicFileAttributes attrs = readAttributes( path, BasicFileAttributes.class );
        Map<String, Object> map = new HashMap<>();
        map.put( "directory", attrs.isDirectory() );
        map.put( "regularFile", attrs.isRegularFile() );
        map.put( "lastModifiedTime", attrs.lastModifiedTime() );
        map.put( "size", attrs.size() );
        return map;
    }

    @Override
    public void setAttribute( @Nonnull Path path,
                              @Nonnull String attribute,
                              @Nonnull Object value,
                              @Nonnull LinkOption... options ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    void closeFileSystem( long moduleId )
    {
        synchronized( this.fileSystems )
        {
            this.fileSystems.remove( moduleId );
        }
    }

    private long getModuleId( @Nonnull URI uri )
    {
        if( !uri.getScheme().equals( "module" ) )
        {
            throw new IllegalArgumentException( "URI '" + uri + "' is not using 'module' scheme" );
        }

        String host = uri.getHost();
        if( host == null )
        {
            throw new IllegalArgumentException( "URI '" + uri + "' has no host" );
        }

        try
        {
            return Integer.parseInt( host );
        }
        catch( NumberFormatException e )
        {
            throw new IllegalArgumentException( "URI '" + uri + "' specifies illegal module ID '" + host + "'" );
        }
    }

    private class EntrySeekableByteChannel implements SeekableByteChannel
    {
        @Nonnull
        private final byte[] bytes;

        private int position;

        private EntrySeekableByteChannel( @Nonnull URL url ) throws IOException
        {
            try( InputStream inputStream = url.openStream() )
            {
                this.bytes = ByteStreams.toByteArray( inputStream );
            }
        }

        @Override
        public int read( ByteBuffer dst ) throws IOException
        {
            if( this.position >= this.bytes.length )
            {
                return -1;
            }

            int remaining = dst.remaining();
            if( remaining <= 0 )
            {
                return 0;
            }

            int available = this.bytes.length - this.position;
            int length = Math.min( available, remaining );
            dst.put( this.bytes, this.position, length );
            this.position += length;
            return length;
        }

        @Override
        public int write( ByteBuffer src ) throws IOException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public long position() throws IOException
        {
            return this.position;
        }

        @Override
        public SeekableByteChannel position( long newPosition ) throws IOException
        {
            Preconditions.checkArgument( newPosition >= 0, "new position must not be negative" );
            this.position = Math.min( ( int ) newPosition, this.bytes.length );
            return this;
        }

        @Override
        public long size() throws IOException
        {
            return this.bytes.length;
        }

        @Override
        public SeekableByteChannel truncate( long size ) throws IOException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isOpen()
        {
            return true;
        }

        @Override
        public void close() throws IOException
        {
            // no-op
        }
    }
}

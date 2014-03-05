package org.mosaic.launcher;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.util.Arrays.asList;

/**
 * @author arik
 */
final class ModulePath implements Path
{
    public static void main( String[] args )
    {
        Path path = Paths.get( "/a/b" );
        System.out.println( "'" + path + "' (" + System.identityHashCode( path ) + ")" );
        System.out.println( path.isAbsolute() );
        System.out.println( path.getNameCount() );
        System.out.println( path.getRoot() );
        System.out.println( path.getParent() );
        System.out.println( "'" + path.getFileName() + "' (" + System.identityHashCode( path.getFileName() ) + ")" );
        System.out.println( path.toUri() );
    }

    @Nonnull
    private final ModuleFileSystem fileSystem;

    @Nonnull
    private final String path;

    @Nonnull
    private final List<String> tokens;

    @Nullable
    private final ModulePath root;

    @Nullable
    private final ModulePath parent;

    @Nullable
    private final ModulePath fileName;

    private final boolean absolute;

    ModulePath( @Nonnull ModuleFileSystem fileSystem, @Nonnull String path )
    {
        this.fileSystem = fileSystem;

        // build path, merging duplicate "/"
        StringBuilder buffer = new StringBuilder( path.length() );
        for( char c : path.toCharArray() )
        {
            if( c != '/' || buffer.length() == 0 || buffer.charAt( buffer.length() - 1 ) != '/' )
            {
                buffer.append( c );
            }
        }

        if( buffer.length() == 0 )
        {
            // empty path has empty string as the path, and has no tokens
            this.path = "";
            this.tokens = asList( "" );
            this.root = null;
            this.absolute = false;
            this.parent = null;
            this.fileName = this;
        }
        else if( buffer.length() == 1 && buffer.charAt( 0 ) == '/' )
        {
            // we're the root component, path is simply "/" and we have no tokens
            this.path = "/";
            this.tokens = Collections.emptyList();
            this.root = this;
            this.absolute = true;
            this.parent = null;
            this.fileName = null;
        }
        else
        {
            // remove trailing "/" if any
            if( buffer.charAt( buffer.length() - 1 ) == '/' )
            {
                buffer.deleteCharAt( buffer.length() - 1 );
            }

            // path as built
            this.path = buffer.toString();
            this.tokens = Splitter.on( '/' ).omitEmptyStrings().splitToList( this.path );
            this.absolute = this.path.startsWith( "/" );

            int lastSlash = this.path.lastIndexOf( '/' );
            if( this.absolute )
            {
                this.root = this.fileSystem.getPath( "/" );
                this.parent = lastSlash == 0 ? this.root : this.fileSystem.getPath( this.path.substring( 0, lastSlash ) );
            }
            else
            {
                this.root = null;
                this.parent = lastSlash < 0 ? null : this.fileSystem.getPath( this.path.substring( 0, lastSlash ) );
            }

            if( this.tokens.size() == 1 )
            {
                this.fileName = this;
            }
            else
            {
                this.fileName = this.fileSystem.getPath( this.tokens.get( this.tokens.size() - 1 ) );
            }
        }
    }

    @Nonnull
    @Override
    public ModuleFileSystem getFileSystem()
    {
        return this.fileSystem;
    }

    @Override
    public boolean isAbsolute()
    {
        return this.absolute;
    }

    @Override
    public Path getRoot()
    {
        return this.root;
    }

    @Override
    public Path getFileName()
    {
        return this.fileName;
    }

    @Override
    public Path getParent()
    {
        return this.parent;
    }

    @Override
    public int getNameCount()
    {
        return this.tokens.size();
    }

    @Override
    public Path getName( int index )
    {
        Preconditions.checkElementIndex( index, this.tokens.size(), "index" );
        return this.fileSystem.getPath( this.tokens.get( index ) );
    }

    @Override
    public ModulePath subpath( int beginIndex, int endIndex )
    {
        return this.fileSystem.getPath( Joiner.on( '/' ).join( this.tokens.subList( beginIndex, endIndex ) ) );
    }

    @Override
    public boolean startsWith( String other )
    {
        return startsWith( getFileSystem().getPath( other ) );
    }

    @Override
    public boolean startsWith( Path other )
    {
        // must be a module path too
        if( !ModulePath.class.isInstance( other ) )
        {
            return false;
        }

        ModulePath that = ( ModulePath ) other;

        // must be of same file-system (ie. same bundle/module)
        if( !this.fileSystem.equals( that.fileSystem ) )
        {
            return false;
        }

        // must both be absolute
        if( !isAbsolute() && that.isAbsolute() )
        {
            return false;
        }

        // check path starts with other path
        return this.path.startsWith( that.path );
    }

    @Override
    public boolean endsWith( String other )
    {
        return endsWith( getFileSystem().getPath( other ) );
    }

    @Override
    public boolean endsWith( Path other )
    {
        // must be a module path too
        if( !ModulePath.class.isInstance( other ) )
        {
            return false;
        }

        ModulePath path = ( ModulePath ) other;
        if( !this.fileSystem.equals( path.fileSystem ) )
        {
            // must be of same file-system (ie. same bundle/module)
            return false;
        }
        else if( other.isAbsolute() )
        {
            // if other is absolute, we must be absolute too
            return isAbsolute() && this.path.startsWith( path.path );
        }
        else
        {
            // if other is not absolute, doesn't matter if we are absolute or not
            return this.path.startsWith( path.path );
        }
    }

    @Override
    public Path normalize()
    {
        List<String> tokens = new ArrayList<>( this.tokens.size() );
        for( String token : this.tokens )
        {
            // skip "." tokens, they are redundant
            if( ".".equals( token ) )
            {
                continue;
            }

            // each ".." preceded
            if( "..".equals( token ) )
            {
                if( !tokens.isEmpty() )
                {
                    tokens.remove( tokens.size() - 1 );
                }
            }
            else
            {
                tokens.add( token );
            }
        }
        return this.fileSystem.getPath( ( isAbsolute() ? "/" : "" ) + Joiner.on( '/' ).join( tokens ) );
    }

    @Override
    public Path resolve( String other )
    {
        return resolve( getFileSystem().getPath( other ) );
    }

    @Override
    public ModulePath resolve( Path other )
    {
        if( other.isAbsolute() )
        {
            return ( ModulePath ) other;
        }
        else if( other.getNameCount() == 1 && other.getName( 0 ).toString().isEmpty() )
        {
            return this;
        }
        return this.fileSystem.getPath( this.path + "/" + other );
    }

    @Override
    public Path resolveSibling( String other )
    {
        return resolveSibling( getFileSystem().getPath( other ) );
    }

    @Override
    public Path resolveSibling( Path other )
    {
        Preconditions.checkNotNull( other, "path must not be null" );
        return this.parent == null ? other : this.parent.resolve( other );
    }

    @Override
    public Path relativize( Path other )
    {
        if( equals( other ) )
        {
            return this.fileSystem.getPath( "" );
        }
        else if( !Objects.equals( getRoot(), other.getRoot() ) )
        {
            throw new IllegalArgumentException( "both this path and given path must have equal root components (or none at all)" );
        }
        else if( isAbsolute() != other.isAbsolute() )
        {
            throw new IllegalArgumentException( "both this path and given path must be absolute, or both must be relative" );
        }
        else if( this.path.isEmpty() )
        {
            // this is the empty path; according to UnixPath, in such a case we should return 'other'
            return other;
        }

        ModulePath that = ( ModulePath ) other;
        int thisCount = getNameCount();
        int thatCount = that.getNameCount();

        // skip matching names
        int n = thisCount > thatCount ? thatCount : thisCount;
        int i = 0;
        while( i < n )
        {
            if( !getName( i ).equals( that.getName( i ) ) )
            {
                break;
            }
            i++;
        }

        // 'i' in this stage is the first name that's *different* between the two paths
        int dotsDotsCountToAdd = thisCount - i;
        if( i < thatCount )
        {
            // remaining name components in other
            ModulePath remainder = that.subpath( i, thatCount );
            if( dotsDotsCountToAdd == 0 )
            {
                return remainder;
            }

            // keep prefixing result with "../" until we reach the common ground
            // so result is a  "../" for each remaining name in base
            // followed by the remaining names in other. If the remainder is
            // the empty path then we don't add the final trailing slash.
            StringBuilder buffer = new StringBuilder( dotsDotsCountToAdd * 3 + remainder.path.length() );
            while( dotsDotsCountToAdd > 0 )
            {
                buffer.append( ".." );
                if( that.isEmpty() )
                {
                    if( dotsDotsCountToAdd > 1 )
                    {
                        buffer.append( '/' );
                    }
                }
                else
                {
                    buffer.append( '/' );
                }
                dotsDotsCountToAdd--;
            }
            buffer.append( remainder.path );
            return this.fileSystem.getPath( buffer.toString() );
        }
        else
        {
            // no remaining names in other so result is simply a sequence of ".."
            StringBuilder buffer = new StringBuilder( dotsDotsCountToAdd * 3 );
            while( dotsDotsCountToAdd > 0 )
            {
                buffer.append( ".." );
                if( dotsDotsCountToAdd > 1 )
                {
                    buffer.append( '/' );
                }
                dotsDotsCountToAdd--;
            }
            return this.fileSystem.getPath( buffer.toString() );
        }
    }

    @Override
    public ModulePath toAbsolutePath()
    {
        if( isAbsolute() )
        {
            return this;
        }
        else
        {
            return getFileSystem().getRoot().resolve( this );
        }
    }

    @Override
    public URI toUri()
    {
        String uri = String.format( "module://%d%s", this.fileSystem.getBundle().getBundleId(), toAbsolutePath() );
        try
        {
            return new URI( uri );
        }
        catch( URISyntaxException x )
        {
            throw new AssertionError( x );  // should not happen
        }
    }

    @Override
    public Path toRealPath( LinkOption... options ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public File toFile()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register( WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers )
            throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register( WatchService watcher, WatchEvent.Kind<?>... events ) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Path> iterator()
    {
        return new Iterator<Path>()
        {
            private int i = 0;

            @Override
            public boolean hasNext()
            {
                return ( i < getNameCount() );
            }

            @Override
            public Path next()
            {
                if( i < getNameCount() )
                {
                    Path result = getName( i );
                    i++;
                    return result;
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
    }

    @Override
    public int compareTo( Path other )
    {
        ModulePath that = ( ModulePath ) other;
        if( !this.fileSystem.equals( that.fileSystem ) )
        {
            // must be of same file-system (ie. same bundle/module)
            throw new IllegalArgumentException( "both paths must be of same file-system and module" );
        }
        else
        {
            return this.path.compareTo( that.path );
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        if( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ModulePath paths = ( ModulePath ) o;

        if( !this.fileSystem.equals( paths.fileSystem ) )
        {
            return false;
        }
        if( this.root != null ? !root.equals( paths.root ) : paths.root != null )
        {
            return false;
        }
        if( !this.tokens.equals( paths.tokens ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = this.fileSystem.hashCode();
        result = 31 * result + this.tokens.hashCode();
        result = 31 * result + ( this.root != null && this.root != this ? this.root.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        return this.path;
    }

    @Nonnull
    String getPath()
    {
        return this.path;
    }

    @Nonnull
    List<String> getTokens()
    {
        return this.tokens;
    }

    private boolean isEmpty()
    {
        return this.tokens.size() == 1 && this.tokens.get( 0 ).isEmpty();
    }
}

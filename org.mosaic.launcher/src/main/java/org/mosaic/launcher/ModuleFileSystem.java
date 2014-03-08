package org.mosaic.launcher;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.osgi.framework.Bundle;

/**
 * @author arik
 */
final class ModuleFileSystem extends FileSystem
{
    private static final Set<String> SUPPORTED_FILE_ATTRIBUTE_VIEWS = Collections.unmodifiableSet( Sets.newHashSet( "basic" ) );

    private static final String GLOB_SYNTAX = "glob";

    private static final String REGEX_SYNTAX = "regex";

    @Nonnull
    private final LoadingCache<String, ModulePath> pathCache;

    @Nonnull
    private final ModuleFileSystemProvider provider;

    @Nonnull
    private final Bundle bundle;

    @Nonnull
    private final ModulePath root;

    @Nonnull
    private final List<Path> rootDirectories;

    @Nonnull
    private final List<FileStore> fileStores;

    private boolean open = true;

    ModuleFileSystem( @Nonnull ModuleFileSystemProvider provider, @Nonnull Bundle bundle )
    {
        this.provider = provider;
        this.bundle = bundle;
        this.pathCache = CacheBuilder.newBuilder()
                                     .build( new CacheLoader<String, ModulePath>()
                                     {
                                         @Nonnull
                                         @Override
                                         public ModulePath load( @Nonnull String key ) throws Exception
                                         {
                                             return new ModulePath( ModuleFileSystem.this, key );
                                         }
                                     } );
        this.root = getPath( "/" );
        this.rootDirectories = Arrays.<Path>asList( this.root );
        this.fileStores = Arrays.<FileStore>asList( new ModuleFileStore( this.bundle ) );
    }

    @Nonnull
    @Override
    public FileSystemProvider provider()
    {
        return this.provider;
    }

    @Override
    public void close() throws IOException
    {
        this.open = false;
        this.provider.closeFileSystem( this.bundle.getBundleId() );
    }

    @Override
    public boolean isOpen()
    {
        return this.open;
    }

    @Override
    public boolean isReadOnly()
    {
        return true;
    }

    @Nonnull
    @Override
    public String getSeparator()
    {
        return "/";
    }

    @Nonnull
    @Override
    public Iterable<Path> getRootDirectories()
    {
        return this.rootDirectories;
    }

    @Nonnull
    @Override
    public Iterable<FileStore> getFileStores()
    {
        return this.fileStores;
    }

    @Nonnull
    @Override
    public Set<String> supportedFileAttributeViews()
    {
        return SUPPORTED_FILE_ATTRIBUTE_VIEWS;
    }

    @Nonnull
    @Override
    public ModulePath getPath( @Nonnull String first, @Nonnull String... more )
    {
        StringBuilder buffer = new StringBuilder( first.length() * 2 );
        if( !first.isEmpty() )
        {
            buffer.append( first );
        }

        for( String s : more )
        {
            if( s != null && !s.isEmpty() )
            {
                if( buffer.length() > 0 )
                {
                    buffer.append( '/' );
                }
                buffer.append( s );
            }
        }
        try
        {
            return this.pathCache.getUnchecked( buffer.toString() );
        }
        catch( UncheckedExecutionException e )
        {
            Throwable cause = e.getCause();
            if( cause instanceof RuntimeException )
            {
                throw ( RuntimeException ) cause;
            }
            else
            {
                throw e;
            }
        }
    }

    @Nonnull
    @Override
    public PathMatcher getPathMatcher( @Nonnull String syntaxAndPattern )
    {
        int pos = syntaxAndPattern.indexOf( ':' );
        if( pos <= 0 || pos == syntaxAndPattern.length() )
        {
            throw new IllegalArgumentException();
        }

        String syntax = syntaxAndPattern.substring( 0, pos );
        String input = syntaxAndPattern.substring( pos + 1 );

        String expr;
        if( syntax.equals( GLOB_SYNTAX ) )
        {
            expr = Globs.toUnixRegexPattern( input );
        }
        else
        {
            if( syntax.equals( REGEX_SYNTAX ) )
            {
                expr = input;
            }
            else
            {
                throw new UnsupportedOperationException( "Syntax '" + syntax +
                                                         "' not recognized" );
            }
        }

        // return matcher
        final Pattern pattern = Pattern.compile( expr );

        return new PathMatcher()
        {
            @Override
            public boolean matches( @Nonnull Path path )
            {
                return pattern.matcher( path.toString() ).matches();
            }
        };
    }

    @Nonnull
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService()
    {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public WatchService newWatchService() throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    ModulePath getRoot()
    {
        return this.root;
    }

    @Nonnull
    Bundle getBundle()
    {
        return this.bundle;
    }
}

package org.mosaic.core.impl;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleRevision;

import static java.nio.file.FileSystems.newFileSystem;
import static java.nio.file.Files.*;

/**
 * @author arik
 */
class Util
{
    @Nonnull
    private static final Pattern REVISION_ID_PATTERN = Pattern.compile( "\\d+\\.(\\d+)" );

    @Nonnull
    static Path getPath( @Nonnull BundleContext bundleContext, @Nonnull String key )
    {
        String location = bundleContext.getProperty( key );
        if( location == null )
        {
            throw new IllegalStateException( "could not discover Mosaic directory location for '" + key + "'" );
        }
        else
        {
            return Paths.get( location );
        }
    }

    @Nonnull
    static Path findRoot( @Nonnull Path path )
    {
        if( isSymbolicLink( path ) )
        {
            try
            {
                Path linkTarget = readSymbolicLink( path );
                Path resolvedTarget = path.resolveSibling( linkTarget );
                Path target = resolvedTarget.toAbsolutePath().normalize();
                return findRoot( target );
            }
            catch( Exception e )
            {
                throw new IllegalStateException( "could not inspect " + path, e );
            }
        }
        else if( isDirectory( path ) )
        {
            return path;
        }
        else if( path.getFileName().toString().toLowerCase().endsWith( ".jar" ) )
        {
            try
            {
                URI uri = URI.create( "jar:file:" + path.toUri().getPath() );
                return newFileSystem( uri, Collections.<String, Object>emptyMap() ).getRootDirectories().iterator().next();
            }
            catch( Exception e )
            {
                throw new IllegalStateException( "could not inspect " + path, e );
            }
        }
        else
        {
            throw new IllegalStateException( "could not inspect " + path );
        }
    }

    @SuppressWarnings( "unchecked" )
    @Nonnull
    static Map<String, String> getHeaders( @Nonnull BundleRevision bundleRevision )
    {
        try
        {
            Method getHeadersMethod = bundleRevision.getClass().getMethod( "getHeaders" );
            getHeadersMethod.setAccessible( true );
            Object headersMap = getHeadersMethod.invoke( bundleRevision );
            if( headersMap instanceof Map )
            {
                return Collections.unmodifiableMap( ( Map ) headersMap );
            }
        }
        catch( Exception e )
        {
            throw new IllegalStateException( "could not extract headers from bundle revision", e );
        }
        throw new IllegalStateException( "could not extract headers from bundle revision" );
    }

    static long getRevisionNumber( @Nonnull BundleRevision bundleRevision )
    {
        try
        {
            return Long.parseLong( bundleRevision.toString() );
        }
        catch( NumberFormatException e )
        {
            Matcher matcher = REVISION_ID_PATTERN.matcher( bundleRevision.toString() );
            if( matcher.matches() )
            {
                return Long.parseLong( matcher.group( 1 ) );
            }
            else
            {
                throw new IllegalStateException( "could not extract bundle revision number from: " + bundleRevision.toString() );
            }
        }
    }

    @Nonnull
    static String getClassName( Path path )
    {
        String className = path.toString().replace( '/', '.' );
        if( className.startsWith( "." ) )
        {
            className = className.substring( 1 );
        }
        if( className.toLowerCase().endsWith( ".class" ) )
        {
            className = className.substring( 0, className.length() - ".class".length() );
        }
        return className;
    }

    static class FilterBuilder
    {
        @Nonnull
        public static Filter create( @Nonnull Class<?> type, @Nullable String additionalFilter )
        {
            FilterBuilder filterBuilder = new FilterBuilder();
            filterBuilder.addClass( type );
            filterBuilder.add( additionalFilter );
            try
            {
                return FrameworkUtil.createFilter( filterBuilder.toFilterString() );
            }
            catch( InvalidSyntaxException e )
            {
                throw new IllegalArgumentException( "could not compose filter of class '" + type.getName() + "' and additional filter '" + additionalFilter + "': " + e.getMessage(), e );
            }
        }

        @Nonnull
        private final List<String> filters = new LinkedList<>();

        @Nonnull
        public String toFilterString()
        {
            if( this.filters.isEmpty() )
            {
                return "";
            }
            else if( this.filters.size() == 1 )
            {
                return this.filters.get( 0 );
            }
            else
            {
                StringBuilder buf = new StringBuilder( 200 );
                for( String filter : this.filters )
                {
                    buf.append( filter );
                }
                return "(&" + buf + ")";
            }
        }

        FilterBuilder addClass( @Nullable Class<?> clazz )
        {
            if( clazz != null )
            {
                addClass( clazz.getName() );
            }
            return this;
        }

        FilterBuilder addClass( @Nullable String className )
        {
            if( className != null )
            {
                className = className.trim();
                if( !className.isEmpty() )
                {
                    addEquals( Constants.OBJECTCLASS, className );
                }
            }
            return this;
        }

        FilterBuilder addEquals( @Nullable String key, @Nullable String value )
        {
            if( key != null )
            {
                key = key.trim();
                if( !key.isEmpty() )
                {
                    if( value == null )
                    {
                        value = "null";
                    }
                    value = value.trim();
                    add( key + "=" + value );
                }
            }
            return this;
        }

        FilterBuilder add( @Nullable String filter )
        {
            if( filter != null )
            {
                filter = filter.trim();
                if( !filter.isEmpty() )
                {
                    if( !filter.startsWith( "(" ) )
                    {
                        filter = "(" + filter + ")";
                    }
                    this.filters.add( filter );
                }
            }
            return this;
        }

        @Nonnull
        Filter toFilter()
        {
            String filter = toFilterString();
            try
            {
                return FrameworkUtil.createFilter( filter );
            }
            catch( InvalidSyntaxException e )
            {
                throw new IllegalArgumentException( "illegal filter built: " + filter, e );
            }
        }
    }

    private Util()
    {
    }
}

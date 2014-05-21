package org.mosaic.core.impl;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mosaic.core.util.Nonnull;
import org.osgi.framework.BundleContext;
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

    @SuppressWarnings("unchecked")
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

    private Util()
    {
    }
}

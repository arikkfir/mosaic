package org.mosaic.web.server.impl.marshall;

import com.google.common.collect.Iterables;
import com.google.common.net.MediaType;
import freemarker.template.TemplateException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.util.B64Code;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.mosaic.modules.Component;
import org.mosaic.modules.Ranking;
import org.mosaic.modules.Service;
import org.mosaic.security.Security;
import org.mosaic.web.application.Application;
import org.mosaic.web.server.HandlerResultMarshaller;
import org.mosaic.web.server.HttpStatus;
import org.mosaic.web.server.WebInvocation;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.net.MediaType.*;
import static java.nio.file.Files.*;
import static java.util.Locale.ENGLISH;
import static org.mosaic.web.server.HttpStatus.*;

/**
 * @author arik
 */
@Service
@Ranking( -100 )
final class ResourceResultMarshaller implements HandlerResultMarshaller
{
    @Nonnull
    private final MimeTypes mimeTypes = new MimeTypes();

    @Nonnull
    @Component
    private FreemarkerRenderer freemarkerRenderer;

    @Nonnull
    @Service
    private Security security;

    ResourceResultMarshaller()
    {
        this.mimeTypes.addMimeMapping( "map", "application/javascript" );
    }

    @Override
    public boolean canMarshall( @Nonnull MediaType mediaType, @Nonnull Object value )
    {
        if( value instanceof Application.ApplicationResource )
        {
            Application.ApplicationResource resource = ( Application.ApplicationResource ) value;
            value = resource.getPath();
        }

        if( value instanceof Path )
        {
            Path path = ( Path ) value;
            if( Files.isRegularFile( path ) )
            {
                if( path.toString().endsWith( ".ftl" ) )
                {
                    return HTML_UTF_8.is( mediaType ) || XHTML_UTF_8.is( mediaType );
                }
                else
                {
                    MediaType fileContentType = findContentType( path.getFileName().toString() );
                    if( fileContentType == null )
                    {
                        fileContentType = PLAIN_TEXT_UTF_8;
                    }
                    return fileContentType.is( mediaType );
                }
            }
            else if( Files.isDirectory( path ) )
            {
                return PLAIN_TEXT_UTF_8.is( mediaType ) || HTML_UTF_8.is( mediaType ) || XHTML_UTF_8.is( mediaType );
            }
        }

        return false;
    }

    @Override
    public void marshall( @Nonnull WebInvocation invocation, @Nonnull Object value ) throws Exception
    {
        // TODO: support gzip

        Period cachePeriod;
        boolean browsingEnabled;
        Path path;

        if( value instanceof Application.ApplicationResource )
        {
            Application.ApplicationResource resource = ( Application.ApplicationResource ) value;
            cachePeriod = resource.getCachePeriod();
            browsingEnabled = resource.isBrowsingEnabled();
            path = resource.getPath();
        }
        else if( value instanceof Path )
        {
            cachePeriod = null;
            browsingEnabled = false;
            path = ( Path ) value;
        }
        else
        {
            invocation.getHttpResponse().setStatus( HttpStatus.NOT_IMPLEMENTED, "" );
            invocation.disableCaching();
            return;
        }

        if( isDirectory( path ) )
        {
            if( browsingEnabled )
            {
                serveDirectory( invocation, path );
            }
            else
            {
                invocation.getHttpResponse().setStatus( FORBIDDEN, "" );
                invocation.disableCaching();
            }
        }
        else if( path.toString().endsWith( ".ftl" ) )
        {
            serveDynamicFile( invocation, path );
        }
        else
        {
            serveFile( invocation, path, cachePeriod );
        }
    }

    private void serveDirectory( @Nonnull WebInvocation invocation, @Nonnull Path path )
            throws IOException, TemplateException
    {
        List<Path> children = new LinkedList<>();
        try( DirectoryStream<Path> stream = Files.newDirectoryStream( path ) )
        {
            Iterables.addAll( children, stream );
        }

        invocation.disableCaching();
        invocation.getHttpResponse().setLastModified( DateTime.now() );
        invocation.getHttpResponse().setContentType( HTML_UTF_8 );

        Map<String, Object> context = new HashMap<>();
        context.put( "request", invocation );
        context.put( "subject", this.security.getSubject() );
        context.put( "user", this.security.getSubject() );
        try
        {
            this.freemarkerRenderer.render( invocation.getApplication(),
                                            context,
                                            "app:/directory-listing.html.ftl",
                                            firstNonNull( invocation.getHttpRequest().getContentLanguage(), ENGLISH ),
                                            invocation.getHttpResponse().getWriter() );
        }
        catch( FileNotFoundException e )
        {
            invocation.getHttpResponse().setStatus( FORBIDDEN, "" );
            invocation.disableCaching();
        }
    }

    private void serveDynamicFile( @Nonnull WebInvocation invocation, @Nonnull Path path )
            throws IOException, TemplateException
    {
        String filename = path.getFileName().toString();
        filename = filename.substring( 0, filename.lastIndexOf( ".ftl" ) );

        invocation.disableCaching();
        invocation.getHttpResponse().setLastModified( DateTime.now() );
        invocation.getHttpResponse().setContentType( findContentType( filename ) );

        Map<String, Object> context = new HashMap<>();
        context.put( "request", invocation );
        context.put( "subject", this.security.getSubject() );
        context.put( "user", this.security.getSubject() );
        this.freemarkerRenderer.render( invocation.getApplication(),
                                        context,
                                        path,
                                        firstNonNull( invocation.getHttpRequest().getContentLanguage(), ENGLISH ),
                                        invocation.getHttpResponse().getWriter() );
    }

    private void serveFile( @Nonnull WebInvocation invocation, @Nonnull Path path, @Nullable Period cachePeriod )
            throws IOException
    {
        // caching
        if( cachePeriod != null )
        {
            invocation.getHttpResponse().setCacheControl( "must-revalidate, private, max-age=" + cachePeriod.toStandardSeconds().getSeconds() );
            invocation.getHttpResponse().setExpires( DateTime.now().withPeriodAdded( cachePeriod, 1 ) );
        }
        else
        {
            invocation.disableCaching();
        }

        // last modified
        DateTime lastModified = findLastModified( path );
        invocation.getHttpResponse().setLastModified( findLastModified( path ) );

        // content length & type
        invocation.getHttpResponse().setContentType( findContentType( path.getFileName().toString() ) );
        Long contentLength;
        try
        {
            contentLength = Files.size( path );
            invocation.getHttpResponse().setContentLength( contentLength );
        }
        catch( IOException e )
        {
            contentLength = null;
        }

        // etag
        String etag = findETag( path, lastModified, contentLength );
        invocation.getHttpResponse().setETag( etag );

        Collection<String> ifMatch = invocation.getHttpRequest().getIfMatch();
        if( !ifMatch.isEmpty() )
        {
            if( !ifMatch.contains( "*" ) && !ifMatch.contains( etag ) )
            {
                invocation.getHttpResponse().setStatus( PRECONDITION_FAILED, "" );
                return;
            }
        }

        Collection<String> ifNoneMatch = invocation.getHttpRequest().getIfNoneMatch();
        if( !ifNoneMatch.isEmpty() && etag != null )
        {
            if( ifNoneMatch.contains( "*" ) || ifNoneMatch.contains( etag ) )
            {
                invocation.getHttpResponse().setStatus( NOT_MODIFIED, "Unmodified" );
                return;
            }
        }

        DateTime ifModifiedSince = invocation.getHttpRequest().getIfModifiedSince();
        if( ifModifiedSince != null && lastModified != null && ( ifModifiedSince.equals( lastModified ) || ifModifiedSince.isAfter( lastModified ) ) )
        {
            // TODO: according to RFC-2616 14.26, "If-Modified-Since" can override "If-None-Match", we need to support that
            invocation.getHttpResponse().setStatus( NOT_MODIFIED, "Unmodified" );
            return;
        }

        DateTime ifUnmodifiedSince = invocation.getHttpRequest().getIfUnmodifiedSince();
        if( ifUnmodifiedSince != null && lastModified != null && ( ifUnmodifiedSince.equals( lastModified ) || ifUnmodifiedSince.isBefore( lastModified ) ) )
        {
            invocation.getHttpResponse().setStatus( PRECONDITION_FAILED, "" );
            return;
        }
        copy( path, invocation.getHttpResponse().getOutputStream() );
    }

    @Nullable
    private MediaType findContentType( @Nonnull String fileName )
    {
        String mimeType = this.mimeTypes.getMimeByExtension( fileName );
        return mimeType != null ? MediaType.parse( mimeType ) : null;
    }

    @Nullable
    private DateTime findLastModified( @Nonnull Path file )
    {
        try
        {
            return new DateTime( getLastModifiedTime( file ).toMillis() ).withMillisOfSecond( 0 );
        }
        catch( IOException ignore )
        {
            return null;
        }
    }

    @Nullable
    private String findETag( @Nonnull Path file, @Nullable DateTime lastModified, @Nullable Long contentLength )
    {
        try
        {
            StringBuilder b = new StringBuilder( 32 );
            b.append( "W/\"" );

            String name = file.getFileName().toString();
            int length = name.length();
            long lhash = 0;
            for( int i = 0; i < length; i++ )
            {
                lhash = 31 * lhash + name.charAt( i );
            }

            B64Code.encode( ( lastModified == null ? -1 : lastModified.getMillis() ) ^ lhash, b );
            B64Code.encode( ( contentLength == null ? -1 : contentLength ) ^ lhash, b );
            b.append( '"' );

            return b.toString();
        }
        catch( Exception ignore )
        {
            return null;
        }
    }
}

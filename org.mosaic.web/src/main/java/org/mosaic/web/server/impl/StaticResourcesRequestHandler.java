package org.mosaic.web.server.impl;

import com.google.common.net.MediaType;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
import org.mosaic.web.server.RequestHandler;
import org.mosaic.web.server.WebInvocation;

import static com.google.common.base.Objects.firstNonNull;
import static java.nio.file.Files.*;
import static java.util.Locale.ENGLISH;
import static org.mosaic.web.application.Application.ApplicationResource;
import static org.mosaic.web.server.HttpStatus.*;

/**
 * @author arik
 */
@Service(properties = @Service.P(key = "methods", value = "get,head"))
@Ranking(-1)
final class StaticResourcesRequestHandler implements RequestHandler
{
    @Nonnull
    private final MimeTypes mimeTypes = new MimeTypes();

    @Nonnull
    @Component
    private FreemarkerRenderer freemarkerRenderer;

    @Nonnull
    @Service
    private Security security;

    @Override
    public boolean canHandle( @Nonnull WebInvocation request )
    {
        String path = request.getHttpRequest().getUri().getDecodedPath();

        ApplicationResource resource = request.getApplication().getResource( path + ".ftl" );
        if( resource == null )
        {
            resource = request.getApplication().getResource( path );
        }

        if( resource != null )
        {
            request.getAttributes().put( "resource", resource );
            return true;
        }
        else
        {
            return false;
        }
    }

    @Nullable
    @Override
    public Object handle( @Nonnull WebInvocation invocation ) throws Throwable
    {
        // TODO: support gzip

        ApplicationResource resource = invocation.getAttributes().find( "resource", ApplicationResource.class ).get();
        if( isDirectory( resource.getPath() ) )
        {
            serveDirectory( invocation, resource );
        }
        else if( resource.getPath().toString().endsWith( ".ftl" ) )
        {
            serveDynamicFile( invocation, resource );
        }
        else
        {
            serveFile( invocation, resource );
        }
        return null;
    }

    private void serveDirectory( @Nonnull WebInvocation request, @Nonnull Application.ApplicationResource resource )
    {
        // TODO: handle directory listing (if enabled)
        request.getHttpResponse().setStatus( NOT_IMPLEMENTED, "Not implemented" );
        request.disableCaching();
    }

    private void serveDynamicFile( @Nonnull WebInvocation request, @Nonnull Application.ApplicationResource resource )
            throws IOException, TemplateException
    {
        Path file = resource.getPath();
        String filename = file.getFileName().toString();
        filename = filename.substring( 0, filename.lastIndexOf( ".ftl" ) );

        request.disableCaching();

        request.getHttpResponse().setLastModified( DateTime.now() );

        request.getHttpResponse().setContentType( findContentType( filename ) );

        if( request.getHttpRequest().getMethod().equalsIgnoreCase( "GET" ) )
        {
            Map<String, Object> context = new HashMap<>();
            context.put( "request", request );
            context.put( "subject", this.security.getSubject() );
            context.put( "user", this.security.getSubject() );
            this.freemarkerRenderer.render( request.getApplication(),
                                            context,
                                            request.getHttpRequest().getUri().getDecodedPath(),
                                            firstNonNull( request.getHttpRequest().getContentLanguage(), ENGLISH ),
                                            request.getHttpResponse().getWriter() );
        }
        else if( !request.getHttpRequest().getMethod().equalsIgnoreCase( "HEAD" ) )
        {
            request.getHttpResponse().setStatus( METHOD_NOT_ALLOWED, "Method not allowed" );
            request.getHttpResponse().setAllow( Arrays.asList( "GET", "HEAD" ) );
        }
    }

    private void serveFile( @Nonnull WebInvocation request, @Nonnull ApplicationResource resource )
            throws IOException
    {
        Path file = resource.getPath();

        // caching
        Period cachePeriod = resource.getCachePeriod();
        if( cachePeriod != null )
        {
            request.getHttpResponse().setCacheControl( "must-revalidate, private, max-age=" + cachePeriod.toStandardSeconds().getSeconds() );
            request.getHttpResponse().setExpires( DateTime.now().withPeriodAdded( cachePeriod, 1 ) );
        }
        else
        {
            request.disableCaching();
        }

        // last modified
        DateTime lastModified = findLastModified( file );
        request.getHttpResponse().setLastModified( findLastModified( file ) );

        // content length & type
        request.getHttpResponse().setContentType( findContentType( file.getFileName().toString() ) );
        Long contentLength;
        try
        {
            contentLength = Files.size( file );
            request.getHttpResponse().setContentLength( contentLength );
        }
        catch( IOException e )
        {
            contentLength = null;
        }

        // etag
        String etag = findETag( file, lastModified, contentLength );
        request.getHttpResponse().setETag( etag );

        if( request.getHttpRequest().getMethod().equalsIgnoreCase( "GET" ) )
        {
            Collection<String> ifMatch = request.getHttpRequest().getIfMatch();
            if( !ifMatch.isEmpty() )
            {
                if( !ifMatch.contains( "*" ) && !ifMatch.contains( etag ) )
                {
                    request.getHttpResponse().setStatus( PRECONDITION_FAILED, "" );
                    return;
                }
            }

            Collection<String> ifNoneMatch = request.getHttpRequest().getIfNoneMatch();
            if( !ifNoneMatch.isEmpty() && etag != null )
            {
                if( ifNoneMatch.contains( "*" ) || ifNoneMatch.contains( etag ) )
                {
                    request.getHttpResponse().setStatus( NOT_MODIFIED, "Unmodified" );
                    return;
                }
            }

            DateTime ifModifiedSince = request.getHttpRequest().getIfModifiedSince();
            if( ifModifiedSince != null && lastModified != null && ( ifModifiedSince.equals( lastModified ) || ifModifiedSince.isAfter( lastModified ) ) )
            {
                // TODO: according to RFC-2616 14.26, "If-Modified-Since" can override "If-None-Match", we need to support that
                request.getHttpResponse().setStatus( NOT_MODIFIED, "Unmodified" );
                return;
            }

            DateTime ifUnmodifiedSince = request.getHttpRequest().getIfUnmodifiedSince();
            if( ifUnmodifiedSince != null && lastModified != null && ( ifUnmodifiedSince.equals( lastModified ) || ifUnmodifiedSince.isBefore( lastModified ) ) )
            {
                request.getHttpResponse().setStatus( PRECONDITION_FAILED, "" );
                return;
            }
            copy( file, request.getHttpResponse().getOutputStream() );
        }
        else if( !request.getHttpRequest().getMethod().equalsIgnoreCase( "HEAD" ) )
        {
            request.getHttpResponse().setStatus( METHOD_NOT_ALLOWED, "Method not allowed" );
            request.getHttpResponse().setAllow( Arrays.asList( "GET", "HEAD" ) );
        }
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

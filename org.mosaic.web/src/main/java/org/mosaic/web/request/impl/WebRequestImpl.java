package org.mosaic.web.request.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import org.eclipse.jetty.server.Request;
import org.mosaic.modules.Service;
import org.mosaic.security.Security;
import org.mosaic.util.collections.ConcurrentHashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.web.application.Application;
import org.mosaic.web.request.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Strings.padStart;
import static java.util.Collections.list;

/**
 * @author arik
 */
final class WebRequestImpl implements WebRequest
{
    private static final Logger LOG = LoggerFactory.getLogger( WebRequestImpl.class );

    @Nonnull
    private final Application application;

    @Nonnull
    private final Request request;

    @Nonnull
    private final InetAddress clientAddress;

    @Nonnull
    private final String httpMethod;

    @Nonnull
    private final WebRequestUri uri;

    @Nonnull
    private final WebDevice device;

    @Nonnull
    private final WebRequestHeaders headers;

    @Nonnull
    private final MapEx<String, Object> attributes = new ConcurrentHashMapEx<>( 20 );

    @Nonnull
    private final WebResponse response;

    @Nonnull
    @Service
    private Security security;

    WebRequestImpl( @Nonnull Application application, @Nonnull Request request )
    {
        this.application = application;
        this.request = request;
        this.httpMethod = this.request.getMethod().toUpperCase();

        String remoteAddr = this.request.getRemoteAddr();
        try
        {
            this.clientAddress = InetAddress.getByName( remoteAddr );
        }
        catch( UnknownHostException e )
        {
            throw new IllegalStateException( "could not get IP address for '" + remoteAddr + "': " + e.getMessage(), e );
        }

        this.uri = new WebRequestUriImpl( this.request );
        this.device = new WebDeviceImpl();
        this.headers = new WebRequestHeadersImpl( this.request );
        this.response = new WebResponseImpl( this.request );

        // TODO: authenticate

    }

    @Nonnull
    @Override
    public Application getApplication()
    {
        return this.application;
    }

    @Nonnull
    @Override
    public InetAddress getClientAddress()
    {
        return this.clientAddress;
    }

    @Nonnull
    @Override
    public String getProtocol()
    {
        return this.request.getProtocol();
    }

    @Nonnull
    @Override
    public String getMethod()
    {
        return this.httpMethod;
    }

    @Nonnull
    @Override
    public WebRequestUri getUri()
    {
        return this.uri;
    }

    @Nonnull
    @Override
    public WebDevice getClientDevice()
    {
        return this.device;
    }

    @Nonnull
    @Override
    public WebRequestHeaders getHeaders()
    {
        return this.headers;
    }

    @Nullable
    @Override
    public WebRequestCookie getCookie( String name )
    {
        Cookie[] cookies = this.request.getCookies();
        if( cookies != null )
        {
            for( Cookie cookie : cookies )
            {
                if( cookie.getName().equals( name ) )
                {
                    return new WebRequestCookieImpl( cookie );
                }
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public MapEx<String, Object> getAttributes()
    {
        return this.attributes;
    }

    @Nullable
    @Override
    public WebSession getSession()
    {
        HttpSession httpSession = this.request.getSession( false );
        if( httpSession == null )
        {
            return null;
        }

        Object webSession = httpSession.getAttribute( WebSessionImpl.WEB_SESSION_ATTR_KEY );
        if( webSession == null )
        {
            return null;
        }
        else if( webSession instanceof WebSession )
        {
            return ( WebSession ) webSession;
        }
        else
        {
            dumpToWarnLog( LOG, "Mosaic session attribute contained a value that does not implement {}: {}", WebSession.class.getName(), webSession );
            return null;
        }
    }

    @Nonnull
    @Override
    public WebSession getOrCreateSession()
    {
        HttpSession httpSession = this.request.getSession( true );

        Object webSession = httpSession.getAttribute( WebSessionImpl.WEB_SESSION_ATTR_KEY );
        if( webSession == null )
        {
            return new WebSessionImpl( httpSession );
        }
        else if( webSession instanceof WebSession )
        {
            return ( WebSession ) webSession;
        }
        else
        {
            throw new CreateSessionException( "Mosaic session attribute contained a value that does not implement WebSession: " + webSession );
        }
    }

    @Nonnull
    @Override
    public WebResponse getResponse()
    {
        return this.response;
    }

    @Override
    public void dumpToTraceLog( @Nonnull Logger logger, @Nullable String message, @Nullable Object... arguments )
    {
        logger.trace( message + "\n" + getDebugString(), arguments );
    }

    @Override
    public void dumpToDebugLog( @Nonnull Logger logger, @Nullable String message, @Nullable Object... arguments )
    {
        logger.debug( message + "\n" + getDebugString(), arguments );
    }

    @Override
    public void dumpToInfoLog( @Nonnull Logger logger, @Nullable String message, @Nullable Object... arguments )
    {
        logger.info( message + "\n" + getDebugString(), arguments );
    }

    @Override
    public void dumpToWarnLog( @Nonnull Logger logger, @Nullable String message, @Nullable Object... arguments )
    {
        logger.warn( message + "\n" + getDebugString(), arguments );
    }

    @Override
    public void dumpToErrorLog( @Nonnull Logger logger, @Nullable String message, @Nullable Object... arguments )
    {
        logger.error( message + "\n" + getDebugString(), arguments );
    }

    @Nonnull
    private String getDebugString()
    {
        StringBuilder buffer = new StringBuilder( 5000 );
        buffer.append( "\n" );
        buffer.append( "GENERAL INFORMATION\n" );
        buffer.append( "                      Method: " ).append( getMethod() ).append( "\n" );
        buffer.append( "                   Jetty URL: " ).append( this.request.getUri() ).append( "\n" );
        buffer.append( "                      Scheme: " ).append( this.uri.getScheme() ).append( "\n" );
        buffer.append( "                        Host: " ).append( this.uri.getHost() ).append( "\n" );
        buffer.append( "                        Port: " ).append( this.uri.getPort() ).append( "\n" );
        buffer.append( "                Decoded path: " ).append( this.uri.getDecodedPath() ).append( "\n" );
        buffer.append( "                Encoded path: " ).append( this.uri.getEncodedPath() ).append( "\n" );
        buffer.append( "               Encoded query: " ).append( this.uri.getEncodedQueryString() ).append( "\n" );
        buffer.append( "        Decoded query params: " ).append( this.uri.getDecodedQueryParameters().isEmpty() ? "" : this.uri.getDecodedQueryParameters() ).append( "\n" );
        buffer.append( "                    Fragment: " ).append( this.uri.getFragment() ).append( "\n" );
        buffer.append( "\n" );
        buffer.append( "CLIENT INFORMATION\n" );
        buffer.append( "        Client address: " ).append( getClientAddress() ).append( "\n" );
        buffer.append( "               Session: " ).append( getSession() ).append( "\n" );
        buffer.append( "                Device: " ).append( getClientDevice() ).append( "\n" );
        buffer.append( "\n" );
        buffer.append( "REQUEST HEADERS\n" );

        for( String headerName : list( this.request.getHeaderNames() ) )
        {
            ArrayList<String> values = list( this.request.getHeaders( headerName ) );

            headerName = padStart( headerName, 20, ' ' );
            buffer.append( "        " ).append( padStart( headerName, 20, ' ' ) ).append( ": " );

            if( values.isEmpty() )
            {
                buffer.append( "\n" );
            }
            else
            {
                boolean first = true;
                for( String value : values )
                {
                    if( first )
                    {
                        first = false;
                    }
                    else
                    {
                        buffer.append( ", " );
                    }
                    buffer.append( value ).append( "\n" );
                }
            }
        }
        return buffer.toString().replace( "{}", "\\{}" );
    }
}

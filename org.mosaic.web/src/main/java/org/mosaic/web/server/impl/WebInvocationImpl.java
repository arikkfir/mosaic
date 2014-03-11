package org.mosaic.web.server.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import org.eclipse.jetty.server.Request;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.mosaic.util.collections.ConcurrentHashMapEx;
import org.mosaic.util.collections.MapEx;
import org.mosaic.web.application.Application;
import org.mosaic.web.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public final class WebInvocationImpl implements WebInvocation
{
    private static final Logger LOG = LoggerFactory.getLogger( WebInvocationImpl.class );

    private static final String WEB_SESSION_ATTR_KEY = WebSession.class.getName() + "#" + WebSession.class.hashCode();

    @Nonnull
    private final JettyHttpRequestImpl request;

    @Nonnull
    private final JettyHttpResponseImpl response;

    @Nonnull
    private final HttpLogger httpLogger;

    @Nonnull
    private final Application application;

    @Nonnull
    private final UserAgent userAgent;

    @Nonnull
    private final MapEx<String, Object> attributes = new ConcurrentHashMapEx<>( 20 );

    @Nullable
    private final SecurityConstraint securityConstraint;

    public WebInvocationImpl( @Nonnull Request request, @Nonnull Application application )
    {
        this.request = new JettyHttpRequestImpl( request );
        this.response = new JettyHttpResponseImpl( request.getResponse() );
        this.httpLogger = new HttpLogger( LOG, this.request );
        this.application = application;
        this.userAgent = new UserAgentImpl();
        this.securityConstraint = this.application.getConstraintForPath( this.request.getUri().getDecodedPath() );
    }

    @Nonnull
    @Override
    public HttpRequest getHttpRequest()
    {
        return this.request;
    }

    @Nonnull
    @Override
    public HttpResponse getHttpResponse()
    {
        return this.response;
    }

    @Nonnull
    @Override
    public HttpLogger getHttpLogger()
    {
        return this.httpLogger;
    }

    @Nonnull
    @Override
    public Application getApplication()
    {
        return this.application;
    }

    @Nonnull
    @Override
    public UserAgent getUserAgent()
    {
        return this.userAgent;
    }

    @Nonnull
    @Override
    public MapEx<String, Object> getAttributes()
    {
        return this.attributes;
    }

    @Nullable
    @Override
    public SecurityConstraint getSecurityConstraint()
    {
        return this.securityConstraint;
    }

    @Nullable
    @Override
    public WebSession getSession()
    {
        HttpSession httpSession = this.request.getJettyRequest().getSession( false );
        if( httpSession == null )
        {
            return null;
        }

        Object webSession = httpSession.getAttribute( WEB_SESSION_ATTR_KEY );
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
            this.httpLogger.warn( "Mosaic session attribute contained a value that does not implement {}: {}", WebSession.class.getName(), webSession );
            return null;
        }
    }

    @Nonnull
    @Override
    public WebSession getOrCreateSession()
    {
        HttpSession httpSession = this.request.getJettyRequest().getSession( true );

        Object webSession = httpSession.getAttribute( WEB_SESSION_ATTR_KEY );
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
            throw new CreateWebSessionException( "Mosaic session attribute contained a value that does not implement WebSession: " + webSession );
        }
    }

    @Override
    public void permanentRedirect( @Nonnull String location ) throws IOException
    {
        this.response.setLocation( location );
        this.response.setStatus( HttpStatus.MOVED_PERMANENTLY, "" );
    }

    @Override
    public void temporaryRedirect( @Nonnull String location ) throws IOException
    {
        this.response.setLocation( location );
        this.response.setStatus( HttpStatus.MOVED_TEMPORARILY, "" );
    }

    @Nullable
    @Override
    public WebCookie getCookie( String name )
    {
        Cookie[] cookies = this.request.getJettyRequest().getCookies();
        if( cookies != null )
        {
            for( Cookie cookie : cookies )
            {
                if( cookie.getName().equals( name ) )
                {
                    return new WebCookieImpl( cookie );
                }
            }
        }
        return null;
    }

    @Override
    public void addCookie( @Nonnull String name, @Nonnull Object value )
    {
        this.response.getJettyResponse().getHttpFields().addSetCookie( name, value.toString(), null, null, -1, null, false, false, 1 );
    }

    @Override
    public void addCookie( @Nonnull String name, @Nonnull Object value, @Nonnull String domain )
    {
        this.response.getJettyResponse().getHttpFields().addSetCookie( name, value.toString(), domain, null, -1, null, false, false, 1 );
    }

    @Override
    public void addCookie( @Nonnull String name, @Nonnull Object value, @Nonnull String domain, @Nonnull String path )
    {
        this.response.getJettyResponse().getHttpFields().addSetCookie( name, value.toString(), domain, path, -1, null, false, false, 1 );
    }

    @Override
    public void addCookie( @Nonnull String name,
                           @Nonnull Object value,
                           @Nonnull String domain,
                           @Nonnull String path,
                           @Nonnull Period maxAge )
    {
        int seconds = maxAge.toStandardSeconds().getSeconds();
        this.response.getJettyResponse().getHttpFields().addSetCookie( name, value.toString(), domain, path, seconds, null, false, false, 1 );
    }

    @Override
    public void removeCookie( @Nonnull String name )
    {
        this.response.getJettyResponse().getHttpFields().addSetCookie( name, null, null, null, 0, null, false, false, 1 );
    }

    @Override
    public void disableCaching()
    {
        this.response.setCacheControl( "no-cache, no-store, must-revalidate" );
        this.response.setExpires( new DateTime() );
        this.response.setPragma( "no-cache" );
    }

    private class UserAgentImpl implements UserAgent
    {
        @Nonnull
        private final InetAddress clientAddress;

        private UserAgentImpl()
        {
            try
            {
                this.clientAddress = InetAddress.getByName( request.getClientAddress() );
            }
            catch( UnknownHostException e )
            {
                throw new CreateWebInvocationException( "could not get IP address for '" + request.getClientAddress() + "': " + e.getMessage(), e );
            }
        }

        @Nonnull
        @Override
        public InetAddress getClientAddress()
        {
            return this.clientAddress;
        }
    }

    private class WebCookieImpl implements WebCookie
    {
        @Nonnull
        private final Cookie cookie;

        WebCookieImpl( @Nonnull Cookie cookie )
        {
            this.cookie = cookie;
        }

        @Nonnull
        @Override
        public String getName()
        {
            return this.cookie.getName();
        }

        @Nullable
        @Override
        public String getValue()
        {
            return this.cookie.getValue();
        }

        @Nullable
        @Override
        public String getDomain()
        {
            return this.cookie.getDomain();
        }

        @Nullable
        @Override
        public String getPath()
        {
            return this.cookie.getPath();
        }

        @Nullable
        @Override
        public Period getMaxAge()
        {
            return Period.seconds( this.cookie.getMaxAge() );
        }

        @Override
        public boolean isSecure()
        {
            return this.cookie.getSecure();
        }

        @Nullable
        @Override
        public String getComment()
        {
            return this.cookie.getComment();
        }

        @Override
        public boolean isHttpOnly()
        {
            return this.cookie.isHttpOnly();
        }

        @Override
        public int getVersion()
        {
            return this.cookie.getVersion();
        }
    }

    private class WebSessionImpl implements WebSession, HttpSessionBindingListener
    {
        @Nonnull
        private final MapEx<String, Object> attributes;

        @Nonnull
        private final HttpSession session;

        WebSessionImpl( @Nonnull HttpSession session )
        {
            this.attributes = new ConcurrentHashMapEx<>( 10 );
            this.session = session;
            this.session.setAttribute( WEB_SESSION_ATTR_KEY, this );
        }

        @Override
        public void valueBound( HttpSessionBindingEvent event )
        {
            LOG.debug( "Created Mosaic web session '{}'", this );
        }

        @Override
        public void valueUnbound( HttpSessionBindingEvent event )
        {
            LOG.debug( "Destroyed Mosaic web session '{}'", this );
        }

        @Nonnull
        @Override
        public MapEx<String, Object> getAttributes()
        {
            return this.attributes;
        }

        @Nonnull
        @Override
        public String getId()
        {
            return this.session.getId();
        }

        @Nonnull
        @Override
        public DateTime getCreationTime()
        {
            return new DateTime( this.session.getCreationTime() );
        }

        @Nonnull
        @Override
        public DateTime getLastAccessTime()
        {
            return new DateTime( this.session.getLastAccessedTime() );
        }

        @Nonnull
        @Override
        public Duration getMaxInactiveIntervalInSeconds()
        {
            return new Duration( this.session.getMaxInactiveInterval() );
        }

        @Override
        public boolean isNew()
        {
            return this.session.isNew();
        }

        @Override
        public void invalidate()
        {
            this.session.invalidate();
        }
    }
}

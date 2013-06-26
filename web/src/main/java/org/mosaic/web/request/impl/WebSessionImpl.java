package org.mosaic.web.request.impl;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.mosaic.util.collect.ConcurrentHashMapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.web.request.WebSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class WebSessionImpl extends ConcurrentHashMapEx<String, Object>
        implements WebSession, HttpSessionBindingListener
{
    private static final Logger LOG = LoggerFactory.getLogger( WebSessionImpl.class );

    public static final String WEB_SESSION_ATTR_KEY = WebSession.class.getName() + "#" + WebSession.class.hashCode();

    @Nonnull
    private final HttpSession session;

    public WebSessionImpl( @Nonnull HttpSession session, @Nonnull ConversionService conversionService )
    {
        super( 100, conversionService );
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

package org.mosaic.server.web.jetty.impl;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.mosaic.util.collection.WrappingTypedDict;
import org.mosaic.web.HttpSession;
import org.springframework.core.convert.ConversionService;

/**
 * @author arik
 */
public class HttpSessionImpl extends WrappingTypedDict<Object> implements HttpSession {

    private final javax.servlet.http.HttpSession session;

    public HttpSessionImpl( ConversionService conversionService, javax.servlet.http.HttpSession session ) {
        super( new ConcurrentHashMap<String, List<Object>>(), conversionService, Object.class );
        this.session = session;
    }

    @Override
    public DateTime getCreationTime() {
        return new DateTime( this.session.getCreationTime() );
    }

    @Override
    public String getId() {
        return this.session.getId();
    }

    @Override
    public DateTime getLastAccessTime() {
        return new DateTime( this.session.getLastAccessedTime() );
    }

    @Override
    public Duration getMaxInactiveInterval() {
        return Duration.standardSeconds( this.session.getMaxInactiveInterval() );
    }

    @Override
    public boolean isNew() {
        return this.session.isNew();
    }

    @Override
    public void invalidate() {
        this.session.invalidate();
    }
}

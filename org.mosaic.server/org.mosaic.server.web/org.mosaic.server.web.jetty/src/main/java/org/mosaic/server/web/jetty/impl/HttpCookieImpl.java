package org.mosaic.server.web.jetty.impl;

import javax.servlet.http.Cookie;
import org.mosaic.web.HttpCookie;

/**
 * @author arik
 */
public class HttpCookieImpl implements HttpCookie {

    private final Cookie cookie;

    public HttpCookieImpl( Cookie cookie ) {
        this.cookie = cookie;
    }

    @Override
    public String getName() {
        return this.cookie.getName();
    }

    @Override
    public String getValue() {
        return this.cookie.getValue();
    }

    @Override
    public void setValue( String value ) {
        this.cookie.setValue( value );
    }

    @Override
    public String getDomain() {
        return this.cookie.getDomain();
    }

    @Override
    public void setDomain( String domain ) {
        this.cookie.setDomain( domain );
    }

    @Override
    public String getPath() {
        return this.cookie.getPath();
    }

    @Override
    public void setPath( String path ) {
        this.cookie.setPath( path );
    }

    @Override
    public Integer getMaxAge() {
        int maxAge = this.cookie.getMaxAge();
        return maxAge < 0 ? null : maxAge;
    }

    @Override
    public void setMaxAge( Integer maxAge ) {
        this.cookie.setMaxAge( maxAge == null ? -1 : maxAge );
    }

    @Override
    public boolean getSecure() {
        return this.cookie.getSecure();
    }

    @Override
    public void setSecure( boolean secure ) {
        this.cookie.setSecure( secure );
    }

    @Override
    public boolean getHttpOnly() {
        return this.cookie.isHttpOnly();  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setHttpOnly( boolean httpOnly ) {
        this.cookie.setHttpOnly( httpOnly );
    }

    @Override
    public int getVersion() {
        return this.cookie.getVersion();
    }

    @Override
    public void setVersion( int version ) {
        this.cookie.setVersion( version );
    }

    @Override
    public String getComment() {
        return this.cookie.getComment();
    }

    @Override
    public void setComment( String comment ) {
        this.cookie.setComment( comment );
    }
}

package org.mosaic.web;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import org.joda.time.DateTime;
import org.mosaic.collection.TypedDict;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/**
 * @author arik
 */
public interface HttpResponseHeaders extends TypedDict<String> {

    Set<HttpMethod> getAllow();

    void setAllow( Collection<HttpMethod> methods );

    String getCacheControl();

    void setCacheControl( String cacheControl );

    Locale getContentLanguage();

    void setContentLanguage( Locale contentLanguage );

    Long getContentLength();

    void setContentLength( Long contentLength );

    MediaType getContentType();

    void setContentType( MediaType contentType );

    Charset getContentCharset();

    void setContentCharset( Charset charset );

    void addCookie( HttpCookie cookie );

    String getETag();

    void setETag( String eTag );

    DateTime getExpires();

    void setExpires( Long seconds );

    void setExpires( DateTime expires );

    DateTime getLastModified();

    void setLastModified( DateTime lastModified );

    String getLocation();

    void setLocation( String location );

    String getPragma();

    void setPragma( String pragma );

    Integer getRetryAfter();

    void setRetryAfter( Integer retryAfter );

    String getServer();

    void setServer( String server );

    String getWwwAuthenticate();

    void setWwwAuthenticate( String wwwAuthenticate );

    void disableCache();
}

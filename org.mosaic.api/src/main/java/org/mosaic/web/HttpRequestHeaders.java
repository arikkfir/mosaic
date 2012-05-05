package org.mosaic.web;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.joda.time.DateTime;
import org.mosaic.util.collection.MultiMapAccessor;
import org.springframework.http.MediaType;

/**
 * @author arik
 */
public interface HttpRequestHeaders extends MultiMapAccessor<String, String>
{
    List<MediaType> getAccept();

    List<Charset> getAcceptCharset();

    List<Locale> getAcceptLanguage();

    String getAuthorization();

    String getCacheControl();

    List<Locale> getContentLanguage();

    Long getContentLength();

    MediaType getContentType();

    HttpCookie getCookie( String name );

    String getHost();

    Set<String> getIfMatch();

    DateTime getIfModifiedSince();

    Set<String> getIfNoneMatch();

    DateTime getIfUnmodifiedSince();

    String getPragma();

    URL getReferer();

    /**
     * @todo would be nice to return a "Device" object here
     */
    String getUserAgent();
}

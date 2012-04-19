package org.mosaic.web;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.joda.time.DateTime;
import org.mosaic.collection.TypedDict;
import org.springframework.http.MediaType;

/**
 * @author arik
 */
public interface HttpRequestHeaders extends TypedDict<String> {

    List<MediaType> getAccept();

    List<Charset> getAcceptCharset();

    List<Locale> getAcceptLanguage();

    String getAuthorization();

    String getCacheControl();

    String getContentEncoding();

    String getContentLanguage();

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

    String getUserAgent();

}

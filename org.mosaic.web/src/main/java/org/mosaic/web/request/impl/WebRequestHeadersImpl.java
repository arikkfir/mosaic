package org.mosaic.web.request.impl;

import com.google.common.net.MediaType;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.Request;
import org.joda.time.DateTime;
import org.mosaic.web.request.WebRequestHeaders;

import static com.google.common.net.HttpHeaders.*;

/**
 * @author arik
 */
final class WebRequestHeadersImpl implements WebRequestHeaders
{
    @Nonnull
    private final Request request;

    WebRequestHeadersImpl( @Nonnull Request request )
    {
        this.request = request;
    }

    @Nonnull
    @Override
    public List<MediaType> getAccept()
    {
        return HeaderUtil.getQualityMediaTypes( this.request.getHttpFields(), ACCEPT );
    }

    @Nonnull
    @Override
    public List<Charset> getAccepCharset()
    {
        return HeaderUtil.getQualityCharsets( this.request.getHttpFields(), ACCEPT_CHARSET );
    }

    @Nonnull
    @Override
    public List<String> getAcceptEncoding()
    {
        return HeaderUtil.getQualityStrings( this.request.getHttpFields(), ACCEPT_ENCODING );
    }

    @Nonnull
    @Override
    public List<Locale> getAcceptLanguage()
    {
        return HeaderUtil.getQualityLocales( this.request.getHttpFields(), ACCEPT_LANGUAGE );
    }

    @Nullable
    @Override
    public String getAuthorization()
    {
        return HeaderUtil.getString( this.request.getHttpFields(), AUTHORIZATION );
    }

    @Nullable
    @Override
    public String getCacheControl()
    {
        return HeaderUtil.getString( this.request.getHttpFields(), CACHE_CONTROL );
    }

    @Nullable
    @Override
    public String getConnection()
    {
        return HeaderUtil.getString( this.request.getHttpFields(), CONNECTION );
    }

    @Nonnull
    @Override
    public Collection<String> getContentEncoding()
    {
        return HeaderUtil.getStrings( this.request.getHttpFields(), CONTENT_ENCODING );
    }

    @Nonnull
    @Override
    public Locale getContentLanguage()
    {
        return this.request.getLocale();
    }

    @Nonnull
    @Override
    public List<Locale> getContentLanguages()
    {
        return Collections.list( this.request.getLocales() );
    }

    @Nullable
    @Override
    public Integer getContentLength()
    {
        int value = this.request.getContentLength();
        return value < 0 ? null : value;
    }

    @Nullable
    @Override
    public URI getContentLocation()
    {
        String value = HeaderUtil.getString( this.request.getHttpFields(), CONTENT_LOCATION );
        return value == null ? null : URI.create( this.request.getUri().toString() ).resolve( value );
    }

    @Nullable
    @Override
    public String getContentMd5()
    {
        return HeaderUtil.getString( this.request.getHttpFields(), CONTENT_MD5 );
    }

    @Nullable
    @Override
    public String getContentRange()
    {
        return HeaderUtil.getString( this.request.getHttpFields(), CONTENT_RANGE );
    }

    @Nullable
    @Override
    public MediaType getContentType()
    {
        String contentType = this.request.getContentType();
        return contentType == null ? null : MediaType.parse( contentType );
    }

    @Nullable
    @Override
    public DateTime getDate()
    {
        return HeaderUtil.getDateTime( this.request.getHttpFields(), DATE );
    }

    @Nullable
    @Override
    public String getExpect()
    {
        return HeaderUtil.getString( this.request.getHttpFields(), EXPECT );
    }

    @Nullable
    @Override
    public String getFrom()
    {
        return HeaderUtil.getString( this.request.getHttpFields(), FROM );
    }

    @Nonnull
    @Override
    public String getHost()
    {
        return this.request.getServerName();
    }

    @Nonnull
    @Override
    public Collection<String> getIfMatch()
    {
        return HeaderUtil.getStrings( this.request.getHttpFields(), IF_MATCH );
    }

    @Nullable
    @Override
    public DateTime getIfModifiedSince()
    {
        return HeaderUtil.getDateTime( this.request.getHttpFields(), IF_MODIFIED_SINCE );
    }

    @Nonnull
    @Override
    public Collection<String> getIfNoneMatch()
    {
        return HeaderUtil.getStrings( this.request.getHttpFields(), IF_NONE_MATCH );
    }

    @Nullable
    @Override
    public DateTime getIfRangeDate()
    {
        String value = HeaderUtil.getString( this.request.getHttpFields(), IF_RANGE );
        if( value == null )
        {
            return null;
        }

        long millis = HttpFields.parseDate( value );
        if( millis < 0 )
        {
            return null;
        }

        return new DateTime( millis );
    }

    @Nullable
    @Override
    public String getIfRangeETag()
    {
        DateTime ifRangeDate = getIfRangeDate();
        if( ifRangeDate != null )
        {
            return null;
        }
        else
        {
            return HeaderUtil.getString( this.request.getHttpFields(), IF_RANGE );
        }
    }

    @Nullable
    @Override
    public DateTime getIfUnmodifiedSince()
    {
        return HeaderUtil.getDateTime( this.request.getHttpFields(), IF_UNMODIFIED_SINCE );
    }

    @Nullable
    @Override
    public String getPragma()
    {
        return HeaderUtil.getString( this.request.getHttpFields(), PRAGMA );
    }

    @Nonnull
    @Override
    public Collection<String> getRange()
    {
        return HeaderUtil.getStrings( this.request.getHttpFields(), RANGE );
    }

    @Nullable
    @Override
    public String getReferer()
    {
        return HeaderUtil.getString( this.request.getHttpFields(), REFERER );
    }

    @Nullable
    @Override
    public String getTe()
    {
        return HeaderUtil.getString( this.request.getHttpFields(), TE );
    }

    @Nullable
    @Override
    public String getVia()
    {
        return HeaderUtil.getString( this.request.getHttpFields(), VIA );
    }

    @Nullable
    @Override
    public String getUserAgent()
    {
        return HeaderUtil.getString( this.request.getHttpFields(), USER_AGENT );
    }

    @Nullable
    @Override
    public String getWarning()
    {
        return HeaderUtil.getString( this.request.getHttpFields(), WARNING );
    }

    @Override
    public int size()
    {
        return this.request.getHttpFields().size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.request.getHttpFields().size() <= 0;
    }

    @Override
    public boolean containsKey( @Nonnull String key )
    {
        return this.request.getHttpFields().containsKey( key );
    }

    @Override
    public Collection<String> getValues( @Nonnull String key, boolean splitValues, boolean supportsQuality )
    {
        if( splitValues )
        {
            if( supportsQuality )
            {
                return HeaderUtil.getQualityStrings( this.request.getHttpFields(), key );
            }
            else
            {
                return HeaderUtil.getStrings( this.request.getHttpFields(), key );
            }
        }
        else if( supportsQuality )
        {
            throw new IllegalArgumentException( "supportsQuality cannot be true when splitValues is false" );
        }
        else
        {
            return this.request.getHttpFields().getValuesCollection( key );
        }
    }
}

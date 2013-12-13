package org.mosaic.web.request.impl;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.net.MediaType;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jetty.http.HttpFields;
import org.joda.time.DateTime;

import static com.google.common.collect.Lists.transform;
import static java.util.Collections.enumeration;
import static org.eclipse.jetty.http.HttpFields.qualityList;

/**
 * @author arik
 */
final class Header
{
    private static final Splitter HEADER_MULTIVALUE_SPLITTER = Splitter.on( ',' ).trimResults().omitEmptyStrings();

    @Nullable
    static String getString( @Nonnull HttpFields httpFields, @Nonnull String headerName )
    {
        return httpFields.getStringField( headerName );
    }

    @Nullable
    static DateTime getDateTime( @Nonnull HttpFields httpFields, @Nonnull String headerName )
    {
        long value = httpFields.getDateField( headerName );
        return value < 0 ? null : new DateTime( value );
    }

    @Nullable
    static Integer getInteger( @Nonnull HttpFields httpFields, @Nonnull String headerName )
    {
        long value = httpFields.getLongField( headerName );
        return value < 0 ? null : ( int ) value;
    }

    @Nonnull
    static List<String> getStrings( @Nonnull HttpFields httpFields, @Nonnull String headerName )
    {
        Enumeration<String> unparsed = httpFields.getValues( headerName );
        List<String> values = new LinkedList<>();
        while( unparsed.hasMoreElements() )
        {
            values.addAll( HEADER_MULTIVALUE_SPLITTER.splitToList( unparsed.nextElement() ) );
        }
        return values;
    }

    @Nonnull
    static List<String> getQualityStrings( @Nonnull HttpFields httpFields, @Nonnull String headerName )
    {
        List<String> values = qualityList( enumeration( getStrings( httpFields, headerName ) ) );
        return transform( values, new Function<String, String>()
        {
            @Nullable
            @Override
            public String apply( @Nullable String input )
            {
                return input;
            }
        } );
    }

    @Nonnull
    static List<Charset> getQualityCharsets( @Nonnull HttpFields httpFields, @Nonnull String headerName )
    {
        return transform( getQualityStrings( httpFields, headerName ), new Function<String, Charset>()
        {
            @Nullable
            @Override
            public Charset apply( @Nullable String input )
            {
                return input == null ? null : Charset.forName( input );
            }
        } );
    }

    @Nonnull
    static List<Locale> getQualityLocales( @Nonnull HttpFields httpFields, @Nonnull String headerName )
    {
        return transform( getQualityStrings( httpFields, headerName ), new Function<String, Locale>()
        {
            @Nullable
            @Override
            public Locale apply( @Nullable String input )
            {
                return input == null ? null : Locale.forLanguageTag( input );
            }
        } );
    }

    @Nonnull
    static List<MediaType> getQualityMediaTypes( @Nonnull HttpFields httpFields, @Nonnull String headerName )
    {
        return transform( getQualityStrings( httpFields, headerName ), new Function<String, MediaType>()
        {
            @Nullable
            @Override
            public MediaType apply( @Nullable String input )
            {
                return input == null ? null : MediaType.parse( input );
            }
        } );
    }

    private Header()
    {
        // no-op
    }
}
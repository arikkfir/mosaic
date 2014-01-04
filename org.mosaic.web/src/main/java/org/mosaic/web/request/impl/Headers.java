package org.mosaic.web.request.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.util.StringUtil;
import org.joda.time.DateTime;

import static java.util.Arrays.asList;
import static java.util.Collections.enumeration;
import static java.util.Collections.list;
import static org.eclipse.jetty.http.HttpFields.qualityList;

/**
 * @author arik
 */
final class Headers
{
    private static final Map<String, HeaderValuesExtractor> HEADER_EXTRACTORS;

    private static final HeaderValuesExtractor SINGLE_VALUE_EXTRACTOR = new HeaderValuesExtractor()
    {
        @Nonnull
        @Override
        protected List<String> extractInternal( @Nonnull String headerName, @Nonnull HttpFields httpFields )
        {
            String value = httpFields.getStringField( headerName );
            return value == null ? Collections.<String>emptyList() : asList( value );
        }
    };

    private static final HeaderValuesExtractor COMMA_SEPARATED_VALUE_EXTRACTOR = new HeaderValuesExtractor()
    {
        @Nonnull
        @Override
        protected List<String> extractInternal( @Nonnull String headerName, @Nonnull HttpFields httpFields )
        {
            return list( httpFields.getValues( headerName, "," ) );
        }
    };

    private static final HeaderValuesExtractor QUALIFIED_COMMA_SEPARATED_VALUE_EXTRACTOR = new HeaderValuesExtractor()
    {
        @Nonnull
        @Override
        protected List<String> extractInternal( @Nonnull String headerName, @Nonnull HttpFields httpFields )
        {
            // TODO: ensure highest quality returned first here
            return qualityList( enumeration( list( httpFields.getValues( headerName, "," ) ) ) );
        }
    };

    private static final Function<String, Locale> STRING_TO_LOCALE_TRANSFORM_FUNCTION = new Function<String, Locale>()
    {
        @Nullable
        @Override
        public Locale apply( @Nullable String input )
        {
            return input == null ? null : Locale.forLanguageTag( input );
        }
    };

    private static final Function<Locale, String> LOCALE_TO_STRING_TRANSFORM_FUNCTION = new Function<Locale, String>()
    {
        @Nullable
        @Override
        public String apply( @Nullable Locale input )
        {
            return input == null ? null : input.toLanguageTag();
        }
    };

    private static final Function<String, MediaType> MEDIA_TYPE_TO_STRING_TRANSFORM_FUNCTION = new Function<String, MediaType>()
    {
        @Nullable
        @Override
        public MediaType apply( @Nullable String input )
        {
            return input == null ? null : MediaType.parse( input );
        }
    };

    private static final Function<String, Charset> CHARSET_TO_STRING_TRANSFORM_FUNCTION = new Function<String, Charset>()
    {
        @Nullable
        @Override
        public Charset apply( @Nullable String input )
        {
            return input == null ? null : Charset.forName( input );
        }
    };

    static
    {
        Map<String, HeaderValuesExtractor> headerExtractors = new HashMap<>();
        for( Field field : HttpHeaders.class.getDeclaredFields() )
        {
            if( Modifier.isStatic( field.getModifiers() ) && field.getType().equals( String.class ) )
            {
                Object value;
                try
                {
                    value = field.get( null );
                }
                catch( IllegalAccessException e )
                {
                    throw new IllegalStateException( "could not extract value of field '" + field.getName() + "': " + e.getMessage(), e );
                }

                if( value != null )
                {
                    String headerName = value.toString();
                    switch( headerName )
                    {
                        case HttpHeaders.ACCEPT:
                        case HttpHeaders.ACCEPT_CHARSET:
                        case HttpHeaders.ACCEPT_ENCODING:
                        case HttpHeaders.ACCEPT_LANGUAGE:
                            headerExtractors.put( headerName, QUALIFIED_COMMA_SEPARATED_VALUE_EXTRACTOR );
                            break;

                        case HttpHeaders.ACCEPT_RANGES:
                        case HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS:
                        case HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS:
                        case HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN:
                        case HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS:
                        case HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS:
                        case HttpHeaders.ALLOW:
                        case HttpHeaders.CONTENT_ENCODING:
                        case HttpHeaders.CONTENT_LANGUAGE:
                        case HttpHeaders.IF_MATCH:
                        case HttpHeaders.IF_NONE_MATCH:
                        case HttpHeaders.X_FORWARDED_FOR:
                            headerExtractors.put( headerName, COMMA_SEPARATED_VALUE_EXTRACTOR );
                            break;

                        default:
                            headerExtractors.put( headerName, SINGLE_VALUE_EXTRACTOR );
                    }
                }
            }
        }
        HEADER_EXTRACTORS = Collections.unmodifiableMap( headerExtractors );
    }

    private static abstract class HeaderValuesExtractor
    {
        @Nonnull
        protected final List<String> extract( @Nonnull String headerName, @Nonnull HttpFields httpFields )
        {
            return extractInternal( headerName, httpFields );
        }

        @Nonnull
        protected abstract List<String> extractInternal( @Nonnull String headerName, @Nonnull HttpFields httpFields );
    }

    @Nonnull
    private final HttpFields httpFields;

    @Nonnull
    private final Map<String, List<String>> multiValues = new HashMap<>( 15 );

    Headers( @Nonnull HttpFields httpFields )
    {
        this.httpFields = httpFields;
    }

    int size()
    {
        return this.httpFields.size();
    }

    boolean containsKey( @Nonnull String headerName )
    {
        return this.httpFields.containsKey( headerName );
    }

    boolean containsEntry( @Nonnull String headerName, @Nonnull String value )
    {
        return getStrings( headerName ).contains( value );
    }

    @Nonnull
    Set<String> keySet()
    {
        return Sets.newHashSet( this.httpFields.getFieldNamesCollection() );
    }

    void remove( @Nonnull String headerName )
    {
        this.httpFields.remove( headerName );
    }

    @Nonnull
    List<String> getStrings( @Nonnull String headerName )
    {
        String lcHeaderName = headerName.toLowerCase();

        List<String> values = this.multiValues.get( lcHeaderName );
        if( values == null )
        {
            HeaderValuesExtractor extractor = getExtractor( lcHeaderName );
            values = extractor.extract( lcHeaderName, this.httpFields );
            this.multiValues.put( lcHeaderName, values );
        }
        return values;
    }

    void setStrings( @Nonnull String headerName, @Nullable List<String> values )
    {
        if( values == null )
        {
            this.httpFields.remove( headerName );
        }
        else
        {
            StringBuilder s = new StringBuilder( values.size() * 4 );
            for( String value : values )
            {
                if( s.length() > 0 )
                {
                    s.append( ", " );
                }
                s.append( value );
            }
            this.httpFields.put( headerName, s.toString() );
        }
    }

    @Nullable
    String getString( @Nonnull String headerName )
    {
        List<String> values = getStrings( headerName );
        return values.isEmpty() ? null : values.get( 0 );
    }

    void setString( @Nonnull String headerName, @Nullable String value )
    {
        this.httpFields.put( headerName, value );
    }

    @Nullable
    Integer getInteger( @Nonnull String headerName )
    {
        String value = getString( headerName );
        if( value != null )
        {
            return StringUtil.toInt( HttpFields.valueParameters( value, null ) );
        }
        return null;
    }

    void setInteger( @Nonnull String headerName, @Nullable Integer value )
    {
        if( value == null )
        {
            this.httpFields.remove( headerName );
        }
        else
        {
            this.httpFields.putLongField( headerName, value );
        }
    }

    @Nullable
    Long getLong( @Nonnull String headerName )
    {
        String value = getString( headerName );
        if( value != null )
        {
            return StringUtil.toLong( HttpFields.valueParameters( value, null ) );
        }
        return null;
    }

    void setLong( @Nonnull String headerName, @Nullable Long value )
    {
        if( value == null )
        {
            this.httpFields.remove( headerName );
        }
        else
        {
            this.httpFields.putLongField( headerName, value );
        }
    }

    @Nullable
    DateTime getDateTime( @Nonnull String headerName )
    {
        String value = getString( headerName );
        if( value != null )
        {
            long millis = HttpFields.parseDate( HttpFields.valueParameters( value, null ) );
            return millis < 0 ? null : new DateTime( millis );
        }
        return null;
    }

    void setDateTime( @Nonnull String headerName, @Nullable DateTime value )
    {
        if( value == null )
        {
            this.httpFields.remove( headerName );
        }
        else
        {
            this.httpFields.putDateField( headerName, value.getMillis() );
        }
    }

    @Nullable
    Locale getLocale( @Nonnull String headerName )
    {
        return STRING_TO_LOCALE_TRANSFORM_FUNCTION.apply( getString( headerName ) );
    }

    void setLocale( @Nonnull String headerName, @Nullable Locale value )
    {
        if( value == null )
        {
            this.httpFields.remove( headerName );
        }
        else
        {
            this.httpFields.put( headerName, value.toLanguageTag() );
        }
    }

    @Nonnull
    List<Locale> getLocales( @Nonnull String headerName )
    {
        return Lists.transform( getStrings( headerName ), STRING_TO_LOCALE_TRANSFORM_FUNCTION );
    }

    void setLocales( @Nonnull String headerName, @Nullable List<Locale> value )
    {
        if( value == null )
        {
            this.httpFields.remove( headerName );
        }
        else
        {
            setStrings( headerName, Lists.transform( value, LOCALE_TO_STRING_TRANSFORM_FUNCTION ) );
        }
    }

    @Nullable
    MediaType getMediaType( @Nonnull String headerName )
    {
        return MEDIA_TYPE_TO_STRING_TRANSFORM_FUNCTION.apply( getString( headerName ) );
    }

    void setMediaType( @Nonnull String headerName, @Nullable MediaType value )
    {
        setString( headerName, Objects.toString( value, null ) );
    }

    @Nonnull
    List<MediaType> getMediaTypes( @Nonnull String headerName )
    {
        return Lists.transform( getStrings( headerName ), MEDIA_TYPE_TO_STRING_TRANSFORM_FUNCTION );
    }

    @Nullable
    Charset getCharset( @Nonnull String headerName )
    {
        return CHARSET_TO_STRING_TRANSFORM_FUNCTION.apply( getString( headerName ) );
    }

    @Nonnull
    List<Charset> getCharsets( @Nonnull String headerName )
    {
        return Lists.transform( getStrings( headerName ), CHARSET_TO_STRING_TRANSFORM_FUNCTION );
    }

    @Nonnull
    private HeaderValuesExtractor getExtractor( @Nonnull String headerName )
    {
        HeaderValuesExtractor extractor = HEADER_EXTRACTORS.get( headerName );
        return extractor == null ? SINGLE_VALUE_EXTRACTOR : extractor;
    }
}

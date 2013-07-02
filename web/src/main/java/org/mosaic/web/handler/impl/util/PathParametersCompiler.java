package org.mosaic.web.handler.impl.util;

import com.google.common.cache.CacheLoader;
import com.google.common.reflect.TypeToken;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import javax.annotation.Nonnull;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.util.collect.HashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.collect.UnmodifiableMapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.pair.Pair;
import org.mosaic.web.request.IllegalPathTemplateException;

import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;

/**
 * @author arik
 */
@Bean
public class PathParametersCompiler extends CacheLoader<Pair<String, String>, MapEx<String, String>>
{
    public static final MapEx<String, String> NO_MATCH = new HashMapEx<>( new ConversionService()
    {
        @Nonnull
        @Override
        public <Source, Dest> Dest convert( @Nonnull Source source, @Nonnull TypeToken<Dest> targetTypeToken )
        {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public <Source, Dest> Dest convert( @Nonnull Source source, @Nonnull Class<Dest> targetTypeToken )
        {
            throw new UnsupportedOperationException();
        }
    } );

    @Nonnull
    private ConversionService conversionService;

    @ServiceRef
    public void setConversionService( @Nonnull ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @Override
    public MapEx<String, String> load( Pair<String, String> key ) throws Exception
    {
        String pathTemplate = key.getLeft();
        if( pathTemplate == null )
        {
            throw new IllegalArgumentException( "Illegal path template cache key: " + key );
        }

        String encodedPath = key.getRight();
        if( encodedPath == null )
        {
            throw new IllegalArgumentException( "Illegal path template cache key: " + key );
        }

        StringBuilder pathTemplateBuilder = new StringBuilder( pathTemplate.length() * 2 );
        List<String> variableNames = new LinkedList<>();

        StringBuilder buffer = new StringBuilder( pathTemplate.length() );
        char[] chars = pathTemplate.toCharArray();
        for( int i = 0; i < chars.length; i++ )
        {
            char c = chars[ i ];
            if( c == '{' )
            {
                pathTemplateBuilder.append( quote( buffer.toString() ) );
                buffer.delete( 0, buffer.length() );

                int end = pathTemplate.indexOf( '}', i );
                if( end < 0 )
                {
                    throw new IllegalPathTemplateException( "open-ended curly brace at index " + i, pathTemplate );
                }
                else if( end == i + 1 )
                {
                    throw new IllegalPathTemplateException( "empty path variable name at index " + i, pathTemplate );
                }

                variableNames.add( new String( chars, i + 1, end - i - 1 ) );
                i = end + 1;

                pathTemplateBuilder.append( "([^/]+)" );
            }
            else if( c == '*' && i + 1 < chars.length && chars[ i + 1 ] == '*' )
            {
                // a "**" encountered

                if( buffer.length() > 0 )
                {
                    pathTemplateBuilder.append( quote( buffer.toString() ) );
                    buffer.delete( 0, buffer.length() );
                }

                pathTemplateBuilder.append( ".*" );

                // skip the next "*" too
                i++;
            }
            else if( c == '*' )
            {
                pathTemplateBuilder.append( "[^/]*" );
            }
            else
            {
                buffer.append( c );
            }
        }

        if( buffer.length() > 0 )
        {
            pathTemplateBuilder.append( quote( buffer.toString() ) );
            buffer.delete( 0, buffer.length() );
        }

        Matcher matcher = compile( pathTemplateBuilder.toString() ).matcher( encodedPath );
        if( matcher.matches() )
        {
            MapEx<String, String> pathParameters = new HashMapEx<>( variableNames.size(), this.conversionService );
            for( int i = 0; i < variableNames.size(); i++ )
            {
                String variableName = variableNames.get( i );
                String variableValue = matcher.group( i );
                pathParameters.put( variableName, variableValue );
            }
            return new UnmodifiableMapEx<>( pathParameters );
        }
        else
        {
            return NO_MATCH;
        }
    }
}

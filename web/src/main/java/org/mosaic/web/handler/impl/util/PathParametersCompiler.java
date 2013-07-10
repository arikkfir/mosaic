package org.mosaic.web.handler.impl.util;

import com.google.common.cache.CacheLoader;
import com.google.common.reflect.TypeToken;
import javax.annotation.Nonnull;
import org.mosaic.lifecycle.annotation.Bean;
import org.mosaic.lifecycle.annotation.ServiceRef;
import org.mosaic.util.collect.HashMapEx;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.collect.UnmodifiableMapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.pair.Pair;

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
    public MapEx<String, String> load( Pair<String, String> key )
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

        MapEx<String, String> pathParameters = new HashMapEx<>( 5, this.conversionService );

        AntPathMatcher pathMatcher = new AntPathMatcher();
        if( pathMatcher.doMatch( pathTemplate, encodedPath, true, pathParameters ) )
        {
            return new UnmodifiableMapEx<>( pathParameters );
        }
        else
        {
            return NO_MATCH;
        }
    }
}

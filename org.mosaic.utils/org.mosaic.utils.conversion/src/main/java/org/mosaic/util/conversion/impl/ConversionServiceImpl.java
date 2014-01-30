package org.mosaic.util.conversion.impl;

import com.google.common.reflect.TypeToken;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.conversion.ConversionService;
import org.mosaic.util.conversion.Converter;
import org.mosaic.util.reflection.TypeTokens;

/**
 * @author arik
 */
final class ConversionServiceImpl implements ConversionService
{
    @Nonnull
    private final ConvertersGraph convertersGraph;

    ConversionServiceImpl( @Nonnull ConvertersGraph convertersGraph )
    {
        this.convertersGraph = convertersGraph;
    }

    @SuppressWarnings( "unchecked" )
    @Nullable
    @Override
    public <Source, Dest> Dest convert( @Nullable Source source, @Nonnull TypeToken<Dest> targetTypeToken )
    {
        // null always returns absent Optional
        if( source == null )
        {
            return null;
        }

        // avoid conversion if not necessary
        TypeToken<?> sourceTypeToken = TypeTokens.of( source.getClass() );
        if( targetTypeToken.isAssignableFrom( sourceTypeToken ) )
        {
            return ( Dest ) source;
        }

        // find converter and convert; if no converter found, the 'getConverter' method will throw a ConversionException
        Converter converter = this.convertersGraph.getConverter( sourceTypeToken, targetTypeToken );
        return ( Dest ) converter.convert( source );
    }

    @SuppressWarnings( "unchecked" )
    @Nullable
    @Override
    public <Source, Dest> Dest convert( @Nullable Source source, @Nonnull Class<Dest> targetType )
    {
        // null always returns absent Optional
        if( source == null )
        {
            return null;
        }

        // avoid conversion if not necessary
        TypeToken<?> sourceTypeToken = TypeTokens.of( source.getClass() );
        TypeToken<?> targetTypeToken = TypeTokens.of( targetType );
        if( targetTypeToken.isAssignableFrom( sourceTypeToken ) )
        {
            return ( Dest ) source;
        }

        // find converter and convert; if no converter found, the 'getConverter' method will throw a ConversionException
        Converter converter = this.convertersGraph.getConverter( sourceTypeToken, targetTypeToken );
        return ( Dest ) converter.convert( source );
    }
}

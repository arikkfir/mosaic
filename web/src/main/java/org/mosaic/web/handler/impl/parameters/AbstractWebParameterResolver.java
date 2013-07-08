package org.mosaic.web.handler.impl.parameters;

import com.google.common.reflect.TypeToken;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public abstract class AbstractWebParameterResolver implements MethodHandle.ParameterResolver, ConversionService
{
    @Nullable
    private final ConversionService conversionService;

    protected AbstractWebParameterResolver()
    {
        this( null );
    }

    public AbstractWebParameterResolver( @Nullable ConversionService conversionService )
    {
        this.conversionService = conversionService;
    }

    @Nonnull
    public WebRequest getRequest( @Nonnull MapEx<String, Object> context )
    {
        return context.require( "request", WebRequest.class );
    }

    @Nonnull
    @Override
    public <Source, Dest> Dest convert( @Nonnull Source source, @Nonnull TypeToken<Dest> targetTypeToken )
    {
        return requireConversionService().convert( source, targetTypeToken );
    }

    @Nonnull
    @Override
    public <Source, Dest> Dest convert( @Nonnull Source source, @Nonnull Class<Dest> targetTypeToken )
    {
        return requireConversionService().convert( source, targetTypeToken );
    }

    @Nonnull
    private ConversionService requireConversionService()
    {
        if( this.conversionService == null )
        {
            throw new IllegalStateException( "ConversionService not set on " + this );
        }
        else
        {
            return this.conversionService;
        }
    }
}

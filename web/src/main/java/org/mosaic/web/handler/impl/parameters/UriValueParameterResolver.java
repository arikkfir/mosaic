package org.mosaic.web.handler.impl.parameters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.handler.annotation.UriValue;

/**
 * @author arik
 */
public class UriValueParameterResolver extends AnnotatedParameterResolver<UriValue>
{
    public UriValueParameterResolver( @Nonnull ConversionService conversionService )
    {
        super( conversionService );
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    protected Object resolveWithAnnotation( @Nonnull MethodParameter parameter,
                                            @Nonnull MapEx<String, Object> context,
                                            @Nonnull UriValue annotation ) throws Exception
    {
        String paramName = annotation.value().isEmpty() ? parameter.getName() : annotation.value();
        Object value = context.require( "pathParameters", MapEx.class ).get( paramName );
        if( value == null )
        {
            return null;
        }
        else
        {
            return convert( value, parameter.getType() );
        }
    }
}

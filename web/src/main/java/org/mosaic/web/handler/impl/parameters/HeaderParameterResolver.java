package org.mosaic.web.handler.impl.parameters;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.handler.annotation.Header;

/**
 * @author arik
 */
public class HeaderParameterResolver extends AnnotatedParameterResolver<Header>
{
    public HeaderParameterResolver( @Nonnull ConversionService conversionService )
    {
        super( conversionService );
    }

    @Nullable
    @Override
    protected Object resolveWithAnnotation( @Nonnull MethodParameter parameter,
                                            @Nonnull MapEx<String, Object> context,
                                            @Nonnull Header annotation ) throws Exception
    {
        String headerName = annotation.value().isEmpty() ? parameter.getName() : annotation.value();
        Collection<String> values = getRequest( context ).getHeaders().getAllHeaders().get( headerName );
        if( values == null )
        {
            return null;
        }

        if( parameter.isArray() || parameter.isCollection() || parameter.isMap() || parameter.isProperties() )
        {
            return convert( values, parameter.getType() );
        }
        else if( values.isEmpty() )
        {
            return null;
        }
        else
        {
            return convert( values.iterator().next(), parameter.getType() );
        }
    }
}

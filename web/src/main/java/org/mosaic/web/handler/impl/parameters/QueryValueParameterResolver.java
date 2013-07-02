package org.mosaic.web.handler.impl.parameters;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.handler.annotation.QueryValue;

/**
 * @author arik
 */
public class QueryValueParameterResolver extends AnnotatedParameterResolver<QueryValue>
{
    public QueryValueParameterResolver( @Nonnull ConversionService conversionService )
    {
        super( conversionService );
    }

    @Nullable
    @Override
    protected Object resolveWithAnnotation( @Nonnull MethodParameter parameter,
                                            @Nonnull MapEx<String, Object> context,
                                            @Nonnull QueryValue annotation ) throws Exception
    {
        String paramName = annotation.value().isEmpty() ? parameter.getName() : annotation.value();
        Collection<String> values = getRequest( context ).getUri().getDecodedQueryParameters().get( paramName );
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

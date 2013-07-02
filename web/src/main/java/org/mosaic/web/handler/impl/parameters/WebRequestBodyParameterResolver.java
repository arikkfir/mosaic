package org.mosaic.web.handler.impl.parameters;

import java.io.InputStream;
import java.io.Reader;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.handler.annotation.Body;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public class WebRequestBodyParameterResolver extends AbstractWebParameterResolver
{
    public WebRequestBodyParameterResolver( @Nullable ConversionService conversionService )
    {
        super( conversionService );
    }

    @Nullable
    @Override
    public Object resolve( @Nonnull MethodParameter parameter, @Nonnull MapEx<String, Object> resolveContext )
            throws Exception
    {
        if( parameter.getType().isAssignableFrom( WebRequest.Body.class ) )
        {
            return getRequest( resolveContext ).getBody();
        }
        else if( parameter.hasAnnotation( Body.class ) )
        {
            if( parameter.getType().isAssignableFrom( InputStream.class ) )
            {
                return getRequest( resolveContext ).getBody().asStream();
            }
            else if( parameter.getType().isAssignableFrom( Reader.class ) )
            {
                return getRequest( resolveContext ).getBody().asReader();
            }
            else
            {
                return convert( getRequest( resolveContext ).getBody(), parameter.getType() );
            }
        }
        return SKIP;
    }
}

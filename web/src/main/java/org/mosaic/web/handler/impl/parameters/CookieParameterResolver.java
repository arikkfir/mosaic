package org.mosaic.web.handler.impl.parameters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.handler.annotation.Cookie;
import org.mosaic.web.request.RequestCookie;

/**
 * @author arik
 */
public class CookieParameterResolver extends AnnotatedParameterResolver<Cookie>
{
    public CookieParameterResolver( @Nonnull ConversionService conversionService )
    {
        super( conversionService );
    }

    @Nullable
    @Override
    protected Object resolveWithAnnotation( @Nonnull MethodParameter parameter,
                                            @Nonnull MapEx<String, Object> context,
                                            @Nonnull Cookie annotation ) throws Exception
    {
        String cookieName = annotation.value().isEmpty() ? parameter.getName() : annotation.value();
        RequestCookie cookie = getRequest( context ).getHeaders().getCookie( cookieName );
        if( cookie == null )
        {
            return null;
        }
        else
        {
            return convert( cookie, parameter.getType() );
        }
    }
}

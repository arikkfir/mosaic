package org.mosaic.web.handler.impl.adapter;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.handler.annotation.UriValue;
import org.mosaic.web.request.WebRequest;

/**
 * @author arik
 */
public abstract class AbstractPathMethodEndpointAdapter extends AbstractMethodEndpointAdapter
{
    @Nonnull
    protected final String[] pathTemplates;

    public AbstractPathMethodEndpointAdapter( long id,
                                              int rank,
                                              @Nonnull MethodEndpoint endpoint,
                                              @Nonnull ExpressionParser expressionParser,
                                              @Nonnull final ConversionService conversionService )
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        super( id, rank, endpoint, expressionParser, conversionService );

        Annotation endpointType = getEndpoint().getType();
        Class<? extends Annotation> annotationType = endpointType.annotationType();
        java.lang.reflect.Method method = annotationType.getDeclaredMethod( "value" );
        this.pathTemplates = ( String[] ) method.invoke( endpointType );

        addParameterResolvers( new MethodHandle.ParameterResolver()
        {
            @Nullable
            @Override
            public Object resolve( @Nonnull MethodParameter parameter,
                                   @Nonnull MapEx<String, Object> resolveContext )
            {
                UriValue ann = parameter.getAnnotation( UriValue.class );
                if( ann != null )
                {
                    PathHandlerContext handlerContext = resolveContext.require( "handlerContext", PathHandlerContext.class );
                    String pathTemplate = handlerContext.getPathTemplate();
                    MapEx<String, String> pathParameters = handlerContext.getRequest().getUri().getPathParameters( pathTemplate );
                    if( pathParameters == null )
                    {
                        return null;
                    }

                    String value = pathParameters.get( ann.value() );
                    if( value == null )
                    {
                        return null;
                    }
                    else
                    {
                        return conversionService.convert( value, parameter.getType() );
                    }
                }
                return SKIP;
            }
        } );
    }

    @Nonnull
    public String[] getPathTemplates()
    {
        return this.pathTemplates;
    }

    protected class PathHandlerContext extends SimpleContext
    {
        @Nonnull
        private final String pathTemplate;

        @Nonnull
        private final MapEx<String, String> pathVariables;

        public PathHandlerContext( @Nonnull WebRequest request,
                                   @Nonnull String pathTemplate,
                                   @Nonnull MapEx<String, String> pathVariables )
        {
            super( request );
            this.pathTemplate = pathTemplate;
            this.pathVariables = pathVariables;
        }

        @Nonnull
        public String getPathTemplate()
        {
            return this.pathTemplate;
        }

        @Nonnull
        public MapEx<String, String> getPathVariables()
        {
            return this.pathVariables;
        }
    }
}

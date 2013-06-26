package org.mosaic.web.handler.impl.adapter;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class ExceptionHandlerEndpointAdapter extends AbstractMethodEndpointAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger( ExceptionHandlerEndpointAdapter.class );

    @Nullable
    private final Class<? extends Throwable> exceptionType;

    public ExceptionHandlerEndpointAdapter( long id,
                                            int rank,
                                            @Nonnull MethodEndpoint endpoint,
                                            @Nonnull ExpressionParser expressionParser,
                                            @Nonnull ConversionService conversionService )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        super( id, rank, endpoint, expressionParser, conversionService );

        Class<? extends Throwable> exceptionType = null;
        for( MethodParameter parameter : getEndpoint().getParameters() )
        {
            Class<?> parameterType = parameter.getType().getRawType();
            if( Throwable.class.isAssignableFrom( parameterType ) )
            {
                exceptionType = parameterType.asSubclass( Throwable.class );
                break;
            }
        }

        if( exceptionType == null )
        {
            LOG.warn( "Exception handler {} has no exception parameter - it will not be activated on any error", getEndpoint() );
            this.exceptionType = null;
        }
        else
        {
            this.exceptionType = exceptionType;
        }

        addParameterResolvers( new MethodHandle.ParameterResolver()
        {
            @Nullable
            @Override
            public Object resolve( @Nonnull MethodParameter parameter,
                                   @Nonnull MapEx<String, Object> resolveContext )
            {
                if( Throwable.class.isAssignableFrom( parameter.getType().getRawType() ) )
                {
                    return resolveContext.require( "exception" );
                }
                return SKIP;
            }
        } );
    }

    @Nullable
    public ExceptionHandlerContext matches( @Nonnull WebRequest request, @Nonnull Throwable exception )
    {
        if( !matchesWebApplication( request ) )
        {
            return null;
        }

        if( !matchesHttpMethod( request ) )
        {
            return null;
        }

        if( this.exceptionType == null || !this.exceptionType.isInstance( exception ) )
        {
            return null;
        }

        return new ExceptionHandlerContext( request, exception );
    }

    @Nullable
    public Object handle( @Nonnull HandlerContext context ) throws Exception
    {
        Map<String, Object> resolveContext = new HashMap<>();
        resolveContext.put( "handlerContext", context );
        resolveContext.put( "exception", ( ( ExceptionHandlerContext ) context ).getException() );
        return getEndpointInvoker().resolve( resolveContext ).invoke();
    }

    public class ExceptionHandlerContext extends SimpleContext
    {
        @Nonnull
        private final Throwable exception;

        private final int distance;

        public ExceptionHandlerContext( @Nonnull WebRequest request, @Nonnull Throwable exception )
        {
            super( request );
            this.exception = exception;

            int distance = 0;
            Class throwableType = exception.getClass();
            while( throwableType != null && !throwableType.equals( ExceptionHandlerEndpointAdapter.this.exceptionType ) )
            {
                distance++;
                throwableType = throwableType.getSuperclass();
            }
            if( throwableType == null )
            {
                throw new IllegalStateException();
            }
            else
            {
                this.distance = distance;
            }
        }

        @Nonnull
        public Throwable getException()
        {
            return this.exception;
        }

        public int getDistance()
        {
            return distance;
        }
    }
}

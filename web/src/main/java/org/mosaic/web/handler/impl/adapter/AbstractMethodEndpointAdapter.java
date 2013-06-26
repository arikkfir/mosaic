package org.mosaic.web.handler.impl.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.MethodEndpoint;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.convert.ConversionService;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionEvaluateException;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.reflection.MethodHandle;
import org.mosaic.util.reflection.MethodParameter;
import org.mosaic.web.application.WebApplication;
import org.mosaic.web.handler.annotation.*;
import org.mosaic.web.net.HttpMethod;
import org.mosaic.web.request.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;
import static org.mosaic.web.net.HttpMethod.GET;
import static org.mosaic.web.net.HttpMethod.POST;

/**
 * @author arik
 */
public abstract class AbstractMethodEndpointAdapter implements Comparable<AbstractMethodEndpointAdapter>
{
    private static final Logger LOG = LoggerFactory.getLogger( AbstractMethodEndpointAdapter.class );

    private final long id;

    private final int rank;

    @Nonnull
    private final MethodEndpoint endpoint;

    @Nullable
    private final Expression.Invoker webAppFilterExpression;

    @Nullable
    private final Expression.Invoker securityExpression;

    @Nonnull
    private final Collection<HttpMethod> httpMethods;

    @Nonnull
    private final List<MethodHandle.ParameterResolver> parameterResolvers = new LinkedList<>();

    @Nullable
    private MethodEndpoint.Invoker endpointInvoker;

    public AbstractMethodEndpointAdapter( long id,
                                          int rank,
                                          @Nonnull MethodEndpoint endpoint,
                                          @Nonnull ExpressionParser expressionParser,
                                          @Nonnull final ConversionService conversionService )
    {
        this.endpoint = endpoint;
        this.id = id;
        this.rank = rank;

        WebAppFilter webAppFilterAnn = getAnnotation( WebAppFilter.class );
        if( webAppFilterAnn != null )
        {
            this.webAppFilterExpression = expressionParser.parseExpression( webAppFilterAnn.value() ).createInvoker();
        }
        else
        {
            this.webAppFilterExpression = null;
        }

        Secured securedAnn = getAnnotation( Secured.class );
        if( securedAnn != null )
        {
            this.securityExpression = expressionParser.parseExpression( securedAnn.value() ).createInvoker();
        }
        else
        {
            this.securityExpression = null;
        }

        Method methodAnn = getAnnotation( Method.class );
        if( methodAnn != null )
        {
            this.httpMethods = asList( methodAnn.value() );
        }
        else
        {
            this.httpMethods = asList( GET, POST );
        }

        addParameterResolvers(
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext )
                    {
                        Cookie ann = parameter.getAnnotation( Cookie.class );
                        if( ann != null )
                        {
                            HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                            RequestCookie cookie = handlerContext.getRequest().getHeaders().getCookie( ann.value() );
                            if( cookie == null )
                            {
                                return null;
                            }
                            else
                            {
                                return conversionService.convert( cookie, parameter.getType() );
                            }
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext )
                    {
                        Header ann = parameter.getAnnotation( Header.class );
                        if( ann != null )
                        {
                            HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                            Collection<String> values = handlerContext.getRequest().getHeaders().getAllHeaders().get( ann.value() );
                            if( values == null )
                            {
                                return null;
                            }

                            if( parameter.isArray() || parameter.isCollection() || parameter.isMap() || parameter.isProperties() )
                            {
                                return conversionService.convert( values, parameter.getType() );
                            }
                            else if( values.isEmpty() )
                            {
                                return null;
                            }
                            else
                            {
                                return conversionService.convert( values.iterator().next(), parameter.getType() );
                            }
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext )
                    {
                        QueryValue ann = parameter.getAnnotation( QueryValue.class );
                        if( ann != null )
                        {
                            HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                            Collection<String> values = handlerContext.getRequest().getUri().getDecodedQueryParameters().get( ann.value() );
                            if( values == null )
                            {
                                return null;
                            }

                            if( parameter.isArray() || parameter.isCollection() || parameter.isMap() || parameter.isProperties() )
                            {
                                return conversionService.convert( values, parameter.getType() );
                            }
                            else if( values.isEmpty() )
                            {
                                return null;
                            }
                            else
                            {
                                return conversionService.convert( values.iterator().next(), parameter.getType() );
                            }
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext )
                    {
                        if( parameter.getType().isAssignableFrom( WebApplication.class ) )
                        {
                            HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                            return handlerContext.getRequest().getApplication();
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext )
                    {
                        if( parameter.getType().isAssignableFrom( WebDevice.class ) )
                        {
                            HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                            return handlerContext.getRequest().getDevice();
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext ) throws IOException
                    {
                        if( parameter.getType().isAssignableFrom( WebPart.class ) )
                        {
                            HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                            return handlerContext.getRequest().getBody().getPart( parameter.getName() );
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext ) throws IOException
                    {
                        if( parameter.getType().isAssignableFrom( WebRequest.class ) )
                        {
                            HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                            return handlerContext.getRequest();
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext ) throws IOException
                    {
                        if( parameter.getType().isAssignableFrom( WebResponse.class ) )
                        {
                            HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                            return handlerContext.getRequest().getResponse();
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext ) throws IOException
                    {
                        if( parameter.getType().isAssignableFrom( WebSession.class ) )
                        {
                            HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                            if( parameter.hasAnnotation( Nonnull.class ) )
                            {
                                return handlerContext.getRequest().getOrCreateSession();
                            }
                            else
                            {
                                return handlerContext.getRequest().getSession();
                            }
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext ) throws IOException
                    {
                        if( parameter.getType().isAssignableFrom( WebRequest.Uri.class ) )
                        {
                            HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                            return handlerContext.getRequest().getUri();
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext ) throws IOException
                    {
                        if( parameter.getType().isAssignableFrom( WebRequest.Headers.class ) )
                        {
                            HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                            return handlerContext.getRequest().getHeaders();
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext ) throws IOException
                    {
                        if( parameter.getType().isAssignableFrom( WebRequest.Body.class ) )
                        {
                            HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                            return handlerContext.getRequest().getBody();
                        }
                        return SKIP;
                    }
                },
                new MethodHandle.ParameterResolver()
                {
                    @Nullable
                    @Override
                    public Object resolve( @Nonnull MethodParameter parameter,
                                           @Nonnull MapEx<String, Object> resolveContext ) throws IOException
                    {
                        if( parameter.hasAnnotation( Body.class ) )
                        {
                            if( parameter.getType().isAssignableFrom( InputStream.class ) )
                            {
                                HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                                return handlerContext.getRequest().getBody().asStream();
                            }
                            else if( parameter.getType().isAssignableFrom( Reader.class ) )
                            {
                                HandlerContext handlerContext = resolveContext.require( "handlerContext", HandlerContext.class );
                                return handlerContext.getRequest().getBody().asReader();
                            }
                            else
                            {
                                throw new IllegalStateException( "Illegal parameter definition " + parameter + " - parameters with @Body must be of type InputStream or Reader" );
                            }
                        }
                        return SKIP;
                    }
                }
        );
    }

    public long getId()
    {
        return this.id;
    }

    @Nonnull
    public MethodEndpoint getEndpoint()
    {
        return this.endpoint;
    }

    @Nonnull
    public Collection<HttpMethod> getHttpMethods()
    {
        return this.httpMethods;
    }

    @Override
    public int compareTo( AbstractMethodEndpointAdapter o )
    {
        if( this.rank > o.rank )
        {
            return -1;
        }
        else if( this.rank < o.rank )
        {
            return 1;
        }
        else if( this.id < o.id )
        {
            return -1;
        }
        else if( this.id > o.id )
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }

    public boolean matchesHttpMethod( @Nonnull WebRequest request )
    {
        return this.httpMethods.contains( request.getMethod() );
    }

    protected synchronized void addParameterResolvers( @Nonnull MethodHandle.ParameterResolver... parameterResolvers )
    {
        if( this.endpointInvoker != null )
        {
            throw new IllegalStateException( "Method endpoint invoker already created!" );
        }
        this.parameterResolvers.addAll( asList( parameterResolvers ) );
    }

    @Nonnull
    protected MethodEndpoint.Invoker getEndpointInvoker()
    {
        if( this.endpointInvoker == null )
        {
            synchronized( this )
            {
                if( this.endpointInvoker == null )
                {
                    this.endpointInvoker = this.endpoint.createInvoker( this.parameterResolvers );
                }
            }
        }
        return this.endpointInvoker;
    }

    protected boolean matchesWebApplication( @Nonnull WebRequest request )
    {
        if( this.webAppFilterExpression == null )
        {
            return true;
        }

        try
        {
            Boolean matches = this.webAppFilterExpression.withRoot( request ).expect( Boolean.class ).invoke();
            return matches != null && matches;
        }
        catch( ExpressionEvaluateException e )
        {
            LOG.warn( "Error while checking web application match for web endpoint {}: {}", this.endpoint, e.getMessage() );
            return false;
        }
        catch( Exception e )
        {
            LOG.warn( "Error while checking web application match for web endpoint {}: {}", this.endpoint, e.getMessage(), e );
            return false;
        }
    }

    protected boolean isUserAllowed( @Nonnull WebRequest request )
    {
        if( this.securityExpression == null )
        {
            return true;
        }

        try
        {
            Boolean matches = this.securityExpression.withRoot( request ).expect( Boolean.class ).invoke();
            return matches != null && matches;
        }
        catch( ExpressionEvaluateException e )
        {
            LOG.warn( "Error while checking authorization for web endpoint {}: {}", this.endpoint, e.getMessage() );
            return false;
        }
        catch( Exception e )
        {
            LOG.warn( "Error while checking authorization for web endpoint {}: {}", this.endpoint, e.getMessage(), e );
            return false;
        }
    }

    protected <T extends Annotation> T getAnnotation( @Nonnull Class<T> annotationType )
    {
        T annotation = this.endpoint.getAnnotation( annotationType );
        if( annotation != null )
        {
            return annotation;
        }

        Class<?> declaringType = this.endpoint.getDeclaringType();
        while( declaringType != null )
        {
            annotation = declaringType.getAnnotation( annotationType );
            if( annotation != null )
            {
                return annotation;
            }
            else
            {
                declaringType = declaringType.getSuperclass();
            }
        }
        return null;
    }
}

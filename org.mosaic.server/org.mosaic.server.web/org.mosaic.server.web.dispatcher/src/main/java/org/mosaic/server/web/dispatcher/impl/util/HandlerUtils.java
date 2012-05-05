package org.mosaic.server.web.dispatcher.impl.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.mosaic.server.web.dispatcher.impl.endpoint.MethodParameterResolver;
import org.mosaic.web.annotation.*;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpMethod;

import static java.lang.String.format;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * @author arik
 */
public abstract class HandlerUtils
{
    public static List<RegexPathMatcher> getMethodPaths( Method method, Class<? extends Annotation> annotationType )
    {
        Annotation pathPatternsAnn = HandlerUtils.findAnn( method, annotationType );
        if( pathPatternsAnn == null )
        {
            throw new IllegalArgumentException( String.format( "Method '%s' has no @%s annotation", method.getName(), annotationType.getSimpleName() ) );
        }

        List<RegexPathMatcher> matchers = new LinkedList<>();
        for( String pathPattern : ( String[] ) AnnotationUtils.getValue( pathPatternsAnn ) )
        {
            matchers.add( new RegexPathMatcher( pathPattern ) );
        }
        return matchers;
    }

    public static Expression getMethodFilter( Method method )
    {
        Filter filterAnn = HandlerUtils.findAnn( method, Filter.class );
        return filterAnn != null ? new SpelExpressionParser().parseExpression( filterAnn.value() ) : null;
    }

    public static Expression getMethodSecurity( Method method )
    {
        Secured securedAnn = HandlerUtils.findAnn( method, Secured.class );
        if( securedAnn == null )
        {
            return null;
        }
        else if( securedAnn.value().trim().length() == 0 )
        {
            return new SpelExpressionParser().parseExpression( "user != null" );
        }
        else
        {
            return new SpelExpressionParser().parseExpression( securedAnn.value() );
        }
    }

    public static List<MethodParameterResolver.ResolvedParameter> resolveMethodParameters(
            Collection<MethodParameterResolver> resolvers, Method method )
    {
        ParameterNameDiscoverer nameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

        List<MethodParameterResolver.ResolvedParameter> resolvedParameters = new LinkedList<>();
        for( int i = 0; i < method.getParameterTypes().length; i++ )
        {
            MethodParameter methodParameter = new MethodParameter( method, i );
            methodParameter.initParameterNameDiscovery( nameDiscoverer );

            for( MethodParameterResolver resolver : resolvers )
            {
                MethodParameterResolver.ResolvedParameter resolvedParameter = resolver.resolve( methodParameter );
                if( resolvedParameter != null )
                {
                    resolvedParameters.add( resolvedParameter );
                    break;
                }
            }

            if( resolvedParameters.size() == i )
            {
                throw new IllegalStateException( format( "Parameter '%s' of method '%s' in class '%s' is not supported",
                                                         methodParameter.getParameterName(),
                                                         method.getName(),
                                                         method.getDeclaringClass().getSimpleName() ) );
            }
        }
        return resolvedParameters;
    }

    public static Collection<HttpMethod> resolveHttpMethods( Method method )
    {
        Collection<HttpMethod> httpMethods = new LinkedList<>();
        if( findAnn( method, Get.class ) != null )
        {
            httpMethods.add( HttpMethod.GET );
        }
        if( findAnn( method, Post.class ) != null )
        {
            httpMethods.add( HttpMethod.POST );
        }
        if( findAnn( method, Put.class ) != null )
        {
            httpMethods.add( HttpMethod.PUT );
        }
        if( findAnn( method, Delete.class ) != null )
        {
            httpMethods.add( HttpMethod.DELETE );
        }
        return httpMethods;
    }

    public static <A extends Annotation> A findAnn( Method method, Class<A> type )
    {
        A ann = findAnnotation( method, type );
        if( ann == null )
        {
            return findAnnotation( method.getDeclaringClass(), type );
        }
        else
        {
            return ann;
        }
    }
}

package org.mosaic.util.expression.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.lifecycle.impl.util.ServiceUtils;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionParseException;
import org.mosaic.util.expression.ExpressionParser;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author arik
 */
public class ExpressionParserImpl implements ExpressionParser, InitializingBean, DisposableBean
{
    @Nullable
    private final BundleContext bundleContext;

    @Nonnull
    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();

    @Nonnull
    private final LoadingCache<String, Expression> expressionsCache;

    @Nullable
    private ServiceRegistration<ExpressionParser> serviceRegistration;

    public ExpressionParserImpl( @Nullable BundleContext bundleContext ) throws InvalidSyntaxException
    {
        this.bundleContext = bundleContext;
        this.expressionsCache =
                CacheBuilder.newBuilder()
                            .concurrencyLevel( 100 )
                            .expireAfterAccess( 2, TimeUnit.HOURS )
                            .initialCapacity( 500 )
                            .maximumSize( 5000 )
                            .build( new CacheLoader<String, Expression>()
                            {
                                @Override
                                public Expression load( String key ) throws Exception
                                {
                                    return new ExpressionImpl( spelExpressionParser.parseExpression( key ) );
                                }
                            } );
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        if( this.bundleContext != null )
        {
            this.serviceRegistration = ServiceUtils.register( this.bundleContext, ExpressionParser.class, this );
        }
    }

    @Override
    public void destroy() throws Exception
    {
        if( this.bundleContext != null && this.serviceRegistration != null )
        {
            this.serviceRegistration = ServiceUtils.unregister( this.serviceRegistration );
            this.serviceRegistration = null;
        }
    }

    @Nonnull
    @Override
    public Expression parseExpression( @Nonnull String expression )
    {
        try
        {
            return this.expressionsCache.get( expression );
        }
        catch( ExecutionException e )
        {
            Throwable cause = e.getCause();
            throw new ExpressionParseException(
                    "Could not parse or create compiled expression from '" + expression + "': " + cause.getMessage(),
                    cause
            );
        }
    }

    private class ExpressionImpl implements Expression
    {
        @Nonnull
        private final org.springframework.expression.Expression spelExpression;

        private ExpressionImpl( @Nonnull org.springframework.expression.Expression spelExpression )
        {
            this.spelExpression = spelExpression;
        }

        @Override
        public Invoker createInvoker()
        {
            return new InvokerImpl();
        }

        private class InvokerImpl implements Invoker
        {
            @Override
            public InvokerWithRoot withRoot( @Nonnull Object root )
            {
                return new InvokerWithRootImpl( root );
            }
        }

        private class InvokerWithRootImpl implements InvokerWithRoot
        {
            @Nonnull
            private final Object root;

            @Nullable
            private Map<String, Object> variables;

            private InvokerWithRootImpl( @Nonnull Object root )
            {
                this.root = root;
            }

            @Override
            public InvokerWithRoot setVariable( @Nonnull String name, @Nullable Object value )
            {
                if( this.variables == null )
                {
                    this.variables = new HashMap<>( 5 );
                }
                this.variables.put( name, value );
                return this;
            }

            @Override
            public <T> TypedInvoker<T> expect( @Nonnull Class<T> type )
            {
                return new TypedInvokerImpl<>(
                        this.root,
                        this.variables == null ? Collections.<String, Object>emptyMap() : this.variables,
                        type
                );
            }
        }

        private class TypedInvokerImpl<T> implements Expression.TypedInvoker<T>
        {
            @Nonnull
            private final Object root;

            @Nonnull
            private final Map<String, Object> variables;

            @Nonnull
            private final Class<T> expectedType;

            private TypedInvokerImpl( @Nonnull Object root,
                                      @Nonnull Map<String, Object> variables,
                                      @Nonnull Class<T> expectedType )
            {
                this.root = root;
                this.variables = variables;
                this.expectedType = expectedType;
            }

            @Override
            public T invoke()
            {
                StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
                evaluationContext.setRootObject( this.root );
                evaluationContext.setVariables( this.variables );
                return spelExpression.getValue( evaluationContext, this.expectedType );
            }
        }
    }
}

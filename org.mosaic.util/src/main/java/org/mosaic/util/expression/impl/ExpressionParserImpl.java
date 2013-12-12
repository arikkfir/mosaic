package org.mosaic.util.expression.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionEvaluateException;
import org.mosaic.util.expression.ExpressionParseException;
import org.mosaic.util.expression.ExpressionParser;
import org.osgi.framework.InvalidSyntaxException;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;

/**
 * @author arik
 */
final class ExpressionParserImpl implements ExpressionParser
{
    @Nonnull
    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();

    @Nonnull
    private final LoadingCache<String, org.springframework.expression.Expression> expressionsCache;

    ExpressionParserImpl() throws InvalidSyntaxException
    {
        this.expressionsCache =
                CacheBuilder.newBuilder()
                            .concurrencyLevel( 100 )
                            .expireAfterAccess( 2, TimeUnit.HOURS )
                            .initialCapacity( 500 )
                            .maximumSize( 5000 )
                            .build( new CacheLoader<String, org.springframework.expression.Expression>()
                            {
                                @Nonnull
                                @Override
                                public org.springframework.expression.Expression load( @Nonnull String key )
                                        throws Exception
                                {
                                    return ExpressionParserImpl.this.spelExpressionParser.parseExpression( key );
                                }
                            } );
    }

    @Nonnull
    @Override
    public Expression<Object> parseExpression( @Nonnull String expression )
    {
        return parseExpression( expression, Object.class );
    }

    @Nonnull
    @Override
    public <T> Expression<T> parseExpression( @Nonnull String expression, @Nonnull Class<T> expectedType )
            throws ExpressionParseException
    {
        return parseExpression( expression, expectedType, getClass() );
    }

    @Nonnull
    @Override
    public <T> Expression<T> parseExpression( @Nonnull String expression,
                                              @Nonnull Class<T> expectedType,
                                              @Nonnull Class<?> classContext ) throws ExpressionParseException
    {
        org.springframework.expression.Expression spelExpr;
        try
        {
            spelExpr = this.expressionsCache.get( expression );
        }
        catch( ExecutionException e )
        {
            Throwable cause = e.getCause();
            throw new ExpressionParseException(
                    "Could not parse expression '" + expression + "': " + cause.getMessage(),
                    cause
            );
        }

        return new ExpressionImpl<>( expectedType, classContext, spelExpr );
    }

    private class ExpressionImpl<T> implements Expression<T>
    {
        @Nonnull
        private final Class<T> expectedType;

        @Nonnull
        private final TypeLocator typeLocator;

        @Nonnull
        private final org.springframework.expression.Expression spelExpression;

        private ExpressionImpl( @Nonnull Class<T> expectedType,
                                @Nonnull Class<?> classContext,
                                @Nonnull org.springframework.expression.Expression spelExpression )
        {
            this.expectedType = expectedType;
            this.typeLocator = new StandardTypeLocator( classContext.getClassLoader() );
            this.spelExpression = spelExpression;
        }

        @Nonnull
        @Override
        public Invoker<T> createInvocation( @Nonnull Object root )
        {
            return new InvokerImpl( root );
        }

        private class InvokerImpl implements Invoker<T>
        {
            @Nonnull
            private final Object root;

            @Nullable
            private Map<String, Object> variables;

            private InvokerImpl( @Nonnull Object root )
            {
                this.root = root;
            }

            @Nonnull
            @Override
            public Invoker<T> setVariable( @Nonnull String name, @Nullable Object value )
            {
                if( this.variables == null )
                {
                    this.variables = new HashMap<>( 5 );
                }
                this.variables.put( name, value );
                return this;
            }

            @Nullable
            @Override
            public T get()
            {
                try
                {
                    StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
                    evaluationContext.setTypeLocator( ExpressionImpl.this.typeLocator );
                    evaluationContext.setRootObject( this.root );
                    if( this.variables != null )
                    {
                        evaluationContext.setVariables( this.variables );
                    }
                    return ExpressionImpl.this.spelExpression.getValue( evaluationContext, ExpressionImpl.this.expectedType );
                }
                catch( EvaluationException e )
                {
                    throw new ExpressionEvaluateException( e.getMessage(), e );
                }
            }

            @Nonnull
            @Override
            public T require()
            {
                T result = get();
                if( result == null )
                {
                    throw new ExpressionEvaluateException( "result of expression '" + spelExpression.getExpressionString() + "' must not be null" );
                }
                else
                {
                    return result;
                }
            }
        }
    }
}

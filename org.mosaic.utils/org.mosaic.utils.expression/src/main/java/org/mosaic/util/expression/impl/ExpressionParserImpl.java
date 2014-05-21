package org.mosaic.util.expression.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.util.expression.Expression;
import org.mosaic.util.expression.ExpressionEvaluateException;
import org.mosaic.util.expression.ExpressionParseException;
import org.mosaic.util.expression.ExpressionParser;
import org.mosaic.util.reflection.TypeTokens;
import org.osgi.framework.InvalidSyntaxException;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

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
        return parseExpression( expression, TypeTokens.of( Object.class ) );
    }

    @Nonnull
    @Override
    public <T> Expression<T> parseExpression( @Nonnull String expression, @Nonnull TypeToken<T> expectedType )
            throws ExpressionParseException
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

        return new ExpressionImpl<>( expectedType, spelExpr );
    }

    void clearCaches()
    {
        this.expressionsCache.invalidateAll();
    }

    private class ExpressionImpl<T> implements Expression<T>
    {
        @Nonnull
        private final TypeToken<T> expectedType;

        @Nonnull
        private final org.springframework.expression.Expression spelExpression;

        private ExpressionImpl( @Nonnull TypeToken<T> expectedType,
                                @Nonnull org.springframework.expression.Expression spelExpression )
        {
            this.expectedType = expectedType;
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

            @SuppressWarnings("unchecked")
            @Nullable
            @Override
            public T invoke()
            {
                try
                {
                    StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
                    evaluationContext.setRootObject( this.root );
                    if( this.variables != null )
                    {
                        evaluationContext.setVariables( this.variables );
                    }

                    Object result = ExpressionImpl.this.spelExpression.getValue( evaluationContext );
                    if( result == null )
                    {
                        return null;
                    }
                    else if( expectedType.isAssignableFrom( result.getClass() ) )
                    {
                        return ( T ) result;
                    }
                    else
                    {
                        throw new ExpressionEvaluateException( "value '" + result + "' resulted from expression '" + spelExpression.getExpressionString() + "' must be of type '" + expectedType + "'" );
                    }
                }
                catch( EvaluationException e )
                {
                    throw new ExpressionEvaluateException( e.getMessage(), e );
                }
            }
        }
    }
}

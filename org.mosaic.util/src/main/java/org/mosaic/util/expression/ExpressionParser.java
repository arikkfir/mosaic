package org.mosaic.util.expression;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface ExpressionParser
{
    @Nonnull
    Expression<Object> parseExpression( @Nonnull String expression ) throws ExpressionParseException;

    @Nonnull
    <T> Expression<T> parseExpression( @Nonnull String expression, @Nonnull Class<T> expectedType )
            throws ExpressionParseException;

    @Nonnull
    <T> Expression<T> parseExpression( @Nonnull String expression,
                                       @Nonnull Class<T> expectedType,
                                       @Nonnull Class<?> classContext )
            throws ExpressionParseException;
}

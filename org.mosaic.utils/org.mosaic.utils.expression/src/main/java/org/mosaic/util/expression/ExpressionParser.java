package org.mosaic.util.expression;

import com.google.common.reflect.TypeToken;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
public interface ExpressionParser
{
    @Nonnull
    Expression<Object> parseExpression( @Nonnull String expression ) throws ExpressionParseException;

    @Nonnull
    <T> Expression<T> parseExpression( @Nonnull String expression, @Nonnull TypeToken<T> expectedType )
            throws ExpressionParseException;
}

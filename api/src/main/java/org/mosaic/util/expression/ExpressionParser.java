package org.mosaic.util.expression;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface ExpressionParser
{
    @Nonnull
    Expression parseExpression( @Nonnull String expression );
}

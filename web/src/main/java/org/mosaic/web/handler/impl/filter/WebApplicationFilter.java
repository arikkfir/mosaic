package org.mosaic.web.handler.impl.filter;

import javax.annotation.Nonnull;
import org.mosaic.util.collect.MapEx;
import org.mosaic.util.expression.Expression;
import org.mosaic.web.handler.impl.RequestExecutionPlan;

/**
 * @author arik
 */
public class WebApplicationFilter implements Filter
{
    @Nonnull
    private final String webAppFilterExpression;

    public WebApplicationFilter( @Nonnull String webAppFilterExpression )
    {
        this.webAppFilterExpression = webAppFilterExpression;
    }

    @Override
    public boolean matches( @Nonnull RequestExecutionPlan plan, @Nonnull MapEx<String, Object> context )
    {
        Expression compiledExpression = plan.getExpressionParser().parseExpression( this.webAppFilterExpression );
        return compiledExpression.createInvoker().withRoot( plan.getRequest() ).expect( Boolean.class ).require();
    }
}

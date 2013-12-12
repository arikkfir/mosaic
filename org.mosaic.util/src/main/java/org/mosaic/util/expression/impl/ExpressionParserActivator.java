package org.mosaic.util.expression.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.expression.ExpressionParser;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author arik
 */
public final class ExpressionParserActivator implements BundleActivator
{
    @Nullable
    private ServiceRegistration<ExpressionParser> expressionParserServiceRegistration;

    @Override
    public void start( @Nonnull final BundleContext bundleContext ) throws Exception
    {
        this.expressionParserServiceRegistration = bundleContext.registerService( ExpressionParser.class, new ExpressionParserImpl(), null );
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        if( this.expressionParserServiceRegistration != null )
        {
            try
            {
                this.expressionParserServiceRegistration.unregister();
            }
            catch( Exception ignore )
            {
            }
        }
        this.expressionParserServiceRegistration = null;
    }
}

package org.mosaic.util.expression.impl;

import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Nullable;
import org.mosaic.util.expression.ExpressionParser;
import org.osgi.framework.*;

/**
 * @author arik
 */
public class Activator implements BundleActivator, BundleListener
{
    @Nullable
    private ServiceRegistration<ExpressionParser> registration;

    private ExpressionParserImpl expressionParser;

    @Override
    public void start( @Nonnull BundleContext context ) throws Exception
    {
        context.addBundleListener( this );
        this.expressionParser = new ExpressionParserImpl();
        this.registration = context.registerService( ExpressionParser.class, this.expressionParser, null );
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        ServiceRegistration<ExpressionParser> registration = this.registration;
        if( registration != null )
        {
            try
            {
                registration.unregister();
            }
            catch( Exception ignore )
            {
            }
            this.registration = null;
            this.expressionParser = null;
        }

        context.removeBundleListener( this );
    }

    @Override
    public void bundleChanged( @Nonnull BundleEvent event )
    {
        ExpressionParserImpl expressionParser = this.expressionParser;
        if( expressionParser != null && event.getType() == BundleEvent.UNRESOLVED )
        {
            expressionParser.clearCaches();
        }
    }
}

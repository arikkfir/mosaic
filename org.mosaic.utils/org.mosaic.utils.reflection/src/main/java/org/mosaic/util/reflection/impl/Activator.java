package org.mosaic.util.reflection.impl;

import javax.annotation.Nonnull;
import org.mosaic.util.reflection.ClassAnnotations;
import org.mosaic.util.reflection.MethodAnnotations;
import org.mosaic.util.reflection.MethodParameterNames;
import org.mosaic.util.reflection.TypeTokens;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * @author arik
 */
public class Activator implements BundleActivator, BundleListener
{
    @Override
    public void start( @Nonnull BundleContext context ) throws Exception
    {
        context.addBundleListener( this );
    }

    @Override
    public void stop( @Nonnull BundleContext context ) throws Exception
    {
        context.removeBundleListener( this );
    }

    @Override
    public void bundleChanged( @Nonnull BundleEvent event )
    {
        if( event.getType() == BundleEvent.UNRESOLVED )
        {
            ClassAnnotations.clearCaches();
            MethodAnnotations.clearCaches();
            MethodParameterNames.clearCaches();
            TypeTokens.clearCaches();
        }
    }
}

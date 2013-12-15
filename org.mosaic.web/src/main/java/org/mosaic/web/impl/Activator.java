package org.mosaic.web.impl;

import javax.annotation.Nonnull;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Slf4jLog;
import org.mosaic.modules.Module;
import org.mosaic.modules.spi.ModuleActivator;

/**
 * @author arik
 */
final class Activator implements ModuleActivator
{
    @Override
    public void onBeforeActivate( @Nonnull Module module )
    {
        Log.setLog( new Slf4jLog( "org.eclipse.jetty" ) );
    }

    @Override
    public void onAfterDeactivate( @Nonnull Module module )
    {
        // no-op
    }
}

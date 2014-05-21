package org.mosaic.launcher;

import org.apache.felix.framework.Felix;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * @author arik
 */
class Shutdown implements Runnable
{
    private static Felix felix;

    public static void setFelix( Felix felix )
    {
        Shutdown.felix = felix;
    }

    @Override
    public void run()
    {
        Felix felix = Shutdown.felix;
        if( felix != null )
        {
            EventsLogger.printEmphasizedInfoMessage( "STOPPING MOSAIC" );
            try
            {
                BundleContext bundleContext = felix.getBundleContext();
                if( bundleContext != null )
                {
                    Bundle coreBundle = bundleContext.getBundle( 1 );
                    if( coreBundle != null )
                    {
                        coreBundle.stop();
                    }

                    Bundle systemBundle = bundleContext.getBundle( 0 );
                    if( systemBundle != null )
                    {
                        systemBundle.stop();
                    }
                }
            }
            catch( BundleException e )
            {
                EventsLogger.printEmphasizedErrorMessage( "ERROR STOPPING MOSAIC: {}", e.getMessage(), e );
            }
        }
    }
}

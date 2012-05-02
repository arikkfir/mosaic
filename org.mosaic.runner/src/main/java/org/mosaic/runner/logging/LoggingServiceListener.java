package org.mosaic.runner.logging;

import org.mosaic.runner.util.BundleUtils;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author arik
 */
public class LoggingServiceListener implements ServiceListener
{
    private static final String OSGI_LOG_NAME = "org.mosaic.server.osgi.services";

    private static final String MDC_SR_KEY = "logging-osgi-service-ref";

    private static final String MDC_BUNDLE_KEY = "logging-osgi-bundle";

    @Override
    public void serviceChanged( ServiceEvent event )
    {
        ServiceReference<?> sr = event.getServiceReference( );
        Bundle bundle = sr.getBundle( );
        String bts = BundleUtils.toString( bundle );

        MDC.put( MDC_SR_KEY, sr.toString( ) );
        MDC.put( MDC_BUNDLE_KEY, bts );
        try
        {
            Logger logger = LoggerFactory.getLogger( OSGI_LOG_NAME );
            switch( event.getType( ) )
            {
                case ServiceEvent.REGISTERED:
                    logger.info( "Service for types '{}' has been registered by bundle '{}'", sr.getProperty( Constants.OBJECTCLASS ), bts );
                    break;

                case ServiceEvent.MODIFIED:
                    logger.info( "Service for types '{}' form bundle '{}' has been modified", sr.getProperty( Constants.OBJECTCLASS ), bts );
                    break;

                case ServiceEvent.UNREGISTERING:
                    logger.info( "Service for types '{}' from bundle '{}' has been unregistered", sr.getProperty( Constants.OBJECTCLASS ), bts );
                    break;
            }
        }
        finally
        {
            MDC.remove( MDC_SR_KEY );
            MDC.remove( MDC_BUNDLE_KEY );
        }
    }
}

package org.mosaic.runner.logging;

import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class ServiceEventListener implements ServiceListener {

    private final Logger logger;

    public ServiceEventListener() {
        this.logger = LoggerFactory.getLogger( "org.osgi.services" );
    }

    @Override
    public void serviceChanged( ServiceEvent event ) {
        ServiceReference<?> sr = event.getServiceReference();
        Bundle bundle = sr.getBundle();
        String bsn = bundle.getSymbolicName();
        Version version = bundle.getVersion();
        long bundleId = bundle.getBundleId();
        switch( event.getType() ) {
            case ServiceEvent.REGISTERED:
                this.logger.trace( "Service for types '{}' has been registered by bundle '{}-{}[{}]'",
                                   new Object[] {
                                           sr.getProperty( Constants.OBJECTCLASS ),
                                           bsn,
                                           version,
                                           bundleId
                                   } );
                break;

            case ServiceEvent.MODIFIED:
                this.logger.trace( "Service for types '{}' form bundle '{}-{}[{}]' has been modified",
                                   new Object[] {
                                           sr.getProperty( Constants.OBJECTCLASS ),
                                           bsn,
                                           version,
                                           bundleId
                                   } );
                break;

            case ServiceEvent.UNREGISTERING:
                this.logger.trace( "Service for types '{}' from bundle '{}-{}[{}]' has been unregistered",
                                   new Object[] {
                                           sr.getProperty( Constants.OBJECTCLASS ),
                                           bsn,
                                           version,
                                           bundleId
                                   } );
                break;
        }
    }
}

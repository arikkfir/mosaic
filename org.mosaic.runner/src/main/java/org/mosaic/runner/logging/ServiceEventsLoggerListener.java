package org.mosaic.runner.logging;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arik
 */
public class ServiceEventsLoggerListener implements ServiceListener {

    private final Logger logger = LoggerFactory.getLogger( "org.osgi.services" );

    @Override
    public void serviceChanged( ServiceEvent event ) {
        ServiceReference<?> sr = event.getServiceReference();
        String bundle = LogUtils.toString( sr.getBundle() );
        Object objectClass = sr.getProperty( Constants.OBJECTCLASS );
        switch( event.getType() ) {
            case ServiceEvent.REGISTERED:
                this.logger.trace( "Service for types '{}' has been registered by bundle '{}'", objectClass, bundle );
                break;

            case ServiceEvent.MODIFIED:
                this.logger.trace( "Service for types '{}' form bundle '{}' has been modified", objectClass, bundle );
                break;

            case ServiceEvent.UNREGISTERING:
                this.logger.trace( "Service for types '{}' from bundle '{}' has been unregistered", objectClass, bundle );
                break;
        }
    }
}

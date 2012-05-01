package org.mosaic.server.osgi.util;

import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.ServiceReference;

/**
 * @author arik
 */
public abstract class ServiceUtils {

    public static Map<String, Object> getServiceProperties( ServiceReference<?> serviceReference ) {
        Map<String, Object> properties = new HashMap<>();
        for( String property : serviceReference.getPropertyKeys() ) {
            properties.put( property, serviceReference.getProperty( property ) );
        }
        return properties;
    }

}

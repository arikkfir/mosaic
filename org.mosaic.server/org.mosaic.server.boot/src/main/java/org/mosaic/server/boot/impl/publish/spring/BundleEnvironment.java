package org.mosaic.server.boot.impl.publish.spring;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.Bundle;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author arik
 */
public class BundleEnvironment extends StandardEnvironment {

    public BundleEnvironment( Bundle bundle ) {
        getPropertySources().addFirst( new MapPropertySource( "bundle", getBundleHeaders( bundle ) ) );
    }

    private static Map<String, Object> getBundleHeaders( Bundle bundle ) {
        Map<String, Object> headersMap = new HashMap<>();
        Dictionary<String, String> headers = bundle.getHeaders();
        Enumeration<String> headerNames = headers.keys();
        while( headerNames.hasMoreElements() ) {
            String headerName = headerNames.nextElement();
            headersMap.put( headerName, headers.get( headerName ) );
        }
        return headersMap;
    }
}

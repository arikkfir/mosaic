package org.mosaic.server.boot.impl.track;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.Bundle;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author arik
 */
public class BundleEnvironment extends StandardEnvironment {

    private final Bundle bundle;

    public BundleEnvironment( Bundle bundle ) {
        this.bundle = bundle;
    }

    @Override
    protected void customizePropertySources( MutablePropertySources propertySources ) {
        super.customizePropertySources( propertySources );
        propertySources.addFirst( new MapPropertySource( "bundle", getHeaderMap() ) );
    }

    private Map<String, Object> getHeaderMap() {
        Map<String, Object> headersMap = new HashMap<>();
        Dictionary<String, String> headers = this.bundle.getHeaders();
        Enumeration<String> headerNames = headers.keys();
        while( headerNames.hasMoreElements() ) {
            String headerName = headerNames.nextElement();
            headersMap.put( headerName, headers.get( headerName ) );
        }
        return headersMap;
    }
}

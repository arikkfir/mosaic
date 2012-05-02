package org.mosaic.web;

import java.util.Map;
import java.util.Set;
import org.mosaic.security.PermissionPolicy;

/**
 * @author arik
 */
public interface HttpApplication extends Map<String, Object>
{
    String getName();

    Map<String, String> getParameters();

    Set<String> getVirtualHosts();

    boolean isHostIncluded( String host );

    boolean isAddressAllowed( String address );

    PermissionPolicy getPermissionPolicy();
}

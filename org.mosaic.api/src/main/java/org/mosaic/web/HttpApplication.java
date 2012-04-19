package org.mosaic.web;

import java.util.Set;
import org.mosaic.collection.TypedDict;

/**
 * @author arik
 */
public interface HttpApplication extends TypedDict<Object> {

    String getName();

    TypedDict<String> getParameters();

    Set<String> getVirtualHosts();

    boolean isHostIncluded( String host );

    boolean isAddressAllowed( String address );

    Set<String> getAvailableRoles();

    boolean isPermissionIncluded( String permission, String... roles );

}

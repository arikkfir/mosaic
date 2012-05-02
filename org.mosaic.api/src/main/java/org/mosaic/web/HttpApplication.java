package org.mosaic.web;

import java.util.Set;
import org.mosaic.security.PermissionPolicy;
import org.mosaic.util.collection.TypedDict;

/**
 * @author arik
 */
public interface HttpApplication extends TypedDict<Object>
{

    String getName( );

    TypedDict<String> getParameters( );

    Set<String> getVirtualHosts( );

    boolean isHostIncluded( String host );

    boolean isAddressAllowed( String address );

    PermissionPolicy getPermissionPolicy( );

}

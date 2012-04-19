package org.mosaic.web;

import java.util.Set;
import java.util.regex.Pattern;
import org.mosaic.collection.TypedDict;

/**
 * @author arik
 */
public interface HttpApplication extends TypedDict<Object> {

    String getName();

    TypedDict<String> getParameters();

    Set<String> getVirtualHosts();

    Set<String> getAllowedClientAddresses();

    Set<String> getRestrictedClientAddresses();

    Set<Pattern> getPermissionPatterns( String... roles );

}

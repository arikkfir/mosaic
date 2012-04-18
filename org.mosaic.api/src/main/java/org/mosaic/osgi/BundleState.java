package org.mosaic.osgi;

import org.osgi.framework.Bundle;

/**
 * @author arik
 */
public enum BundleState {

    INSTALLED,
    RESOLVED,
    STARTING,
    ACTIVE,
    PUBLISHED,
    STOPPING,
    UNINSTALLED;

    public static BundleState valueOfOsgiState( int state ) {
        switch( state ) {
            case Bundle.INSTALLED:
                return INSTALLED;
            case Bundle.RESOLVED:
                return RESOLVED;
            case Bundle.ACTIVE:
                return ACTIVE;
            case Bundle.STARTING:
                return STARTING;
            case Bundle.STOPPING:
                return STOPPING;
            case Bundle.UNINSTALLED:
                return UNINSTALLED;
            default:
                throw new IllegalArgumentException( "Illegal OSGi bundle state: " + state );
        }
    }

}

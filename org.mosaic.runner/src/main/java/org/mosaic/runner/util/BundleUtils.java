package org.mosaic.runner.util;

import org.osgi.framework.Bundle;

/**
 * @author arik
 */
public abstract class BundleUtils {

    public static String toString( Bundle bundle ) {
        if( bundle == null ) {
            return "";
        } else {
            return bundle.getSymbolicName() + "-" + bundle.getVersion() + "[" + bundle.getBundleId() + "]";
        }
    }

}

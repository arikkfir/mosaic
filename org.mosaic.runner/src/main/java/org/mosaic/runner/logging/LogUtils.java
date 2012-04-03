package org.mosaic.runner.logging;

import org.osgi.framework.Bundle;

/**
 * @author arik
 */
public abstract class LogUtils {

    public static String toString( Bundle bundle ) {
        if( bundle == null ) {
            return "";
        } else {
            return bundle.getSymbolicName() + "-" + bundle.getVersion() + "[" + bundle.getBundleId() + "]";
        }
    }
}

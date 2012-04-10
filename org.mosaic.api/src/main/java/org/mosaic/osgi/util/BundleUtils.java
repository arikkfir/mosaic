package org.mosaic.osgi.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

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

    public static String toString( BundleContext bundleContext ) {
        if( bundleContext != null ) {
            try {
                return toString( bundleContext.getBundle() );
            } catch( IllegalStateException ignore ) {
            }
        }
        return "";
    }

}

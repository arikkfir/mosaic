package org.mosaic.osgi.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author arik
 */
public abstract class BundleUtils {

    public static Collection<Bundle> getAllBundles( BundleContext bundleContext ) {
        Bundle[] bundles = bundleContext.getBundles();
        if( bundles == null ) {
            return Collections.emptyList();
        } else {
            return Arrays.asList( bundles );
        }
    }

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

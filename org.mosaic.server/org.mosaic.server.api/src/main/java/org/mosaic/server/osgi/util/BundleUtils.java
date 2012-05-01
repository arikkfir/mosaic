package org.mosaic.server.osgi.util;

import java.util.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

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

    public static Collection<Bundle> findBundlesInStates( BundleContext bundleContext, Integer... states ) {
        Collection<Integer> bundleStates = Arrays.asList( states );
        Collection<Bundle> resolvedBundles = new LinkedList<>();
        for( Bundle bundle : getAllBundles( bundleContext ) ) {
            if( bundleStates.contains( bundle.getState() ) ) {
                resolvedBundles.add( bundle );
            }
        }
        return resolvedBundles;
    }

    public static List<Bundle> findMatchingBundles( BundleContext bundleContext, boolean exact, String... filters ) {
        List<Bundle> matches = new LinkedList<>();
        for( Bundle bundle : getAllBundles( bundleContext ) ) {

            boolean match = true;
            if( filters != null && filters.length > 0 ) {

                long bundleId = bundle.getBundleId();
                String bundleName = bundle.getHeaders().get( Constants.BUNDLE_NAME );
                String symbolicName = bundle.getSymbolicName();
                match = false;
                for( String arg : filters ) {
                    try {
                        if( Integer.parseInt( arg ) == bundleId ) {
                            match = true;
                            break;
                        }
                    } catch( NumberFormatException ignore ) {
                    }

                    if( exact && ( bundleName.equalsIgnoreCase( arg ) || symbolicName.equalsIgnoreCase( arg ) ) ) {
                        match = true;
                        break;
                    } else if( !exact && ( bundleName.contains( arg ) || symbolicName.contains( arg ) ) ) {
                        match = true;
                        break;
                    }
                }

            }

            if( match ) {
                matches.add( bundle );
            }
        }
        return matches;
    }

    public static List<Bundle> filterBundlesByState( List<Bundle> bundles, Integer... states ) {
        List<Integer> statesList = Arrays.asList( states );
        List<Bundle> matches = new LinkedList<>();
        for( Bundle bundle : bundles ) {

            if( statesList.contains( bundle.getState() ) ) {
                matches.add( bundle );
            }

        }
        return matches;
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

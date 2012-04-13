package org.mosaic.server.shell.commands.impl;

import java.util.ArrayList;
import java.util.List;
import org.mosaic.lifecycle.BundleContextAware;
import org.mosaic.lifecycle.BundleStatusHelper;
import org.mosaic.lifecycle.ServiceRef;
import org.mosaic.osgi.util.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * @author arik
 */
public abstract class AbstractCommand implements BundleContextAware {


    private BundleContext bundleContext;

    protected BundleStatusHelper statusHelper;

    @ServiceRef( required = false )
    public void setStatusHelper( BundleStatusHelper statusHelper ) {
        this.statusHelper = statusHelper;
    }

    @Override
    public void setBundleContext( BundleContext bundleContext ) {
        this.bundleContext = bundleContext;
    }

    protected List<Bundle> findMatchingBundles( boolean exact, List<String> args ) {
        List<Bundle> matches = new ArrayList<>( 100 );
        for( Bundle bundle : BundleUtils.getAllBundles( this.bundleContext ) ) {
            String bundleName = bundle.getHeaders().get( Constants.BUNDLE_NAME );
            String symbolicName = bundle.getSymbolicName();

            boolean match = true;
            if( args != null && !args.isEmpty() ) {
                match = false;
                for( String arg : args ) {
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

    protected String capitalize( String state ) {
        state = state.toLowerCase();
        state = Character.toUpperCase( state.charAt( 0 ) ) + state.substring( 1 );
        return state;
    }
}

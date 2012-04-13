package org.mosaic.server.shell.commands.impl;

import org.mosaic.lifecycle.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author arik
 */
public abstract class AbstractCommand implements BundleContextAware {

    private BundleContext bundleContext;

    private BundleStatusHelper statusHelper;

    @ServiceRef( required = false )
    public void setStatusHelper( BundleStatusHelper statusHelper ) {
        this.statusHelper = statusHelper;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public void setBundleContext( BundleContext bundleContext ) {
        this.bundleContext = bundleContext;
    }

    protected BundleStatus getBundleStatus( Bundle bundle ) {
        if( this.statusHelper != null ) {
            return this.statusHelper.getBundleStatus( bundle.getBundleId() );
        } else {
            return new SimpleBundleStatus( bundle );
        }
    }

    protected String capitalize( String state ) {
        state = state.toLowerCase();
        state = Character.toUpperCase( state.charAt( 0 ) ) + state.substring( 1 );
        return state;
    }
}

package org.mosaic.server.shell.commands.impl;

import java.util.Collection;
import java.util.Collections;
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

    private static class SimpleBundleStatus implements BundleStatus {

        private final Bundle bundle;

        public SimpleBundleStatus( Bundle bundle ) {
            this.bundle = bundle;
        }

        @Override
        public BundleState getState() {
            return BundleState.valueOfOsgiState( this.bundle.getState() );
        }

        @Override
        public Collection<String> getUnsatisfiedRequirements() {
            return Collections.emptyList();
        }
    }
}

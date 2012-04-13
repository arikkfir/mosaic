package org.mosaic.lifecycle;

import java.util.Collection;
import java.util.Collections;
import org.osgi.framework.Bundle;

/**
 * @author arik
 */
public class SimpleBundleStatus implements BundleStatus {

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

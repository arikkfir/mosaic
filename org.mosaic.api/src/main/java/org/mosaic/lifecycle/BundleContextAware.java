package org.mosaic.lifecycle;

import org.osgi.framework.BundleContext;

/**
 * @author arik
 */
public interface BundleContextAware {

    void setBundleContext( BundleContext bundleContext );

}

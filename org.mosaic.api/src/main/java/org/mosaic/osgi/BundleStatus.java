package org.mosaic.osgi;

import java.util.Collection;

/**
 * @author arik
 */
public interface BundleStatus {

    BundleState getState();

    Collection<String> getUnsatisfiedRequirements();

}

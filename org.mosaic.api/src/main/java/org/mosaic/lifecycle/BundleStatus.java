package org.mosaic.lifecycle;

import java.util.Collection;

/**
 * @author arik
 */
public interface BundleStatus {

    BundleState getState();

    Collection<String> getUnsatisfiedRequirements();

}

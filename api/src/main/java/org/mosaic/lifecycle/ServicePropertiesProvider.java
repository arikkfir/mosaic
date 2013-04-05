package org.mosaic.lifecycle;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface ServicePropertiesProvider
{
    @Nonnull
    DP[] getServiceProperties();
}

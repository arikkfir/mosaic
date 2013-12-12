package org.mosaic.modules.impl;

import java.util.List;
import javax.annotation.Nonnull;
import org.mosaic.modules.ModuleWiring;

/**
 * @author arik
 */
public interface ServiceCapabilityProvider
{
    @Nonnull
    List<ModuleWiring.ServiceCapability> getServiceCapabilities();
}

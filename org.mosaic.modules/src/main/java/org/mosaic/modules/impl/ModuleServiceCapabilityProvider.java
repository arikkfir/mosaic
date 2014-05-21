package org.mosaic.modules.impl;

import java.util.List;
import org.mosaic.core.util.Nonnull;
import org.mosaic.modules.Module;

/**
 * @author arik
 */
public interface ModuleServiceCapabilityProvider
{
    @Nonnull
    List<Module.ServiceCapability> getServiceCapabilities();
}

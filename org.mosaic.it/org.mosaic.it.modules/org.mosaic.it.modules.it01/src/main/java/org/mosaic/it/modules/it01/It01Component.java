package org.mosaic.it.modules.it01;

import org.mosaic.core.components.Component;
import org.mosaic.core.components.Inject;
import org.mosaic.core.modules.Module;
import org.mosaic.core.modules.ModuleManager;
import org.mosaic.core.util.Nonnull;

/**
 * @author arik
 */
@Component
class It01Component
{
    @Nonnull
    @Inject
    private ModuleManager moduleManager;

    @Nonnull
    @Inject
    private Module module;

    public It01Component()
    {

    }
}

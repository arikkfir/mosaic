package org.mosaic.development.idea.server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author arik
 */
public abstract class MosaicServer
{
    @NotNull
    public abstract String getName();

    @NotNull
    public abstract String getLocation();

    @Nullable
    public abstract String getVersion();
}

package org.mosaic.development.idea.server;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author arik
 */
public abstract class MosaicServerManager implements ApplicationComponent
{
    @NotNull
    public static MosaicServerManager getInstance()
    {
        return ApplicationManager.getApplication().getComponent( MosaicServerManager.class );
    }

    @NotNull
    public abstract Collection<? extends MosaicServer> getServers();

    @Nullable
    public abstract MosaicServer getServer( @NotNull String name );

    @NotNull
    public abstract MosaicServer addServer( @NotNull String name, @NotNull String location );
}

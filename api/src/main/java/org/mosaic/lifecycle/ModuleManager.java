package org.mosaic.lifecycle;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joda.time.Duration;

/**
 * @author arik
 */
public interface ModuleManager
{
    @Nullable
    Module getModule( long id );

    @Nullable
    Module getModule( @Nonnull String name );

    @Nullable
    Module getModule( @Nonnull String name, @Nonnull String version );

    @Nonnull
    Collection<Module> getModules();

    @Nullable
    Module getModuleFor( @Nonnull Object target );

    @Nonnull
    Module installModule( @Nonnull URL url, @Nonnull Duration timeout ) throws IOException, InterruptedException;
}

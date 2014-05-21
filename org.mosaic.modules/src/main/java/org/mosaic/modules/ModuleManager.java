package org.mosaic.modules;

import com.google.common.base.Optional;
import java.util.Collection;
import org.mosaic.core.util.Nonnull;
import org.mosaic.core.util.Version;

/**
 * @author arik
 */
public interface ModuleManager
{
    @Nonnull
    Optional<? extends Module> getModule( long id );

    @Nonnull
    Optional<? extends Module> getModule( @Nonnull String name, @Nonnull Version version );

    @Nonnull
    Collection<? extends Module> getModules();

    @Nonnull
    Optional<? extends Module> getModuleFor( @Nonnull Object source );
}

package org.mosaic.lifecycle;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface ModuleManager
{
    @Nullable
    Module getModule( long id );

    @Nullable
    Module getModule( @Nonnull String name );

    @Nonnull
    Collection<Module> getModules();

    @Nullable
    Module getModuleFor( @Nonnull Object target );
}

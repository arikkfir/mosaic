package org.mosaic.modules;

import java.net.URL;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface ModuleResources
{
    @Nonnull
    Module getModule();

    @Nullable
    URL getResource( @Nonnull String name );

    @Nonnull
    Collection<URL> getResources( @Nonnull String name );

    @Nonnull
    Collection<URL> findResources( @Nonnull String glob );

    @Nonnull
    ClassLoader getClassLoader();

    @Nonnull
    Class<?> loadClass( @Nonnull String className ) throws ClassNotFoundException;
}

package org.mosaic.modules;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface ServiceReference<Type>
{
    long getId();

    @Nonnull
    Class<? extends Type> getType();

    @Nonnull
    MapEx<String, Object> getProperties();

    @Nullable
    Module getProvider();

    @Nullable
    Type get();

    @Nonnull
    Type require();
}

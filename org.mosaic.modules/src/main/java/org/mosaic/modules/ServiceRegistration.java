package org.mosaic.modules;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface ServiceRegistration<Type>
{
    @Nonnull
    Module getProvider();

    @Nonnull
    Class<? extends Type> getType();

    @Nonnull
    MapEx<String, Object> getProperties();

    void setProperties( @Nonnull Map<String, Object> properties );

    void setProperty( @Nonnull String name, @Nullable Object value );

    void setProperty( @Nonnull Property property );

    void removeProperty( @Nonnull String name );

    boolean isRegistered();

    void unregister();
}

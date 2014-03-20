package org.mosaic.modules;

import com.google.common.reflect.TypeToken;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface ConfigValue
{
    @Nonnull
    String getScope();

    @Nonnull
    String getKey();

    @Nonnull
    String getValue();

    @Nonnull
    <T> T getValue( @Nonnull TypeToken<T> typeToken );
}

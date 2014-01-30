package org.mosaic.util.collections;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public interface MapEx<K, V> extends Map<K, V>
{
    @Nonnull
    Optional<V> find( @Nonnull K key );

    @Nonnull
    <T> Optional<T> find( @Nonnull K key, @Nonnull TypeToken<T> type );

    @Nonnull
    <T> Optional<T> find( @Nonnull K key, @Nonnull Class<T> type );
}

package org.mosaic.util.collections;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface MultiMapEx<K, V> extends Map<K, List<V>>
{
    @Nonnull
    Optional<V> findFirst( @Nonnull K key );

    @Nonnull
    <T> Optional<T> findFirst( @Nonnull K key, @Nonnull TypeToken<T> type );

    @Nonnull
    <T> Optional<T> findFirst( @Nonnull K key, @Nonnull Class<T> type );

    @Nonnull
    <T> List<T> find( @Nonnull K key, @Nonnull TypeToken<T> type );

    @Nonnull
    <T> List<T> find( @Nonnull K key, @Nonnull Class<T> type );

    void addOne( @Nonnull K key, @Nullable V value );
}

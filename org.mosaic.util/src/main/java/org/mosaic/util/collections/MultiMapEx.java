package org.mosaic.util.collections;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface MultiMapEx<K, V> extends Map<K, List<V>>
{
    V getOne( @Nonnull K key );

    V getOne( @Nonnull K key, @Nullable V defaultValue );

    @Nonnull
    V requireOne( @Nonnull K key );

    @Nullable
    <T> T getOne( @Nonnull K key, @Nonnull Class<T> type );

    @Nonnull
    <T> T requireOne( @Nonnull K key, @Nonnull Class<T> type );

    <T> T getOne( @Nonnull K key, @Nonnull Class<T> type, @Nullable T defaultValue );

    @Nonnull
    <T> List<T> getAll( @Nonnull K key, @Nonnull Class<T> type );

    void addOne( @Nonnull K key, @Nullable V value );
}

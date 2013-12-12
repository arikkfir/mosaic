package org.mosaic.util.collections;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface MapEx<K, V> extends Map<K, V>
{
    V get( @Nonnull K key, @Nullable V defaultValue );

    @Nonnull
    V require( @Nonnull K key );

    @Nullable
    <T> T get( @Nonnull K key, @Nonnull Class<T> type );

    @Nonnull
    <T> T require( @Nonnull K key, @Nonnull Class<T> type );

    <T> T get( @Nonnull K key, @Nonnull Class<T> type, @Nullable T defaultValue );
}

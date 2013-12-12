package org.mosaic.datasource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.util.collections.MapEx;

/**
 * @author arik
 */
public interface Transaction
{
    @Nonnull
    String getName();

    @Nullable
    Transaction getParent();

    boolean isReadOnly();

    @Nonnull
    MapEx<String, Object> getAttributes();

    void commit();

    void rollback();

    void close();
}

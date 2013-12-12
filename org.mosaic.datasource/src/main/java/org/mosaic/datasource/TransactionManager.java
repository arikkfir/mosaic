package org.mosaic.datasource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface TransactionManager
{
    @Nonnull
    Transaction startTransaction( @Nonnull String name, boolean readOnly );

    @Nullable
    Transaction getTransaction();

    @Nonnull
    Transaction requireTransaction();
}

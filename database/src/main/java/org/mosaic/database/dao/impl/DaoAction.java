package org.mosaic.database.dao.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface DaoAction
{
    @Nullable
    Object execute( @Nonnull Object proxy, @Nonnull Object... args ) throws Exception;
}

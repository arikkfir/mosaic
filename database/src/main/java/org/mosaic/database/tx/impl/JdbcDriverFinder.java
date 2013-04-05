package org.mosaic.database.tx.impl;

import java.sql.Driver;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author arik
 */
public interface JdbcDriverFinder
{
    @Nullable
    Driver getDriver( @Nonnull String url );
}

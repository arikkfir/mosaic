package org.mosaic.dao.extract;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public abstract class ResultSetExtractor<Type>
{
    public abstract Type extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException;
}

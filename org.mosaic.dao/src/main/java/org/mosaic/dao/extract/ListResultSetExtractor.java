package org.mosaic.dao.extract;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * @author arik
 */
public class ListResultSetExtractor<RowType> extends ResultSetExtractor<List<RowType>>
{
    @Nonnull
    private final ResultSetExtractor<RowType> rowExtractor;

    public ListResultSetExtractor( @Nonnull ResultSetExtractor<RowType> rowExtractor )
    {
        this.rowExtractor = rowExtractor;
    }

    @Override
    public List<RowType> extract( @Nonnull ResultSet rs, @Nonnull Object... arguments ) throws SQLException
    {
        List<RowType> rows = new LinkedList<>();
        while( rs.next() )
        {
            rows.add( this.rowExtractor.extract( rs, arguments ) );
        }
        return rows;
    }
}

package org.mosaic.console.util.table;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
* @author arik
*/
public interface Column<Row>
{
    @Nonnull
    String getHeader();

    @Nonnull
    Number getWidth();

    @Nullable
    String getValue( @Nonnull Row row );
}

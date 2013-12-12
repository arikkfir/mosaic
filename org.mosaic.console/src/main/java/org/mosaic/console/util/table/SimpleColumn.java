package org.mosaic.console.util.table;

import javax.annotation.Nonnull;

/**
 * @author arik
 */
public abstract class SimpleColumn<Row> implements Column<Row>
{
    @Nonnull
    private final String header;

    private Number width;

    public SimpleColumn( @Nonnull String header )
    {
        this( header, -1 );
    }

    public SimpleColumn( @Nonnull String header, Number width )
    {
        this.header = header;
        this.width = width;
    }

    @Nonnull
    @Override
    public String getHeader()
    {
        return this.header;
    }

    @Nonnull
    @Override
    public Number getWidth()
    {
        return this.width;
    }
}

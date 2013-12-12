package org.mosaic.console.util.table;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mosaic.console.Console;

import static com.google.common.base.Strings.padEnd;
import static com.google.common.base.Strings.repeat;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class TablePrinter<Row>
{
    @Nonnull
    private final Console console;

    @Nonnull
    private final String indent;

    @Nonnull
    private final List<Column<Row>> columns;

    @Nonnull
    private final Map<Column<Row>, Integer> columnWidths;

    private boolean printChrome = true;

    private boolean headersWerePrinted;

    @SafeVarargs
    public TablePrinter( @Nonnull Console console, @Nonnull Column<Row>... columns )
    {
        this( console, 0, columns );
    }

    @SafeVarargs
    public TablePrinter( @Nonnull Console console, int indent, @Nonnull Column<Row>... columns )
    {
        this.console = console;
        this.indent = repeat( " ", indent );
        this.columns = asList( columns );

        int cumulativeWidth = 0;
        List<Column<Row>> unknownWidthColumns = new LinkedList<>();
        Map<Column<Row>, Integer> columnWidths = newHashMap();
        for( Column<Row> column : this.columns )
        {
            Number width = column.getWidth();
            if( width.intValue() == -1 )
            {
                unknownWidthColumns.add( column );
            }
            if( width instanceof Integer || width instanceof Byte || width instanceof Short || width instanceof Long )
            {
                cumulativeWidth += width.intValue();
                columnWidths.put( column, width.intValue() );
            }
            else
            {
                Long value = Math.round( ( this.console.getWidth() - indent ) * width.doubleValue() );
                cumulativeWidth += value.intValue();
                columnWidths.put( column, value.intValue() );
            }
        }

        if( !unknownWidthColumns.isEmpty() )
        {
            int remainingWidth = ( this.console.getWidth() - indent ) - cumulativeWidth;
            int spread = remainingWidth / unknownWidthColumns.size();
            for( Column<Row> column : unknownWidthColumns )
            {
                columnWidths.put( column, spread );
            }
        }

        this.columnWidths = columnWidths;
    }

    @Nonnull
    public TablePrinter<Row> noChrome()
    {
        this.printChrome = false;
        return this;
    }

    @Nonnull
    public TablePrinter<Row> print( @Nonnull Row row ) throws IOException
    {
        if( this.printChrome && !this.headersWerePrinted )
        {
            printHeaders();
        }
        printRow( row );
        return this;
    }

    public void endTable() throws IOException
    {
        if( this.printChrome )
        {
            printDelimiterLine();
        }
    }

    private void printHeaders() throws IOException
    {
        this.headersWerePrinted = true;

        // print first delimiter line (eg. "+------+---------+---+-------+")
        printDelimiterLine();

        // print header labels
        List<Iterator<String>> columnHeaderLines = newArrayList();
        for( Column<Row> column : this.columns )
        {
            columnHeaderLines.add( getCellLines( column, column.getHeader() ) );
        }
        LinesAggregatingIterator lines = new LinesAggregatingIterator( columnHeaderLines );
        while( lines.hasNext() )
        {
            this.console.print( this.indent ).println( lines.next() );
        }

        // print second delimiter line
        printDelimiterLine();
    }

    private void printRow( @Nonnull Row row ) throws IOException
    {
        List<Iterator<String>> columnCellLines = newArrayList();
        for( Column<Row> column : this.columns )
        {
            columnCellLines.add( getCellLines( column, column.getValue( row ) ) );
        }
        LinesAggregatingIterator lines = new LinesAggregatingIterator( columnCellLines );
        while( lines.hasNext() )
        {
            this.console.print( this.indent ).println( lines.next() );
        }
    }

    @Nonnull
    private Iterator<String> getCellLines( @Nonnull Column<Row> column, @Nullable String value )
    {
        if( value == null )
        {
            return Iterators.emptyIterator();
        }

        List<String> headerLines = newLinkedList();
        for( String token : Splitter.on( '\n' ).trimResults().split( value ) )
        {
            for( String line : Splitter.fixedLength( this.columnWidths.get( column ) - 2 ).trimResults().split( token ) )
            {
                headerLines.add( line );
            }
        }
        return headerLines.iterator();
    }

    private void printDelimiterLine() throws IOException
    {
        boolean first = true;
        for( Column<Row> column : this.columns )
        {
            if( first )
            {
                this.console.print( this.indent ).print( TablePrinter.this.printChrome ? "+" : " " );
                first = false;
            }
            this.console.print( repeat( TablePrinter.this.printChrome ? "-" : " ",
                                        this.columnWidths.get( column ) - 2 ) )
                        .print( TablePrinter.this.printChrome ? "+" : " " );
        }
        this.console.println();
    }

    private class LinesAggregatingIterator implements Iterator<String>
    {
        @Nonnull
        private final List<Iterator<String>> iterators;

        private LinesAggregatingIterator( @Nonnull List<Iterator<String>> iterators )
        {
            this.iterators = iterators;
        }

        @Override
        public boolean hasNext()
        {
            for( Iterator<String> iterator : this.iterators )
            {
                if( iterator.hasNext() )
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String next()
        {
            boolean first = true;

            StringBuilder line = new StringBuilder( 1000 );
            for( int i = 0; i < this.iterators.size(); i++ )
            {
                if( first )
                {
                    line.append( TablePrinter.this.printChrome ? "|" : " " );
                    first = false;
                }

                Column<Row> column = TablePrinter.this.columns.get( i );
                int width = TablePrinter.this.columnWidths.get( column ) - 2;
                Iterator<String> iterator = this.iterators.get( i );
                line.append( iterator.hasNext() ? padEnd( iterator.next(), width, ' ' ) : repeat( " ", width ) );
                line.append( TablePrinter.this.printChrome ? "|" : " " );
            }
            return line.toString();
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}

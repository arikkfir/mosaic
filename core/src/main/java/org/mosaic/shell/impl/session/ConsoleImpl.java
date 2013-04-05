package org.mosaic.shell.impl.session;

import com.google.common.base.Strings;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.BreakIterator;
import java.util.*;
import jline.console.ConsoleReader;
import jline.console.history.History;
import jline.console.history.PersistentHistory;
import org.mosaic.shell.Console;

import static com.google.common.base.Strings.repeat;
import static java.lang.Math.max;
import static java.lang.Thread.currentThread;

/**
 * @author arik
 */
public class ConsoleImpl implements Console
{
    private static List<String> splitLinesOnLengthAndWords( String text, int length )
    {
        List<String> lines = new LinkedList<>();
        if( text == null || text.length() == 0 )
        {
            lines.add( "" );
            return lines;
        }

        for( String line : text.split( "\n" ) )
        {
            StringBuilder lineBuffer = new StringBuilder( line.length() );

            BreakIterator wordIterator = BreakIterator.getWordInstance();
            wordIterator.setText( line );
            int start = wordIterator.first();
            for( int end = wordIterator.next(); end != BreakIterator.DONE; start = end, end = wordIterator.next() )
            {
                for( String word : splitLinesOnLength( line.substring( start, end ), length ) )
                {
                    if( lineBuffer.length() + word.length() > length )
                    {
                        lines.add( lineBuffer.toString() );
                        lineBuffer.delete( 0, Integer.MAX_VALUE );
                    }
                    lineBuffer.append( word );
                }
            }

            if( lineBuffer.length() > 0 )
            {
                lines.add( lineBuffer.toString() );
            }
        }
        return lines;
    }

    private static List<String> splitLinesOnLength( String text, int length )
    {
        List<String> lines = new LinkedList<>();
        if( text == null || text.length() == 0 )
        {
            lines.add( "" );
            return lines;
        }

        StringBuilder lineBuffer = new StringBuilder( text.length() );
        for( int ci = 0; ci < text.length(); ci++ )
        {
            char c = text.charAt( ci );
            if( c == '\n' || c == '\r' )
            {

                lines.add( lineBuffer.toString() );
                lineBuffer.delete( 0, Integer.MAX_VALUE );

            }
            else if( lineBuffer.length() < length )
            {

                lineBuffer.append( c );

            }
            else
            {

                lines.add( lineBuffer.toString() );
                lineBuffer.delete( 0, Integer.MAX_VALUE );

            }
        }

        if( lineBuffer.length() > 0 )
        {
            lines.add( lineBuffer.toString() );
        }

        return lines;
    }

    private final ConsoleReader consoleReader;

    public ConsoleImpl( ConsoleReader consoleReader )
    {
        this.consoleReader = consoleReader;
    }

    @Override
    public Writer getWriter()
    {
        return this.consoleReader.getOutput();
    }

    @Override
    public int getWidth()
    {
        return this.consoleReader.getTerminal().getWidth();
    }

    @Override
    public int getHeight()
    {
        return this.consoleReader.getTerminal().getHeight();
    }

    @Override
    public boolean setCursorPosition( int position ) throws IOException
    {
        return this.consoleReader.setCursorPosition( position );
    }

    @Override
    public Console flush() throws IOException
    {
        this.consoleReader.flush();

        History history = this.consoleReader.getHistory();
        if( history instanceof PersistentHistory )
        {
            PersistentHistory persistentHistory = ( PersistentHistory ) history;
            persistentHistory.flush();
        }

        return this;
    }

    @Override
    public boolean backspace() throws IOException
    {
        return this.consoleReader.backspace();
    }

    @Override
    public int moveCursor( int num ) throws IOException
    {
        return this.consoleReader.moveCursor( num );
    }

    @Override
    public boolean replace( int num, String replacement ) throws IOException
    {
        boolean replace = this.consoleReader.replace( num, replacement );
        flush();
        return replace;
    }

    @Override
    public int readCharacter() throws IOException
    {
        return this.consoleReader.readCharacter();
    }

    @Override
    public int readCharacter( final char... allowed ) throws IOException
    {
        Arrays.sort( allowed ); // always need to sort before binarySearch
        while( !currentThread().isInterrupted() )
        {
            int i = readCharacter();
            if( i < 0 )
            {
                throw new EOFException( "Connection terminated" );
            }

            char c = ( char ) i;
            if( Arrays.binarySearch( allowed, c ) >= 0 )
            {
                return c;
            }
        }
        throw new EOFException( "Connection terminated" );
    }

    @Override
    public int ask( String question, char... allowed ) throws IOException
    {
        print( question ).print( " [" );
        for( int i = 0, allowedLength = allowed.length; i < allowedLength; i++ )
        {
            if( i > 0 )
            {
                print( "/" );
            }
            print( allowed[ i ] + "" );
        }
        print( "] " );
        flush();
        int i = readCharacter( allowed );
        println();
        return i;
    }

    @Override
    public String readLine() throws IOException
    {
        flush();
        return this.consoleReader.readLine();
    }

    @Override
    public Console print( Object s ) throws IOException
    {
        this.consoleReader.print( s == null ? "" : s.toString() );
        flush();
        return this;
    }

    @Override
    public Console println() throws IOException
    {
        this.consoleReader.println();
        flush();
        return this;
    }

    @Override
    public Console println( Object s ) throws IOException
    {
        print( s == null ? "" : s.toString() ).println();
        return this;
    }

    @Override
    public Console print( int indent, Object s ) throws IOException
    {
        return print( repeat( " ", indent ) ).print( s );
    }

    @Override
    public Console println( int indent, Object s ) throws IOException
    {
        if( s != null )
        {
            String indentLine = repeat( " ", indent );
            for( String line : splitLinesOnLengthAndWords( s.toString(), getWidth() - indentLine.length() ) )
            {
                print( indentLine ).println( line );
            }
            return this;
        }
        else
        {
            return println();
        }
    }

    @Override
    public Console printlnNoFirstLineIndent( int indent, Object s ) throws IOException
    {
        if( s != null )
        {
            String indentLine = repeat( " ", indent );

            List<String> lines = splitLinesOnLengthAndWords( s.toString(), getWidth() - indentLine.length() );
            if( !lines.isEmpty() )
            {
                Iterator<String> iterator = lines.iterator();
                println( iterator.next() );

                while( iterator.hasNext() )
                {
                    print( indentLine ).println( iterator.next() );
                }
            }
            return this;
        }
        else
        {
            return println();
        }
    }

    @Override
    public boolean delete() throws IOException
    {
        return this.consoleReader.delete();
    }

    @Override
    public boolean killLine() throws IOException
    {
        return this.consoleReader.killLine();
    }

    @Override
    public boolean clearScreen() throws IOException
    {
        return this.consoleReader.clearScreen();
    }

    @Override
    public Console beep() throws IOException
    {
        this.consoleReader.beep();
        return this;
    }

    @Override
    public boolean paste() throws IOException
    {
        boolean paste = this.consoleReader.paste();
        flush();
        return paste;
    }

    @Override
    public TableHeaders createTable()
    {
        return new Table();
    }

    @Override
    public TableHeaders createTable( int indent )
    {
        return new Table( indent );
    }

    @Override
    public Console printStackTrace( Throwable throwable ) throws IOException
    {
        PrintWriter printWriter = new PrintWriter( getWriter() );
        throwable.printStackTrace( printWriter );
        printWriter.flush();
        flush();
        return this;
    }

    @Override
    public Console printStackTrace( String message, Throwable throwable ) throws IOException
    {
        println( message );
        return printStackTrace( throwable );
    }

    private class Table implements TableHeaders, TablePrinter
    {
        private final String indent;

        private final List<String> titles = new LinkedList<>();

        private final List<Integer> lengths = new LinkedList<>();

        private int lineLength;

        private String chromeLine;

        private Table()
        {
            this( 0 );
        }

        private Table( int indent )
        {
            this.indent = repeat( " ", indent );
        }

        @Override
        public TableHeaders addHeader( String title )
        {
            return addHeader( title, 0 );
        }

        @Override
        public TableHeaders addHeader( String title, double width )
        {
            int terminalWidth = consoleReader.getTerminal().getWidth() - this.indent.length();
            double relativeWidth = terminalWidth * width;
            return addHeader( title, ( int ) relativeWidth );
        }

        @Override
        public TableHeaders addHeader( String title, int width )
        {
            if( width < 0 )
            {
                throw new IllegalArgumentException( "Width must not be negative" );
            }
            else if( width == 0 )
            {
                if( this.lengths.contains( 0 ) )
                {
                    throw new IllegalArgumentException( "Only one column can be of variable length (add a width parameter)" );
                }
            }
            else if( title.length() > width )
            {
                title = title.substring( 0, width );
            }
            this.titles.add( title );
            this.lengths.add( width );
            this.lineLength += width + 1;
            return this;
        }

        @Override
        public TablePrinter start() throws IOException
        {
            if( this.titles.size() == 0 )
            {
                throw new IllegalStateException( "No headers added to table! Call 'TableHeaders.add(..) method first" );
            }

            int terminalWidth = consoleReader.getTerminal().getWidth() - this.indent.length();
            int widthSum = 1;
            for( Integer length : this.lengths )
            {
                widthSum += length + 1;
            }

            this.lineLength++;
            StringBuilder chromeLine = new StringBuilder( this.lineLength );
            StringBuilder headerLine = new StringBuilder( this.lineLength );
            for( int i = 0; i < this.titles.size(); i++ )
            {
                int length = this.lengths.get( i );
                if( length == 0 )
                {
                    length = terminalWidth - widthSum - 1;
                    this.lengths.set( i, length );
                }
                chromeLine.append( '+' ).append( repeat( "-", length ) );
                headerLine.append( '|' ).append( Strings.padEnd( this.titles.get( i ), length, ' ' ) );
            }
            chromeLine.append( '+' );
            headerLine.append( '|' );
            this.chromeLine = chromeLine.toString();

            ConsoleImpl.this.print( this.indent ).println( chromeLine );
            ConsoleImpl.this.print( this.indent ).println( headerLine );
            ConsoleImpl.this.print( this.indent ).println( chromeLine );
            return this;
        }

        @Override
        public TablePrinter print( Object... values ) throws IOException
        {

            // build a matrix of values - outer list is the columns, and each inner list is lines for that column value
            List<List<String>> matrix = new ArrayList<>( values.length );
            int lineCount = 1;
            for( int colIndex = 0; colIndex < this.titles.size(); colIndex++ )
            {
                Integer colLength = this.lengths.get( colIndex );
                Object value;
                if( colIndex >= values.length || values[ colIndex ] == null )
                {
                    value = "";
                }
                else
                {
                    value = values[ colIndex ];
                }

                List<String> valueLines = splitLinesOnLengthAndWords( value.toString(), colLength );
                matrix.add( valueLines );
                lineCount = max( valueLines.size(), lineCount );
            }

            StringBuilder buffer = new StringBuilder( this.lineLength );
            for( int lineIndex = 0; lineIndex < lineCount; lineIndex++ )
            {
                buffer.delete( 0, Integer.MAX_VALUE );
                for( int colIndex = 0; colIndex < this.titles.size(); colIndex++ )
                {
                    Integer colLength = this.lengths.get( colIndex );
                    List<String> colLines = matrix.get( colIndex );
                    String line = lineIndex < colLines.size()
                                  ? Strings.padEnd( colLines.get( lineIndex ), colLength, ' ' )
                                  : repeat( " ", colLength );
                    buffer.append( '|' ).append( line );
                }
                buffer.append( '|' );
                ConsoleImpl.this.print( this.indent ).println( buffer );
            }

            return this;
        }

        @Override
        public void done() throws IOException
        {
            ConsoleImpl.this.print( this.indent ).println( chromeLine );
        }
    }
}
